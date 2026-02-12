package dev.o7moon.openboatutils.physics;

import net.minecraft.block.BlockState;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/**
 * Four-wheel vehicle dynamics engine.
 * Each wheel (FL, FR, RL, RR) has independent vertical load, slip angle,
 * lateral force, and longitudinal force calculations.
 *
 * Replaces the Bicycle Model with a full four-corner model that supports:
 * - Independent per-wheel weight transfer (longitudinal + lateral)
 * - Differential types (Open, Locked, LSD) per axle
 * - Configurable AWD front/rear torque split
 * - Aerodynamic downforce
 * - Weather-dependent grip modifier
 */
public class FourWheelPhysicsEngine {

    // ─── PERSISTENT STATE ───
    private volatile float vx = 0f; // longitudinal velocity in vehicle frame (m/s), volatile for render thread reads
    private float vy = 0f;        // lateral velocity in vehicle frame (m/s)
    private float yawAngle = 0f;  // heading angle (rad)
    private float yawRate = 0f;   // yaw rate (rad/s)
    private float steeringAngle = 0f; // actual steering angle with delay (rad)
    private float prevSteeringInput = 0f; // previous tick steering input for direction change detection

    // Previous tick accelerations for weight transfer
    private float axPrev = 0f;
    private float ayPrev = 0f;

    // Per-wheel relaxation state (lateral forces)
    private final float[] fyActual = new float[4]; // FL, FR, RL, RR

    // Per-wheel vertical loads
    private final float[] fzWheel = new float[4];

    // Track which boat entity we are simulating
    private int lastBoatId = -1;

    // Configuration
    private VehicleConfig config;
    private boolean enabled = false;

    // Current surface
    private SurfaceProperties currentSurface = SurfaceProperties.ASPHALT_DRY;

    // Weather condition
    private WeatherCondition weather = WeatherCondition.CLEAR;

    // Per-wheel friction circle result objects (thread-safe, instance-level)
    private final TireModel.FrictionCircleResult[] frictionResults = new TireModel.FrictionCircleResult[4];

    // Preallocated per-wheel arrays to avoid GC pressure in substep loop
    private final float[] muWheel = new float[4];
    private final float[] driveForceWheel = new float[4];
    private final float[] brakeForceWheel = new float[4];
    private final float[] fxWheel = new float[4];

    // Reusable surface accumulator to avoid per-tick allocation
    private final SurfaceProperties.SurfaceAccumulator surfaceAccumulator = new SurfaceProperties.SurfaceAccumulator();

    private static final float GRAVITY = 9.81f;
    private static final float TICK_TIME = 0.05f;
    private static final float MIN_MU_PEAK = 0.01f;
    private static final float YAW_RATE_DAMPING = 0.995f;

    // ─── LOW-SPEED DEAD ZONE ───
    private static final float STOP_SPEED_THRESHOLD = 0.15f;
    private static final float LOW_SPEED_FADE_THRESHOLD = 0.5f;

    // ─── AIRBORNE PHYSICS ───
    private static final float AIR_DRAG_COEFFICIENT = 0.35f;
    private static final float AIR_DENSITY = 1.225f;
    private static final float FRONTAL_AREA = 2.0f;
    private static final float AIR_YAW_RATE_DAMPING = 0.998f;

    // ─── VERTICAL PHYSICS ───
    private static final float LANDING_IMPACT_THRESHOLD = -1.5f;
    private static final float MAX_LANDING_GRIP_LOSS = 0.6f;
    private static final float LANDING_GRIP_RECOVERY_RATE = 0.05f;
    private static final float VERTICAL_PITCH_FACTOR = 0.15f;
    private static final float MAX_VERTICAL_PITCH = 0.5f;
    private static final int MIN_AIRBORNE_TICKS_FOR_IMPACT = 3;
    /** Terminal velocity for falling (blocks/tick) — limits how fast the boat can fall */
    private static final float TERMINAL_FALL_VELOCITY = -0.35f;
    /** Vertical drag factor applied per tick to slow down fast falls */
    private static final float VERTICAL_DRAG_FACTOR = 0.85f;

    // ─── STEERING STABILITY ───
    private static final float SELF_ALIGN_SPEED_THRESHOLD = 5.0f;
    private static final float LATERAL_VELOCITY_DAMPING = 0.97f;
    private static final float LATERAL_VELOCITY_DAMPING_ACTIVE = 0.99f;
    private static final float MAX_LATERAL_SPEED_RATIO = 1.2f;
    private static final float HANDBRAKE_FORCE_MULTIPLIER = 0.8f;
    /** Retention factor for lateral forces when steering direction reverses (0 = full reset, 1 = no reset) */
    private static final float STEERING_REVERSAL_FORCE_RETENTION = 0.3f;
    /** Retention factor for lateral velocity when steering direction reverses */
    private static final float STEERING_REVERSAL_VELOCITY_RETENTION = 0.5f;

    // ─── HIGH-SPEED SAFETY ───
    /** Maximum allowed velocity (m/s) to prevent numerical instability */
    private static final float MAX_VELOCITY = 100.0f;
    /** Maximum allowed yaw rate (rad/s) to prevent spinning out of control */
    private static final float MAX_YAW_RATE = 15.0f;

    // ─── COLLISION DETECTION ───
    /** Threshold for detecting collision-induced velocity change (fraction of total speed lost) */
    private static final float COLLISION_SPEED_LOSS_THRESHOLD = 0.3f;
    /** Maximum lateral velocity injection from collision (m/s) — limits false vy from velocity clipping */
    private static final float MAX_COLLISION_LATERAL_INJECTION = 1.5f;
    /** Damping factor for lateral forces when collision is detected (prevents force accumulation) */
    private static final float COLLISION_LATERAL_FORCE_DAMPING = 0.5f;
    /** Damping factor for lateral velocity on landing transition (prevents abrupt lateral forces) */
    private static final float LANDING_LATERAL_DAMPING = 0.5f;
    /** Damping factor for yaw rate on landing transition */
    private static final float LANDING_YAW_RATE_DAMPING = 0.8f;
    /** Minimum airborne ticks for a full lateral state reset (longer flights = full reset) */
    private static final int LANDING_FULL_RESET_AIRBORNE_TICKS = 5;

    // ─── STRAIGHT-LINE LATERAL DAMPING ───
    /** Aggressive damping for vy when driving straight (no steering, low yaw rate) */
    private static final float STRAIGHT_LINE_LATERAL_DAMPING = 0.90f;
    /** Yaw rate threshold below which straight-line damping is applied (rad/s) */
    private static final float STRAIGHT_LINE_YAW_RATE_THRESHOLD = 0.3f;

    // ─── AIRBORNE STATE ───
    private boolean airborne = false;
    private boolean wasAirborne = false;
    private int airborneTicks = 0;
    private float landingGripPenalty = 0f;
    private float verticalVelocity = 0f;
    private float prevVerticalVelocity = 0f;

    // Previous tick expected world velocity (for collision detection)
    private float expectedWorldVx = 0f;
    private float expectedWorldVz = 0f;

    public FourWheelPhysicsEngine() {
        this.config = VehicleConfig.createDefault();
        for (int i = 0; i < 4; i++) {
            frictionResults[i] = new TireModel.FrictionCircleResult();
        }
        resetState();
    }

    public void setConfig(VehicleConfig config) {
        this.config = config;
        resetState();
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }
    public VehicleConfig getConfig() { return config; }
    public void setAirborne(boolean airborne) { this.airborne = airborne; }
    public boolean isAirborne() { return airborne; }
    public void setWeather(WeatherCondition weather) { this.weather = weather; }
    public WeatherCondition getWeather() { return weather; }

    public void resetState() {
        vx = 0f;
        vy = 0f;
        yawAngle = 0f;
        yawRate = 0f;
        steeringAngle = 0f;
        prevSteeringInput = 0f;
        axPrev = 0f;
        ayPrev = 0f;
        for (int i = 0; i < 4; i++) {
            fyActual[i] = 0f;
            fzWheel[i] = 0f;
        }
        float staticFront = config.getStaticFrontLoad() * 0.5f;
        float staticRear = config.getStaticRearLoad() * 0.5f;
        fzWheel[WheelPosition.FRONT_LEFT.index] = staticFront;
        fzWheel[WheelPosition.FRONT_RIGHT.index] = staticFront;
        fzWheel[WheelPosition.REAR_LEFT.index] = staticRear;
        fzWheel[WheelPosition.REAR_RIGHT.index] = staticRear;
        lastBoatId = -1;
        wasAirborne = false;
        airborneTicks = 0;
        landingGripPenalty = 0f;
        verticalVelocity = 0f;
        prevVerticalVelocity = 0f;
        expectedWorldVx = 0f;
        expectedWorldVz = 0f;
    }

    //? >=1.21.3 {
    /*public RealisticPhysicsEngine.PhysicsResult update(net.minecraft.entity.vehicle.AbstractBoatEntity boat,
                                float steeringInput, float throttleInput, float brakeInput, boolean handbrake) {
    *///?}
    //? <=1.21 {
    public RealisticPhysicsEngine.PhysicsResult update(BoatEntity boat,
                                float steeringInput, float throttleInput, float brakeInput, boolean handbrake) {
    //?}
        if (!enabled) return null;

        int boatId = boat.getId();
        if (boatId != lastBoatId) {
            resetState();
            lastBoatId = boatId;
        }

        if (config.wheelbase <= 0.01f || config.trackWidth <= 0.01f || config.mass <= 0f || config.substeps <= 0)
            return null;

        // Detect current surface
        currentSurface = detectSurface(boat);

        float dt = TICK_TIME / config.substeps;

        // Initialize from entity
        Vec3d entityVel = boat.getVelocity();
        float entityYaw = (float) Math.toRadians(boat.getYaw()) + (float)(Math.PI / 2.0);

        float worldVx = (float) (entityVel.x / TICK_TIME);
        float worldVz = (float) (entityVel.z / TICK_TIME);

        // ─── COLLISION-AWARE VELOCITY INITIALIZATION ───
        // When Minecraft's move() clips velocity (wall/block collision), the world-frame
        // velocity changes abruptly. Naively converting to local frame creates a false
        // lateral velocity (vy) that throws the vehicle sideways.
        // Detect this by comparing actual entity velocity with what we expected from last tick.
        float naiveVx = (float) (worldVx * Math.cos(entityYaw) + worldVz * Math.sin(entityYaw));
        float naiveVy = (float) (-worldVx * Math.sin(entityYaw) + worldVz * Math.cos(entityYaw));

        float expectedSpeed = (float) Math.sqrt(expectedWorldVx * expectedWorldVx + expectedWorldVz * expectedWorldVz);
        float actualSpeed = (float) Math.sqrt(worldVx * worldVx + worldVz * worldVz);

        boolean collisionDetected = false;
        if (expectedSpeed > STOP_SPEED_THRESHOLD) {
            float speedLoss = (expectedSpeed - actualSpeed) / expectedSpeed;
            float dvx = worldVx - expectedWorldVx;
            float dvz = worldVz - expectedWorldVz;
            float velocityChange = (float) Math.sqrt(dvx * dvx + dvz * dvz);
            // Collision if significant speed loss OR large velocity direction change
            collisionDetected = (speedLoss > COLLISION_SPEED_LOSS_THRESHOLD)
                    || (velocityChange > expectedSpeed * COLLISION_SPEED_LOSS_THRESHOLD);
        }

        if (collisionDetected && actualSpeed > STOP_SPEED_THRESHOLD) {
            // Project actual world velocity onto vehicle forward direction to get corrected vx,
            // and limit the lateral component to prevent false sideways forces
            vx = naiveVx;
            // Limit how much lateral velocity a collision can inject —
            // vy here retains the previous tick's value (persistent state), which is more
            // trustworthy than the naive conversion from collision-clipped world velocity
            float vyChange = naiveVy - vy;
            float clampedChange = MathHelper.clamp(vyChange,
                    -MAX_COLLISION_LATERAL_INJECTION, MAX_COLLISION_LATERAL_INJECTION);
            vy = vy + clampedChange;
            // Reduce lateral force build-up from collision
            for (int i = 0; i < 4; i++) fyActual[i] *= COLLISION_LATERAL_FORCE_DAMPING;
        } else {
            vx = naiveVx;
            vy = naiveVy;
        }
        yawAngle = entityYaw;

        float Lf = config.getFrontAxleDistance();
        float Lr = config.getRearAxleDistance();
        float halfTrack = config.getHalfTrack();

        // ─── VERTICAL VELOCITY TRACKING ───
        prevVerticalVelocity = verticalVelocity;
        verticalVelocity = (float) (entityVel.y / TICK_TIME);

        // ─── AIRBORNE STATE TRACKING ───
        if (airborne) airborneTicks++;

        // ─── LANDING DETECTION ───
        boolean justLanded = false;
        if (wasAirborne && !airborne) {
            float landingVelocity = Math.min(prevVerticalVelocity, verticalVelocity);
            if (airborneTicks >= MIN_AIRBORNE_TICKS_FOR_IMPACT && landingVelocity < LANDING_IMPACT_THRESHOLD) {
                float impactSeverity = Math.min(1.0f, Math.abs(landingVelocity - LANDING_IMPACT_THRESHOLD) / 8.0f);
                landingGripPenalty = Math.min(MAX_LANDING_GRIP_LOSS, impactSeverity * MAX_LANDING_GRIP_LOSS);
                justLanded = true;
            }
            int savedAirborneTicks = airborneTicks;
            airborneTicks = 0;

            // ─── LANDING LATERAL STATE RESET ───
            // Extended flights (≥5 ticks): full reset of lateral state to prevent
            // accumulated vy/fyActual from carrying over and causing sideways drift.
            // Short flights: dampen but preserve some momentum for natural feel.
            if (savedAirborneTicks >= LANDING_FULL_RESET_AIRBORNE_TICKS) {
                // Full reset — any lateral state from air is unreliable
                vy = 0f;
                for (int i = 0; i < 4; i++) fyActual[i] = 0f;
                yawRate *= 0.5f;
            } else {
                // Short hop — dampen to prevent false lateral forces from collision clipping
                vy *= LANDING_LATERAL_DAMPING;
                for (int i = 0; i < 4; i++) fyActual[i] *= LANDING_LATERAL_DAMPING;
                yawRate *= LANDING_YAW_RATE_DAMPING;
            }

            // Recompute expected world velocity after lateral state reset
            // to prevent the next tick's collision detection from seeing a
            // mismatch and re-injecting false lateral velocity
            float postLandingWorldVx = (float) (vx * Math.cos(yawAngle) - vy * Math.sin(yawAngle));
            float postLandingWorldVz = (float) (vx * Math.sin(yawAngle) + vy * Math.cos(yawAngle));
            expectedWorldVx = postLandingWorldVx;
            expectedWorldVz = postLandingWorldVz;
        }
        wasAirborne = airborne;

        // ─── LANDING GRIP RECOVERY ───
        if (landingGripPenalty > 0f && !justLanded) {
            landingGripPenalty = Math.max(0f, landingGripPenalty - LANDING_GRIP_RECOVERY_RATE);
        }

        // ── AIRBORNE PHYSICS ──
        if (airborne) {
            float airDt = TICK_TIME;
            // Aerodynamic drag on longitudinal velocity
            float airDragForceX = -0.5f * AIR_DRAG_COEFFICIENT * FRONTAL_AREA * AIR_DENSITY * vx * Math.abs(vx);
            float ax = airDragForceX / config.getEffectiveMass();
            vx += ax * airDt;
            // Aerodynamic drag on lateral velocity (side area ≈ frontal area)
            float airDragForceY = -0.5f * AIR_DRAG_COEFFICIENT * FRONTAL_AREA * AIR_DENSITY * vy * Math.abs(vy);
            float ay = airDragForceY / config.getEffectiveMass();
            vy += ay * airDt;
            // In air: only aerodynamic drag decelerates vy — vehicle preserves trajectory (inertia)
            yawRate *= AIR_YAW_RATE_DAMPING;
            yawAngle += yawRate * airDt;
            axPrev = 0f;
            ayPrev = 0f;

            float newWorldVx = (float) (vx * Math.cos(yawAngle) - vy * Math.sin(yawAngle));
            float newWorldVz = (float) (vx * Math.sin(yawAngle) + vy * Math.cos(yawAngle));
            float mcVx = newWorldVx * TICK_TIME;
            float mcVz = newWorldVz * TICK_TIME;
            float yawDelta = (float) Math.toDegrees(yawRate * TICK_TIME);
            float verticalPitch = MathHelper.clamp(verticalVelocity * VERTICAL_PITCH_FACTOR, -MAX_VERTICAL_PITCH, MAX_VERTICAL_PITCH);

            // ─── VERTICAL TERMINAL VELOCITY ───
            // Clamp fall speed to terminal velocity, then apply drag for gradual deceleration
            float clampedVelY = (float) entityVel.y;
            if (clampedVelY < TERMINAL_FALL_VELOCITY) {
                clampedVelY = Math.max(clampedVelY * VERTICAL_DRAG_FACTOR, TERMINAL_FALL_VELOCITY);
            }

            // Store expected world velocity for next tick's collision detection
            expectedWorldVx = newWorldVx;
            expectedWorldVz = newWorldVz;

            return new RealisticPhysicsEngine.PhysicsResult(mcVx, clampedVelY, mcVz, yawDelta,
                    config.getStaticFrontLoad(), config.getStaticRearLoad(), verticalPitch, 0f, steeringAngle);
        }

        // Weather grip modifier + tire preset
        TirePreset tirePreset = config.tirePreset;
        float weatherGrip = weather.gripMultiplier * tirePreset.gripMultiplier;
        float weatherRelax = weather.relaxationMultiplier * tirePreset.relaxationMultiplier;

        // ─── STEERING DIRECTION CHANGE DETECTION ───
        // When the player reverses steering direction, reset lateral force relaxation
        // to prevent counter-rotation (turning right but initially going left)
        boolean steeringReversed = (steeringInput * prevSteeringInput < 0f);
        if (steeringReversed) {
            for (int i = 0; i < 4; i++) fyActual[i] *= STEERING_REVERSAL_FORCE_RETENTION;
            vy *= STEERING_REVERSAL_VELOCITY_RETENTION;
        }
        prevSteeringInput = steeringInput;

        // Nominal loads for load sensitivity (per wheel = half axle)
        float fzNomFrontWheel = config.getStaticFrontLoad() * 0.5f;
        float fzNomRearWheel = config.getStaticRearLoad() * 0.5f;

        for (int step = 0; step < config.substeps; step++) {
            // ── 0. LOW-SPEED DEAD ZONE ──
            float speed = (float) Math.sqrt(vx * vx + vy * vy);
            boolean isStationary = speed < STOP_SPEED_THRESHOLD && throttleInput < 0.01f;
            if (isStationary) {
                vx = 0f;
                vy = 0f;
                yawRate = 0f;
                axPrev = 0f;
                ayPrev = 0f;
                for (int i = 0; i < 4; i++) fyActual[i] = 0f;
                steeringAngle = 0f;
                fzWheel[0] = fzNomFrontWheel;
                fzWheel[1] = fzNomFrontWheel;
                fzWheel[2] = fzNomRearWheel;
                fzWheel[3] = fzNomRearWheel;
                continue;
            }
            float lowSpeedFade = Math.min(1.0f, speed / LOW_SPEED_FADE_THRESHOLD);

            // ── 1. STEERING ──
            float targetSteering = steeringInput * config.getEffectiveMaxSteeringAngle();
            float steeringDelta = targetSteering - steeringAngle;
            float maxSteerChange = config.getEffectiveSteeringSpeed() * dt;
            steeringAngle += MathHelper.clamp(steeringDelta, -maxSteerChange, maxSteerChange);

            if (Math.abs(steeringInput) < 0.01f && Math.abs(steeringAngle) > 0.001f) {
                float alignRate = config.getEffectiveSteeringReturnRate() * Math.min(1.0f, speed / SELF_ALIGN_SPEED_THRESHOLD);
                steeringAngle -= steeringAngle * alignRate * dt;
            }

            float speedFactor = 1.0f / (1.0f + config.getEffectiveSpeedSteeringFactor() * vx * vx);
            float effectiveSteering = steeringAngle * speedFactor;

            // ── 2. FOUR-WHEEL WEIGHT TRANSFER ──
            float staticFrontTotal = config.getStaticFrontLoad();
            float staticRearTotal = config.getStaticRearLoad();

            // Longitudinal transfer (total)
            float effectiveMass = config.getEffectiveMass();
            float effectiveCgHeight = config.getEffectiveCgHeight();
            float deltaFzLong = (effectiveMass * axPrev * effectiveCgHeight) / config.wheelbase;
            float fzFrontTotal = staticFrontTotal - deltaFzLong;
            float fzRearTotal = staticRearTotal + deltaFzLong;
            fzFrontTotal = Math.max(0f, fzFrontTotal);
            fzRearTotal = Math.max(0f, fzRearTotal);

            // ─── AERODYNAMIC DOWNFORCE ───
            float speedSq = vx * vx;
            float totalDownforce = 0.5f * config.getEffectiveDownforceCoefficient() * AIR_DENSITY * speedSq;
            float downforceFront = totalDownforce * config.downforceFrontBias;
            float downforceRear = totalDownforce * (1.0f - config.downforceFrontBias);
            fzFrontTotal += downforceFront;
            fzRearTotal += downforceRear;

            // Lateral transfer per axle
            float deltaFzLatTotal = Math.abs((effectiveMass * ayPrev * effectiveCgHeight) / config.trackWidth);
            float effectiveRollStiffness = config.getEffectiveRollStiffnessRatio();
            float deltaFzLatFront = deltaFzLatTotal * effectiveRollStiffness;
            float deltaFzLatRear = deltaFzLatTotal * (1.0f - effectiveRollStiffness);

            // Distribute to individual wheels
            // ayPrev > 0 means turning right → left wheels get more load
            float lateralSign = (ayPrev >= 0f) ? 1.0f : -1.0f;

            // Front axle: FL and FR
            fzWheel[WheelPosition.FRONT_LEFT.index] = (fzFrontTotal * 0.5f) + (deltaFzLatFront * lateralSign);
            fzWheel[WheelPosition.FRONT_RIGHT.index] = (fzFrontTotal * 0.5f) - (deltaFzLatFront * lateralSign);
            // Rear axle: RL and RR
            fzWheel[WheelPosition.REAR_LEFT.index] = (fzRearTotal * 0.5f) + (deltaFzLatRear * lateralSign);
            fzWheel[WheelPosition.REAR_RIGHT.index] = (fzRearTotal * 0.5f) - (deltaFzLatRear * lateralSign);

            // Clamp all loads to non-negative
            for (int i = 0; i < 4; i++) fzWheel[i] = Math.max(0f, fzWheel[i]);

            // ── 3. EFFECTIVE MU PER WHEEL ──
            float baseMuPeak = currentSurface.muPeak * weatherGrip;
            float baseMuSlide = currentSurface.muSlide * weatherGrip * tirePreset.slideMultiplier;
            float slideScale = baseMuSlide / Math.max(MIN_MU_PEAK, baseMuPeak);
            float effectiveLoadSensitivity = currentSurface.loadSensitivity + tirePreset.loadSensitivityMod;

            float[] muWheel = this.muWheel;
            for (int i = 0; i < 4; i++) {
                float fzNom = (i < 2) ? fzNomFrontWheel : fzNomRearWheel;
                // Load sensitivity
                float ratio = (fzNom > 0f) ? fzWheel[i] / fzNom : 1.0f;
                muWheel[i] = baseMuPeak * (1.0f - effectiveLoadSensitivity * (ratio - 1.0f));
                muWheel[i] = Math.max(MIN_MU_PEAK, muWheel[i]);

                // Landing grip penalty
                if (landingGripPenalty > 0f) {
                    muWheel[i] *= (1.0f - landingGripPenalty);
                    muWheel[i] = Math.max(MIN_MU_PEAK, muWheel[i]);
                }
            }

            // ── 4. SLIP ANGLES PER WHEEL ──
            // Front wheels use effective steering, rear wheels steer = 0
            // Each wheel has its own lateral velocity component due to yaw rate and track width
            float vyFL = vy + yawRate * Lf;
            float vyFR = vy + yawRate * Lf;
            float vyRL = vy - yawRate * Lr;
            float vyRR = vy - yawRate * Lr;

            // Include yaw rate effect across track width
            vyFL += yawRate * halfTrack;
            vyFR -= yawRate * halfTrack;
            vyRL += yawRate * halfTrack;
            vyRR -= yawRate * halfTrack;

            float vxAbs = Math.max(Math.abs(vx), 1.0f);
            float alphaFL = (float) Math.atan2(vyFL, vxAbs) - effectiveSteering;
            float alphaFR = (float) Math.atan2(vyFR, vxAbs) - effectiveSteering;
            float alphaRL = (float) Math.atan2(vyRL, vxAbs);
            float alphaRR = (float) Math.atan2(vyRR, vxAbs);

            // ── 5. LATERAL FORCES PER WHEEL ──
            float cs = currentSurface.corneringStiffness;
            float peakDeg = currentSurface.peakSlipAngleDeg;
            float falloff = currentSurface.slipAngleFalloff;
            float relaxLen = currentSurface.relaxationLength * weatherRelax;

            float fyFL = TireModel.computeLateralForce(alphaFL, fzWheel[0], muWheel[0], muWheel[0] * slideScale, cs, peakDeg, falloff);
            float fyFR = TireModel.computeLateralForce(alphaFR, fzWheel[1], muWheel[1], muWheel[1] * slideScale, cs, peakDeg, falloff);
            float fyRL = TireModel.computeLateralForce(alphaRL, fzWheel[2], muWheel[2], muWheel[2] * slideScale, cs, peakDeg, falloff);
            float fyRR = TireModel.computeLateralForce(alphaRR, fzWheel[3], muWheel[3], muWheel[3] * slideScale, cs, peakDeg, falloff);

            // Apply relaxation
            fyActual[0] = TireModel.applyRelaxation(fyActual[0], fyFL, Math.abs(vx), dt, relaxLen);
            fyActual[1] = TireModel.applyRelaxation(fyActual[1], fyFR, Math.abs(vx), dt, relaxLen);
            fyActual[2] = TireModel.applyRelaxation(fyActual[2], fyRL, Math.abs(vx), dt, relaxLen);
            fyActual[3] = TireModel.applyRelaxation(fyActual[3], fyRR, Math.abs(vx), dt, relaxLen);

            // ── 6. LONGITUDINAL FORCES ──
            float totalDriveForce = throttleInput * config.getEffectiveEngineForce();

            // Distribute drive force by drivetrain and AWD split
            float frontDriveTotal, rearDriveTotal;
            switch (config.drivetrain) {
                case FWD:
                    frontDriveTotal = totalDriveForce;
                    rearDriveTotal = 0f;
                    break;
                case RWD:
                    frontDriveTotal = 0f;
                    rearDriveTotal = totalDriveForce;
                    break;
                case AWD:
                default:
                    frontDriveTotal = totalDriveForce * config.awdFrontSplit;
                    rearDriveTotal = totalDriveForce * (1.0f - config.awdFrontSplit);
                    break;
            }

            // Distribute within axle via differential
            float[] driveForceWheel = this.driveForceWheel;
            distributeTorque(frontDriveTotal, config.frontDifferential, config.lsdLockingCoeff,
                    fzWheel[0], fzWheel[1], driveForceWheel, 0, 1);
            distributeTorque(rearDriveTotal, config.rearDifferential, config.lsdLockingCoeff,
                    fzWheel[2], fzWheel[3], driveForceWheel, 2, 3);

            // Braking forces
            float effectiveBrakingForce = config.getEffectiveBrakingForce();
            float effectiveBrakeBias = config.getEffectiveBrakeBias();
            float brakeForceFrontTotal = brakeInput * effectiveBrakingForce * effectiveBrakeBias;
            float brakeForceRearTotal = brakeInput * effectiveBrakingForce * (1.0f - effectiveBrakeBias);

            float[] brakeForceWheel = this.brakeForceWheel;
            brakeForceWheel[0] = brakeForceFrontTotal * 0.5f;
            brakeForceWheel[1] = brakeForceFrontTotal * 0.5f;
            brakeForceWheel[2] = brakeForceRearTotal * 0.5f;
            brakeForceWheel[3] = brakeForceRearTotal * 0.5f;

            // Handbrake locks rear wheels
            if (handbrake) {
                float hbForce = effectiveBrakingForce * HANDBRAKE_FORCE_MULTIPLIER * 0.5f;
                brakeForceWheel[2] = hbForce;
                brakeForceWheel[3] = hbForce;
            }

            // Engine braking
            float engineBrake = 0f;
            if (throttleInput < 0.01f && Math.abs(vx) > STOP_SPEED_THRESHOLD) {
                engineBrake = config.getEffectiveEngineBraking() * (vx / Math.max(Math.abs(vx), LOW_SPEED_FADE_THRESHOLD));
            }

            // Compute per-wheel longitudinal force
            float[] fxWheel = this.fxWheel;
            for (int i = 0; i < 4; i++) {
                fxWheel[i] = TireModel.computeLongitudinalForce(driveForceWheel[i], brakeForceWheel[i],
                        fzWheel[i], muWheel[i], vx);
            }

            // ── 7. FRICTION CIRCLE PER WHEEL ──
            for (int i = 0; i < 4; i++) {
                TireModel.FrictionCircleResult result = TireModel.applyFrictionCircle(
                        fxWheel[i], fyActual[i], fzWheel[i], muWheel[i], frictionResults[i]);
                fxWheel[i] = result.fx;
                fyActual[i] = result.fy;
            }

            // ── 8. AERODYNAMIC DRAG ──
            float dragForce = -0.5f * config.getEffectiveDragCoefficient() * FRONTAL_AREA * AIR_DENSITY * vx * Math.abs(vx);
            float rollingResForce = -currentSurface.rollingResistance * effectiveMass * GRAVITY
                    * (vx / Math.max(Math.abs(vx), LOW_SPEED_FADE_THRESHOLD)) * lowSpeedFade;

            // ── 9. SUM FORCES ──
            float totalFx = dragForce + rollingResForce - engineBrake;
            float totalFy = 0f;
            for (int i = 0; i < 4; i++) {
                totalFx += fxWheel[i];
                totalFy += fyActual[i];
            }

            float ax = totalFx / effectiveMass + yawRate * vy;
            float ay = totalFy / effectiveMass - yawRate * vx;

            // Prevent braking from reversing direction
            float newVx = vx + ax * dt;
            if (throttleInput < 0.01f && vx * newVx < 0f) {
                newVx = 0f;
                ax = -vx / dt;
            }

            // ── 10. YAW MOMENT (four-wheel) ──
            // Lateral forces: front axle creates yaw at distance Lf, rear at -Lr
            // Both left and right wheels on same axle contribute same-sign lateral force
            // because they share the same slip angle (steering) — the track-width effect
            // is already captured in the per-wheel slip angle computation above
            float yawMoment = 0f;
            yawMoment += fyActual[0] * Lf;  // FL lateral
            yawMoment += fyActual[1] * Lf;  // FR lateral
            yawMoment -= fyActual[2] * Lr;  // RL lateral
            yawMoment -= fyActual[3] * Lr;  // RR lateral

            // Longitudinal force yaw moments (from track width)
            // Left wheel forward force creates positive yaw (counterclockwise from above)
            // Right wheel forward force creates negative yaw (clockwise from above)
            yawMoment += (fxWheel[0] - fxWheel[1]) * halfTrack * (float) Math.sin(effectiveSteering); // front axle (steered)
            yawMoment += (fxWheel[2] - fxWheel[3]) * halfTrack; // rear axle

            // Moment of inertia: rectangular body
            float inertia = effectiveMass * (config.wheelbase * config.wheelbase + config.trackWidth * config.trackWidth) / 12.0f;
            float yawAccel = yawMoment / inertia;

            // ── 11. INTEGRATE ──
            vx = newVx;
            vy += ay * dt;
            yawRate += yawAccel * dt;
            yawRate *= YAW_RATE_DAMPING * config.suspensionPreset.yawRateDampingMultiplier;

            if (Math.abs(steeringInput) < 0.01f) {
                // When driving straight with low yaw rate, apply stronger damping
                // to prevent residual vy from causing sideways drift
                if (Math.abs(yawRate) < STRAIGHT_LINE_YAW_RATE_THRESHOLD) {
                    vy *= STRAIGHT_LINE_LATERAL_DAMPING;
                } else {
                    vy *= LATERAL_VELOCITY_DAMPING;
                }
            } else {
                // Apply moderate damping during active steering to prevent vy accumulation
                vy *= LATERAL_VELOCITY_DAMPING_ACTIVE;
            }

            // Cap lateral velocity to prevent unbounded drift in sharp turns
            float maxLateralSpeed = Math.max(Math.abs(vx), 2.0f) * MAX_LATERAL_SPEED_RATIO;
            if (Math.abs(vy) > maxLateralSpeed) {
                vy = maxLateralSpeed * Math.signum(vy);
            }

            axPrev = ax;
            ayPrev = ay;
            yawAngle += yawRate * dt;

            // ── 12. HIGH-SPEED SAFETY ──
            // Clamp velocities and yaw rate to prevent numerical instability
            vx = MathHelper.clamp(vx, -MAX_VELOCITY, MAX_VELOCITY);
            vy = MathHelper.clamp(vy, -MAX_VELOCITY, MAX_VELOCITY);
            yawRate = MathHelper.clamp(yawRate, -MAX_YAW_RATE, MAX_YAW_RATE);

            // NaN/Infinity protection — reset to safe state if corrupted
            if (Float.isNaN(vx) || Float.isInfinite(vx) ||
                Float.isNaN(vy) || Float.isInfinite(vy) ||
                Float.isNaN(yawRate) || Float.isInfinite(yawRate)) {
                vx = 0f;
                vy = 0f;
                yawRate = 0f;
                for (int i = 0; i < 4; i++) fyActual[i] = 0f;
                break;
            }
        }

        // Convert back to world frame
        float newWorldVx = (float) (vx * Math.cos(yawAngle) - vy * Math.sin(yawAngle));
        float newWorldVz = (float) (vx * Math.sin(yawAngle) + vy * Math.cos(yawAngle));

        float mcVx = newWorldVx * TICK_TIME;
        float mcVz = newWorldVz * TICK_TIME;
        float yawDelta = (float) Math.toDegrees(yawRate * TICK_TIME);

        // Visual pitch
        float pitchAngle = 0f;
        if (config.getEffectiveMass() > 0f) {
            pitchAngle = -(axPrev / GRAVITY) * 0.25f;
            float verticalPitchContribution = MathHelper.clamp(
                    verticalVelocity * VERTICAL_PITCH_FACTOR, -MAX_VERTICAL_PITCH, MAX_VERTICAL_PITCH);
            pitchAngle += verticalPitchContribution;
        }

        // Visual roll
        float rollAngle = 0f;
        if (config.getEffectiveMass() > 0f) {
            rollAngle = (ayPrev / GRAVITY) * 0.20f;
        }

        float fzFrontTotal = fzWheel[0] + fzWheel[1];
        float fzRearTotal = fzWheel[2] + fzWheel[3];

        // Store expected world velocity for next tick's collision detection
        expectedWorldVx = newWorldVx;
        expectedWorldVz = newWorldVz;

        return new RealisticPhysicsEngine.PhysicsResult(mcVx, (float) entityVel.y, mcVz, yawDelta,
                fzFrontTotal, fzRearTotal, pitchAngle, rollAngle, steeringAngle);
    }

    // ─── DIFFERENTIAL TORQUE DISTRIBUTION ───

    /**
     * Distributes axle torque between left and right wheels based on differential type.
     */
    private static void distributeTorque(float axleTorque, DifferentialType diffType, float lsdCoeff,
                                          float fzLeft, float fzRight,
                                          float[] output, int leftIdx, int rightIdx) {
        if (axleTorque == 0f) {
            output[leftIdx] = 0f;
            output[rightIdx] = 0f;
            return;
        }

        switch (diffType) {
            case LOCKED:
                // Both wheels get equal torque
                output[leftIdx] = axleTorque * 0.5f;
                output[rightIdx] = axleTorque * 0.5f;
                break;

            case OPEN:
                // Torque split proportional to available grip (lower loaded wheel limits both)
                float totalFz = fzLeft + fzRight;
                if (totalFz <= 0f) {
                    output[leftIdx] = axleTorque * 0.5f;
                    output[rightIdx] = axleTorque * 0.5f;
                } else {
                    // Open diff: torque goes to the wheel with less resistance
                    // In practice, split by load ratio (simplified)
                    output[leftIdx] = axleTorque * (fzLeft / totalFz);
                    output[rightIdx] = axleTorque * (fzRight / totalFz);
                }
                break;

            case LSD:
            default:
                // LSD: blend between open and locked based on locking coefficient
                float totalFzLsd = fzLeft + fzRight;
                float openLeft, openRight;
                if (totalFzLsd <= 0f) {
                    openLeft = 0.5f;
                    openRight = 0.5f;
                } else {
                    openLeft = fzLeft / totalFzLsd;
                    openRight = fzRight / totalFzLsd;
                }
                float lockedSplit = 0.5f;
                float leftRatio = openLeft * (1.0f - lsdCoeff) + lockedSplit * lsdCoeff;
                float rightRatio = openRight * (1.0f - lsdCoeff) + lockedSplit * lsdCoeff;
                output[leftIdx] = axleTorque * leftRatio;
                output[rightIdx] = axleTorque * rightRatio;
                break;
        }
    }

    // ─── SURFACE DETECTION ───

    //? >=1.21.3 {
    /*private SurfaceProperties detectSurface(net.minecraft.entity.vehicle.AbstractBoatEntity boat) {
    *///?}
    //? <=1.21 {
    private SurfaceProperties detectSurface(BoatEntity boat) {
    //?}
        Box box = boat.getBoundingBox();
        Box box2 = new Box(box.minX, box.minY - 0.001, box.minZ, box.maxX, box.minY, box.maxZ);
        int i = MathHelper.floor(box2.minX) - 1;
        int j = MathHelper.ceil(box2.maxX) + 1;
        int k = MathHelper.floor(box2.minY) - 1;
        int l = MathHelper.ceil(box2.maxY) + 1;
        int m = MathHelper.floor(box2.minZ) - 1;
        int n = MathHelper.ceil(box2.maxZ) + 1;
        VoxelShape voxelShape = VoxelShapes.cuboid(box2);

        surfaceAccumulator.reset();

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int p = i; p < j; ++p) {
            for (int q = m; q < n; ++q) {
                int r = (p == i || p == j - 1 ? 1 : 0) + (q == m || q == n - 1 ? 1 : 0);
                if (r == 2) continue;
                for (int s = k; s < l; ++s) {
                    if (r > 0 && (s == k || s == l - 1)) continue;
                    mutable.set(p, s, q);
                    BlockState blockState = boat.getWorld().getBlockState(mutable);
                    if (blockState.getBlock() instanceof LilyPadBlock ||
                            !VoxelShapes.matchesAnywhere(blockState.getCollisionShape(boat.getWorld(), mutable).offset(p, s, q),
                                    voxelShape, BooleanBiFunction.AND)) continue;

                    String blockId = Registries.BLOCK.getId(blockState.getBlock()).toString();
                    SurfaceProperties surface = SurfaceProperties.getSurfaceForBlock(blockId);
                    surfaceAccumulator.accumulate(surface);
                }
            }
        }

        return surfaceAccumulator.getResult();
    }

    // ─── GETTERS ───
    public float getVx() { return vx; }
    public float getVy() { return vy; }
    public float getYawRate() { return yawRate; }
    public float getSteeringAngle() { return steeringAngle; }
    public float getFzWheel(WheelPosition pos) { return fzWheel[pos.index]; }
    public SurfaceProperties getCurrentSurface() { return currentSurface; }
    public float getLandingGripPenalty() { return landingGripPenalty; }
    public float getVerticalVelocity() { return verticalVelocity; }
}
