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
    private float vx = 0f;        // longitudinal velocity in vehicle frame (m/s)
    private float vy = 0f;        // lateral velocity in vehicle frame (m/s)
    private float yawAngle = 0f;  // heading angle (rad)
    private float yawRate = 0f;   // yaw rate (rad/s)
    private float steeringAngle = 0f; // actual steering angle with delay (rad)

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
    private static final float LANDING_GRIP_RECOVERY_RATE = 0.08f;
    private static final float VERTICAL_PITCH_FACTOR = 0.15f;
    private static final float MAX_VERTICAL_PITCH = 0.5f;
    private static final int MIN_AIRBORNE_TICKS_FOR_IMPACT = 3;

    // ─── STEERING STABILITY ───
    private static final float SELF_ALIGN_BASE_RATE = 3.0f;
    private static final float SELF_ALIGN_SPEED_THRESHOLD = 5.0f;
    private static final float LATERAL_VELOCITY_DAMPING = 0.95f;
    private static final float HANDBRAKE_FORCE_MULTIPLIER = 0.5f;

    // ─── AIRBORNE STATE ───
    private boolean airborne = false;
    private boolean wasAirborne = false;
    private int airborneTicks = 0;
    private float landingGripPenalty = 0f;
    private float verticalVelocity = 0f;
    private float prevVerticalVelocity = 0f;

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
        vx = (float) (worldVx * Math.cos(entityYaw) + worldVz * Math.sin(entityYaw));
        vy = (float) (-worldVx * Math.sin(entityYaw) + worldVz * Math.cos(entityYaw));
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
            airborneTicks = 0;
        }
        wasAirborne = airborne;

        // ─── LANDING GRIP RECOVERY ───
        if (landingGripPenalty > 0f && !justLanded) {
            landingGripPenalty = Math.max(0f, landingGripPenalty - LANDING_GRIP_RECOVERY_RATE);
        }

        // ── AIRBORNE PHYSICS ──
        if (airborne) {
            float airDt = TICK_TIME;
            float airDragForce = -0.5f * AIR_DRAG_COEFFICIENT * FRONTAL_AREA * AIR_DENSITY * vx * Math.abs(vx);
            float ax = airDragForce / config.mass;
            vx += ax * airDt;
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

            return new RealisticPhysicsEngine.PhysicsResult(mcVx, (float) entityVel.y, mcVz, yawDelta,
                    config.getStaticFrontLoad(), config.getStaticRearLoad(), verticalPitch, 0f, steeringAngle);
        }

        // Weather grip modifier
        float weatherGrip = weather.gripMultiplier;
        float weatherRelax = weather.relaxationMultiplier;

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
            float targetSteering = steeringInput * config.maxSteeringAngle;
            float steeringDelta = targetSteering - steeringAngle;
            float maxSteerChange = config.steeringSpeed * dt;
            steeringAngle += MathHelper.clamp(steeringDelta, -maxSteerChange, maxSteerChange);

            if (Math.abs(steeringInput) < 0.01f && Math.abs(steeringAngle) > 0.001f) {
                float alignRate = SELF_ALIGN_BASE_RATE * Math.min(1.0f, speed / SELF_ALIGN_SPEED_THRESHOLD);
                steeringAngle -= steeringAngle * alignRate * dt;
            }

            float speedFactor = 1.0f / (1.0f + config.speedSteeringFactor * vx * vx);
            float effectiveSteering = steeringAngle * speedFactor;

            // ── 2. FOUR-WHEEL WEIGHT TRANSFER ──
            float staticFrontTotal = config.getStaticFrontLoad();
            float staticRearTotal = config.getStaticRearLoad();

            // Longitudinal transfer (total)
            float deltaFzLong = (config.mass * axPrev * config.cgHeight) / config.wheelbase;
            float fzFrontTotal = staticFrontTotal - deltaFzLong;
            float fzRearTotal = staticRearTotal + deltaFzLong;
            fzFrontTotal = Math.max(0f, fzFrontTotal);
            fzRearTotal = Math.max(0f, fzRearTotal);

            // ─── AERODYNAMIC DOWNFORCE ───
            float speedSq = vx * vx;
            float totalDownforce = 0.5f * config.downforceCoefficient * AIR_DENSITY * speedSq;
            float downforceFront = totalDownforce * config.downforceFrontBias;
            float downforceRear = totalDownforce * (1.0f - config.downforceFrontBias);
            fzFrontTotal += downforceFront;
            fzRearTotal += downforceRear;

            // Lateral transfer per axle
            float deltaFzLatTotal = Math.abs((config.mass * ayPrev * config.cgHeight) / config.trackWidth);
            float deltaFzLatFront = deltaFzLatTotal * config.rollStiffnessRatioFront;
            float deltaFzLatRear = deltaFzLatTotal * (1.0f - config.rollStiffnessRatioFront);

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
            float baseMuSlide = currentSurface.muSlide * weatherGrip;
            float slideScale = baseMuSlide / Math.max(MIN_MU_PEAK, baseMuPeak);

            float[] muWheel = this.muWheel;
            for (int i = 0; i < 4; i++) {
                float fzNom = (i < 2) ? fzNomFrontWheel : fzNomRearWheel;
                // Load sensitivity
                float ratio = (fzNom > 0f) ? fzWheel[i] / fzNom : 1.0f;
                muWheel[i] = baseMuPeak * (1.0f - currentSurface.loadSensitivity * (ratio - 1.0f));
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
            float totalDriveForce = throttleInput * config.engineForce;

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
            float brakeForceFrontTotal = brakeInput * config.brakingForce * config.brakeBias;
            float brakeForceRearTotal = brakeInput * config.brakingForce * (1.0f - config.brakeBias);

            float[] brakeForceWheel = this.brakeForceWheel;
            brakeForceWheel[0] = brakeForceFrontTotal * 0.5f;
            brakeForceWheel[1] = brakeForceFrontTotal * 0.5f;
            brakeForceWheel[2] = brakeForceRearTotal * 0.5f;
            brakeForceWheel[3] = brakeForceRearTotal * 0.5f;

            // Handbrake locks rear wheels
            if (handbrake) {
                float hbForce = config.brakingForce * HANDBRAKE_FORCE_MULTIPLIER * 0.5f;
                brakeForceWheel[2] = hbForce;
                brakeForceWheel[3] = hbForce;
            }

            // Engine braking
            float engineBrake = 0f;
            if (throttleInput < 0.01f && Math.abs(vx) > STOP_SPEED_THRESHOLD) {
                engineBrake = config.engineBraking * (vx / Math.max(Math.abs(vx), LOW_SPEED_FADE_THRESHOLD));
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
            float dragForce = -0.5f * config.dragCoefficient * FRONTAL_AREA * AIR_DENSITY * vx * Math.abs(vx);
            float rollingResForce = -currentSurface.rollingResistance * config.mass * GRAVITY
                    * (vx / Math.max(Math.abs(vx), LOW_SPEED_FADE_THRESHOLD)) * lowSpeedFade;

            // ── 9. SUM FORCES ──
            float totalFx = dragForce + rollingResForce - engineBrake;
            float totalFy = 0f;
            for (int i = 0; i < 4; i++) {
                totalFx += fxWheel[i];
                totalFy += fyActual[i];
            }

            float ax = totalFx / config.mass + yawRate * vy;
            float ay = totalFy / config.mass - yawRate * vx;

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
            // Left wheels push forward → positive yaw, right wheels push forward → negative yaw
            yawMoment += (-fxWheel[0] + fxWheel[1]) * halfTrack * (float) Math.sin(effectiveSteering); // front axle
            yawMoment += (-fxWheel[2] + fxWheel[3]) * halfTrack; // rear axle

            // Moment of inertia: rectangular body
            float inertia = config.mass * (config.wheelbase * config.wheelbase + config.trackWidth * config.trackWidth) / 12.0f;
            float yawAccel = yawMoment / inertia;

            // ── 11. INTEGRATE ──
            vx = newVx;
            vy += ay * dt;
            yawRate += yawAccel * dt;
            yawRate *= YAW_RATE_DAMPING;

            if (Math.abs(steeringInput) < 0.01f) {
                vy *= LATERAL_VELOCITY_DAMPING;
            }

            axPrev = ax;
            ayPrev = ay;
            yawAngle += yawRate * dt;
        }

        // Convert back to world frame
        float newWorldVx = (float) (vx * Math.cos(yawAngle) - vy * Math.sin(yawAngle));
        float newWorldVz = (float) (vx * Math.sin(yawAngle) + vy * Math.cos(yawAngle));

        float mcVx = newWorldVx * TICK_TIME;
        float mcVz = newWorldVz * TICK_TIME;
        float yawDelta = (float) Math.toDegrees(yawRate * TICK_TIME);

        // Visual pitch
        float pitchAngle = 0f;
        if (config.mass > 0f) {
            pitchAngle = -(axPrev / GRAVITY) * 0.25f;
            float verticalPitchContribution = MathHelper.clamp(
                    verticalVelocity * VERTICAL_PITCH_FACTOR, -MAX_VERTICAL_PITCH, MAX_VERTICAL_PITCH);
            pitchAngle += verticalPitchContribution;
        }

        // Visual roll
        float rollAngle = 0f;
        if (config.mass > 0f) {
            rollAngle = (ayPrev / GRAVITY) * 0.20f;
        }

        float fzFrontTotal = fzWheel[0] + fzWheel[1];
        float fzRearTotal = fzWheel[2] + fzWheel[3];

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

        float totalMu = 0f, totalMuSlide = 0f, totalCs = 0f;
        float totalRelax = 0f, totalRolling = 0f, totalPeak = 0f;
        float totalFalloff = 0f, totalLoadSens = 0f;
        int count = 0;

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
                    totalMu += surface.muPeak;
                    totalMuSlide += surface.muSlide;
                    totalCs += surface.corneringStiffness;
                    totalRelax += surface.relaxationLength;
                    totalRolling += surface.rollingResistance;
                    totalPeak += surface.peakSlipAngleDeg;
                    totalFalloff += surface.slipAngleFalloff;
                    totalLoadSens += surface.loadSensitivity;
                    count++;
                }
            }
        }

        if (count == 0) return SurfaceProperties.ASPHALT_DRY;

        return new SurfaceProperties(
                totalMu / count, totalMuSlide / count, totalCs / count,
                totalRelax / count, totalRolling / count, totalPeak / count,
                totalFalloff / count, totalLoadSens / count
        );
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
