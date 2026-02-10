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

public class RealisticPhysicsEngine {

    // ─── PERSISTENT STATE ───
    private float vx = 0f;        // longitudinal velocity in vehicle frame (m/s)
    private float vy = 0f;        // lateral velocity in vehicle frame (m/s)
    private float yawAngle = 0f;  // heading angle (rad)
    private float yawRate = 0f;   // yaw rate (rad/s)
    private float steeringAngle = 0f; // actual steering angle with delay (rad)

    // Previous tick accelerations for weight transfer
    private float axPrev = 0f;
    private float ayPrev = 0f;

    // Relaxation state (tire forces)
    private float fyFrontActual = 0f;
    private float fyRearActual = 0f;

    // Vertical loads
    private float fzFront;
    private float fzRear;

    // Track which boat entity we are simulating to reset state on boat change
    private int lastBoatId = -1;

    // Configuration
    private VehicleConfig config;
    private boolean enabled = false;

    // Current surface
    private SurfaceProperties currentSurface = SurfaceProperties.ASPHALT_DRY;

    // Reusable friction circle result objects (per-engine instance, not static)
    private final TireModel.FrictionCircleResult frictionResultFront = new TireModel.FrictionCircleResult();
    private final TireModel.FrictionCircleResult frictionResultRear = new TireModel.FrictionCircleResult();

    // Reusable surface accumulator to avoid per-tick allocation
    private final SurfaceProperties.SurfaceAccumulator surfaceAccumulator = new SurfaceProperties.SurfaceAccumulator();

    private static final float GRAVITY = 9.81f;
    private static final float TICK_TIME = 0.05f; // 20 TPS = 50ms per tick
    private static final float MIN_MU_PEAK = 0.01f;
    private static final float YAW_RATE_DAMPING = 0.995f;

    // ─── LOW-SPEED DEAD ZONE ───
    // Below this threshold, the vehicle is considered fully stopped to prevent oscillation
    private static final float STOP_SPEED_THRESHOLD = 0.15f;
    // Forces are smoothly scaled down when speed is below this value
    private static final float LOW_SPEED_FADE_THRESHOLD = 0.5f;

    // ─── AIRBORNE PHYSICS ───
    // Minimal air drag coefficient (only aerodynamic resistance, no tire/rolling forces)
    private static final float AIR_DRAG_COEFFICIENT = 0.35f;
    // Air density for aerodynamic drag (kg/m³)
    private static final float AIR_DENSITY = 1.225f;
    // Frontal area approximation for air drag (m²)
    private static final float FRONTAL_AREA = 2.0f;
    // Yaw rate damping in air (steering has minimal effect)
    private static final float AIR_YAW_RATE_DAMPING = 0.998f;

    // ─── VERTICAL PHYSICS ───
    // Vertical velocity threshold to consider as "landing impact" (m/s, negative = falling)
    private static final float LANDING_IMPACT_THRESHOLD = -1.5f;
    // Maximum grip reduction on landing (0.0 = full grip, 1.0 = no grip)
    private static final float MAX_LANDING_GRIP_LOSS = 0.6f;
    // How fast grip recovers after landing (per tick, 0-1)
    private static final float LANDING_GRIP_RECOVERY_RATE = 0.08f;
    // Pitch angle contribution from vertical velocity (visual nose-up during jumps)
    private static final float VERTICAL_PITCH_FACTOR = 0.15f;
    // Maximum vertical pitch contribution (degrees, before scaling in mixin)
    private static final float MAX_VERTICAL_PITCH = 0.5f;
    // Minimum airborne time (ticks) before grip penalty applies on landing
    private static final int MIN_AIRBORNE_TICKS_FOR_IMPACT = 3;

    // ─── STEERING STABILITY ───
    // Self-aligning torque base rate (how fast wheels return to center)
    private static final float SELF_ALIGN_BASE_RATE = 3.0f;
    // Speed threshold for full self-alignment effect (m/s)
    private static final float SELF_ALIGN_SPEED_THRESHOLD = 5.0f;
    // Lateral velocity damping when no steering input (prevents drifting without input)
    private static final float LATERAL_VELOCITY_DAMPING = 0.95f;
    // Handbrake force as fraction of total braking force
    private static final float HANDBRAKE_FORCE_MULTIPLIER = 0.5f;

    // Track whether vehicle is airborne for update logic
    private boolean airborne = false;
    // Track previous airborne state for landing detection
    private boolean wasAirborne = false;
    // Count ticks spent airborne (for impact calculation)
    private int airborneTicks = 0;
    // Current grip reduction factor from landing impact (0 = full grip, 1 = no grip)
    private float landingGripPenalty = 0f;
    // Track vertical velocity for pitch and impact calculation
    private float verticalVelocity = 0f;
    // Previous tick's vertical velocity (used for landing impact — current tick may have zeroed velocity)
    private float prevVerticalVelocity = 0f;

    public RealisticPhysicsEngine() {
        this.config = VehicleConfig.createDefault();
        resetState();
    }

    public void setConfig(VehicleConfig config) {
        this.config = config;
        resetState();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void resetState() {
        vx = 0f;
        vy = 0f;
        yawAngle = 0f;
        yawRate = 0f;
        steeringAngle = 0f;
        axPrev = 0f;
        ayPrev = 0f;
        fyFrontActual = 0f;
        fyRearActual = 0f;
        fzFront = config.getStaticFrontLoad();
        fzRear = config.getStaticRearLoad();
        lastBoatId = -1;
        wasAirborne = false;
        airborneTicks = 0;
        landingGripPenalty = 0f;
        verticalVelocity = 0f;
        prevVerticalVelocity = 0f;
    }

    public VehicleConfig getConfig() {
        return config;
    }

    public void setAirborne(boolean airborne) {
        this.airborne = airborne;
    }

    public boolean isAirborne() {
        return airborne;
    }

    //? >=1.21.3 {
    /*public PhysicsResult update(net.minecraft.entity.vehicle.AbstractBoatEntity boat,
                                float steeringInput, float throttleInput, float brakeInput, boolean handbrake) {
    *///?}
    //? <=1.21 {
    public PhysicsResult update(BoatEntity boat,
                                float steeringInput, float throttleInput, float brakeInput, boolean handbrake) {
    //?}
        if (!enabled) return null;

        // Reset state when controlled boat changes to avoid state leaking between boats
        int boatId = boat.getId();
        if (boatId != lastBoatId) {
            resetState();
            lastBoatId = boatId;
        }

        // Validate configuration to prevent division by zero
        if (config.wheelbase <= 0.01f || config.trackWidth <= 0.01f || config.mass <= 0f || config.substeps <= 0) return null;

        // Detect current surface from blocks below boat
        currentSurface = detectSurface(boat);

        // Apply load sensitivity to surface mu
        float fzNominalFront = config.getStaticFrontLoad();
        float fzNominalRear = config.getStaticRearLoad();

        float dt = TICK_TIME / config.substeps;

        // Initialize velocities from entity if needed
        Vec3d entityVel = boat.getVelocity();
        // Minecraft yaw: 0° = South (+Z), 90° = West (-X), so offset by +90° for standard math frame
        float entityYaw = (float) Math.toRadians(boat.getYaw()) + (float)(Math.PI / 2.0);

        // Convert world velocity (blocks/tick) to m/s and then to vehicle frame
        float worldVx = (float) (entityVel.x / TICK_TIME);
        float worldVz = (float) (entityVel.z / TICK_TIME);
        vx = (float) (worldVx * Math.cos(entityYaw) + worldVz * Math.sin(entityYaw));
        vy = (float) (-worldVx * Math.sin(entityYaw) + worldVz * Math.cos(entityYaw));
        yawAngle = entityYaw;

        float Lf = config.getFrontAxleDistance();
        float Lr = config.getRearAxleDistance();

        // ─── VERTICAL VELOCITY TRACKING ───
        // entityVel.y is blocks/tick; divide by TICK_TIME (0.05s) to get m/s (1 block ≈ 1 meter)
        // Save previous velocity before overwriting — used for landing impact calculation
        // because at the landing frame, Minecraft may have already zeroed the vertical velocity
        prevVerticalVelocity = verticalVelocity;
        verticalVelocity = (float) (entityVel.y / TICK_TIME);

        // ─── AIRBORNE STATE TRACKING ───
        if (airborne) {
            airborneTicks++;
        }

        // ─── LANDING DETECTION ───
        // Detect transition from airborne to grounded
        // Use prevVerticalVelocity for impact calculation because at landing frame
        // Minecraft collision may have already zeroed the current vertical velocity
        boolean justLanded = false;
        if (wasAirborne && !airborne) {
            // Calculate landing impact based on vertical velocity and time in air
            float landingVelocity = Math.min(prevVerticalVelocity, verticalVelocity);
            if (airborneTicks >= MIN_AIRBORNE_TICKS_FOR_IMPACT && landingVelocity < LANDING_IMPACT_THRESHOLD) {
                // Harder landing = more grip loss, scaled by impact severity
                float impactSeverity = Math.min(1.0f, Math.abs(landingVelocity - LANDING_IMPACT_THRESHOLD) / 8.0f);
                landingGripPenalty = Math.min(MAX_LANDING_GRIP_LOSS, impactSeverity * MAX_LANDING_GRIP_LOSS);
                justLanded = true;
            }
            airborneTicks = 0;
        }
        wasAirborne = airborne;

        // ─── LANDING GRIP RECOVERY ───
        // Gradually recover grip after landing impact (skip recovery on the landing frame itself)
        if (landingGripPenalty > 0f && !justLanded) {
            landingGripPenalty = Math.max(0f, landingGripPenalty - LANDING_GRIP_RECOVERY_RATE);
        }

        // ── AIRBORNE PHYSICS: skip tire forces, only apply aerodynamic drag ──
        if (airborne) {
            float airDt = TICK_TIME;

            // Only aerodynamic drag in air (no tire forces, no rolling resistance)
            float airDragForce = -0.5f * AIR_DRAG_COEFFICIENT * FRONTAL_AREA * AIR_DENSITY * vx * Math.abs(vx);
            float ax = airDragForce / config.mass;
            vx += ax * airDt;

            // Yaw rate slowly decays in air (no steering authority)
            yawRate *= AIR_YAW_RATE_DAMPING;
            yawAngle += yawRate * airDt;

            // No lateral force changes in air
            // Reset weight transfer state
            axPrev = 0f;
            ayPrev = 0f;

            // Convert vehicle-frame velocity back to world frame
            float newWorldVx = (float) (vx * Math.cos(yawAngle) - vy * Math.sin(yawAngle));
            float newWorldVz = (float) (vx * Math.sin(yawAngle) + vy * Math.cos(yawAngle));

            float mcVx = newWorldVx * TICK_TIME;
            float mcVz = newWorldVz * TICK_TIME;
            float yawDelta = (float) Math.toDegrees(yawRate * TICK_TIME);

            // Visual pitch from vertical velocity: nose up when rising, nose down when falling
            float verticalPitch = MathHelper.clamp(verticalVelocity * VERTICAL_PITCH_FACTOR, -MAX_VERTICAL_PITCH, MAX_VERTICAL_PITCH);

            return new PhysicsResult(mcVx, (float) entityVel.y, mcVz, yawDelta,
                    config.getStaticFrontLoad(), config.getStaticRearLoad(), verticalPitch, 0f, steeringAngle);
        }

        for (int step = 0; step < config.substeps; step++) {
            // ── 0. LOW-SPEED DEAD ZONE ──
            // Prevent oscillation when vehicle is nearly stopped
            float speed = (float) Math.sqrt(vx * vx + vy * vy);
            boolean isStationary = speed < STOP_SPEED_THRESHOLD && throttleInput < 0.01f;
            if (isStationary) {
                vx = 0f;
                vy = 0f;
                yawRate = 0f;
                axPrev = 0f;
                ayPrev = 0f;
                fyFrontActual = 0f;
                fyRearActual = 0f;
                steeringAngle = 0f;
                fzFront = config.getStaticFrontLoad();
                fzRear = config.getStaticRearLoad();
                continue;
            }
            // Smooth fade factor for forces near zero speed (prevents signum oscillation)
            float lowSpeedFade = Math.min(1.0f, speed / LOW_SPEED_FADE_THRESHOLD);

            // ── 1. STEERING with rate limiting and speed-dependent ratio ──
            float targetSteering = steeringInput * config.maxSteeringAngle;
            float steeringDelta = targetSteering - steeringAngle;
            float maxSteerChange = config.steeringSpeed * dt;
            steeringAngle += MathHelper.clamp(steeringDelta, -maxSteerChange, maxSteerChange);

            // Self-aligning torque: when no steering input, wheels return to center faster at speed
            if (Math.abs(steeringInput) < 0.01f && Math.abs(steeringAngle) > 0.001f) {
                float alignRate = SELF_ALIGN_BASE_RATE * Math.min(1.0f, speed / SELF_ALIGN_SPEED_THRESHOLD);
                steeringAngle -= steeringAngle * alignRate * dt;
            }

            // Speed-dependent steering reduction
            float speedFactor = 1.0f / (1.0f + config.speedSteeringFactor * vx * vx);
            float effectiveSteering = steeringAngle * speedFactor;

            // ── 2. WEIGHT TRANSFER ──
            // Longitudinal transfer: braking (ax < 0) loads front, acceleration (ax > 0) loads rear
            // ΔFz = m * ax * h / L is negative during braking
            // Front gains load during braking: Fz_front = static - ΔFz (subtracting negative = adding)
            float deltaFzLong = (config.mass * axPrev * config.cgHeight) / config.wheelbase;
            fzFront = config.getStaticFrontLoad() - deltaFzLong;
            fzRear = config.getStaticRearLoad() + deltaFzLong;

            // Clamp loads to non-negative
            fzFront = Math.max(0f, fzFront);
            fzRear = Math.max(0f, fzRear);

            // ── 3. LOAD SENSITIVITY ──
            float muFront = TireModel.computeEffectiveMu(fzFront, fzNominalFront, currentSurface);
            float muRear = TireModel.computeEffectiveMu(fzRear, fzNominalRear, currentSurface);

            // Lateral weight transfer reduces effective grip (load redistribution between inner/outer wheels)
            // This models the non-linear tire: average grip of unequally loaded tires < grip at average load
            float deltaFzLat = Math.abs((config.mass * ayPrev * config.cgHeight) / config.trackWidth);
            float latGripLossFront = deltaFzLat * config.rollStiffnessRatioFront;
            float latGripLossRear = deltaFzLat * (1.0f - config.rollStiffnessRatioFront);
            // Reduce effective mu proportionally (capped so mu doesn't go below minimum)
            if (fzFront > 0f) {
                float latLossRatioFront = Math.min(0.5f, latGripLossFront / fzFront);
                muFront *= (1.0f - latLossRatioFront);
            }
            if (fzRear > 0f) {
                float latLossRatioRear = Math.min(0.5f, latGripLossRear / fzRear);
                muRear *= (1.0f - latLossRatioRear);
            }
            muFront = Math.max(MIN_MU_PEAK, muFront);
            muRear = Math.max(MIN_MU_PEAK, muRear);

            // ── 3b. LANDING IMPACT GRIP REDUCTION ──
            // After a hard landing, tires temporarily lose grip due to suspension compression
            if (landingGripPenalty > 0f) {
                float gripMultiplier = 1.0f - landingGripPenalty;
                muFront *= gripMultiplier;
                muRear *= gripMultiplier;
                muFront = Math.max(MIN_MU_PEAK, muFront);
                muRear = Math.max(MIN_MU_PEAK, muRear);
            }

            // Precompute slide scaling from base surface properties
            float baseMuPeak = currentSurface.muPeak;
            float baseMuSlide = currentSurface.muSlide;
            float slideScale = baseMuSlide / Math.max(MIN_MU_PEAK, baseMuPeak);

            // ── 4. SLIP ANGLES ──
            float alphaFront = TireModel.computeSlipAngle(vy, vx, yawRate, Lf, effectiveSteering);
            float alphaRear = TireModel.computeSlipAngle(vy, vx, yawRate, -Lr, 0f);

            // ── 5. LATERAL FORCES (with Fiala model) ──
            // Use per-axle mu values without mutating the shared surface object
            float muSlideFront = muFront * slideScale;
            float muSlideRear = muRear * slideScale;
            float fyFrontTarget = TireModel.computeLateralForce(alphaFront, fzFront, muFront, muSlideFront,
                    currentSurface.corneringStiffness, currentSurface.peakSlipAngleDeg, currentSurface.slipAngleFalloff);
            float fyRearTarget = TireModel.computeLateralForce(alphaRear, fzRear, muRear, muSlideRear,
                    currentSurface.corneringStiffness, currentSurface.peakSlipAngleDeg, currentSurface.slipAngleFalloff);

            // Apply relaxation length
            fyFrontActual = TireModel.applyRelaxation(fyFrontActual, fyFrontTarget,
                    Math.abs(vx), dt, currentSurface.relaxationLength);
            fyRearActual = TireModel.applyRelaxation(fyRearActual, fyRearTarget,
                    Math.abs(vx), dt, currentSurface.relaxationLength);

            // ── 6. LONGITUDINAL FORCES ──
            float driveForce = throttleInput * config.engineForce;
            float brakeForceFront = brakeInput * config.brakingForce * config.brakeBias;
            float brakeForceRear = brakeInput * config.brakingForce * (1.0f - config.brakeBias);

            // Handbrake locks rear wheels (reduced force for controllable drifting)
            if (handbrake) {
                brakeForceRear = config.brakingForce * HANDBRAKE_FORCE_MULTIPLIER;
            }

            // Engine braking when no throttle (smoothly faded at low speed)
            float engineBrake = 0f;
            if (throttleInput < 0.01f && Math.abs(vx) > STOP_SPEED_THRESHOLD) {
                engineBrake = config.engineBraking * (vx / Math.max(Math.abs(vx), LOW_SPEED_FADE_THRESHOLD));
            }

            // Drivetrain: distribute drive force between front and rear axles
            float frontDriveRatio = config.drivetrain.getFrontDriveRatio();
            float driveForceFront = driveForce * frontDriveRatio;
            float driveForceRear = driveForce * (1.0f - frontDriveRatio);

            // Temporarily set per-axle mu for longitudinal force computation
            float fxFront = TireModel.computeLongitudinalForce(driveForceFront, brakeForceFront, fzFront, muFront, vx);
            float fxRear = TireModel.computeLongitudinalForce(driveForceRear, brakeForceRear, fzRear, muRear, vx);

            // ── 7. FRICTION CIRCLE CONSTRAINT ──
            TireModel.FrictionCircleResult frontForces = TireModel.applyFrictionCircle(fxFront, fyFrontActual, fzFront, muFront, frictionResultFront);
            fxFront = frontForces.fx;
            fyFrontActual = frontForces.fy;
            TireModel.FrictionCircleResult rearForces = TireModel.applyFrictionCircle(fxRear, fyRearActual, fzRear, muRear, frictionResultRear);
            fxRear = rearForces.fx;
            fyRearActual = rearForces.fy;

            // ── 8. AERODYNAMIC DRAG ──
            float dragForce = -0.5f * config.dragCoefficient * 2.0f * 1.225f * vx * Math.abs(vx);
            // Rolling resistance (smoothly faded at low speed to prevent oscillation)
            float rollingResForce = -currentSurface.rollingResistance * config.mass * GRAVITY
                    * (vx / Math.max(Math.abs(vx), LOW_SPEED_FADE_THRESHOLD)) * lowSpeedFade;

            // ── 9. SUM FORCES AND COMPUTE ACCELERATIONS ──
            float totalFx = fxFront + fxRear + dragForce + rollingResForce - engineBrake;
            float totalFy = fyFrontActual + fyRearActual;

            // Linear accelerations in vehicle frame
            float ax = totalFx / config.mass + yawRate * vy;
            float ay = totalFy / config.mass - yawRate * vx;

            // Prevent braking/rolling resistance from reversing direction
            float newVx = vx + ax * dt;
            if (throttleInput < 0.01f && vx * newVx < 0f) {
                newVx = 0f;
                ax = -vx / dt;
            }

            // Yaw moment: front lateral force * Lf - rear lateral force * Lr
            float yawMoment = fyFrontActual * Lf - fyRearActual * Lr;
            // Moment of inertia for a rectangular body about vertical axis: I = m·(L² + W²)/12
            float inertia = config.mass * (config.wheelbase * config.wheelbase + config.trackWidth * config.trackWidth) / 12.0f;
            float yawAccel = yawMoment / inertia;

            // ── 10. INTEGRATE ──
            vx = newVx;
            vy += ay * dt;
            yawRate += yawAccel * dt;

            // Dampen yaw rate slightly (numerical stability)
            yawRate *= YAW_RATE_DAMPING;

            // Straight-line stability: dampen lateral velocity when no steering input
            // This prevents the vehicle from drifting sideways without driver input
            if (Math.abs(steeringInput) < 0.01f) {
                vy *= LATERAL_VELOCITY_DAMPING;
            }

            // Store accelerations for next step's weight transfer
            axPrev = ax;
            ayPrev = ay;

            // Update yaw angle
            yawAngle += yawRate * dt;
        }

        // Convert vehicle-frame velocity back to world frame
        float newWorldVx = (float) (vx * Math.cos(yawAngle) - vy * Math.sin(yawAngle));
        float newWorldVz = (float) (vx * Math.sin(yawAngle) + vy * Math.cos(yawAngle));

        // Scale from meters/s to Minecraft blocks/tick
        // In Minecraft, 1 block ≈ 1 meter, velocity is blocks/tick (1 tick = 0.05s)
        float mcVx = newWorldVx * TICK_TIME;
        float mcVz = newWorldVz * TICK_TIME;

        // Yaw change in degrees
        float yawDelta = (float) Math.toDegrees(yawRate * TICK_TIME);

        // Visual angles from weight transfer (dimensionless scale factors, converted to degrees in BoatMixin)
        // Pitch: based on longitudinal acceleration (negative ax = nose dips under braking)
        float pitchAngle = 0f;
        if (config.mass > 0f) {
            pitchAngle = -(axPrev / GRAVITY) * 0.25f; // dimensionless, multiplied by 25 in BoatMixin for degrees

            // Add vertical velocity contribution for visual pitch on bumps
            // Positive verticalVelocity (rising) = nose tilts up, negative (falling) = nose tilts down
            float verticalPitchContribution = MathHelper.clamp(
                    verticalVelocity * VERTICAL_PITCH_FACTOR, -MAX_VERTICAL_PITCH, MAX_VERTICAL_PITCH);
            pitchAngle += verticalPitchContribution;
        }
        // Roll: based on lateral acceleration (cornering lean)
        float rollAngle = 0f;
        if (config.mass > 0f) {
            rollAngle = (ayPrev / GRAVITY) * 0.20f; // dimensionless, used for visual roll in BoatMixin
        }

        return new PhysicsResult(mcVx, (float) entityVel.y, mcVz, yawDelta, fzFront, fzRear,
                pitchAngle, rollAngle, steeringAngle);
    }

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

    // ─── GETTERS FOR DEBUG/DISPLAY ───

    public float getVx() { return vx; }
    public float getVy() { return vy; }
    public float getYawRate() { return yawRate; }
    public float getSteeringAngle() { return steeringAngle; }
    public float getFzFront() { return fzFront; }
    public float getFzRear() { return fzRear; }
    public SurfaceProperties getCurrentSurface() { return currentSurface; }
    public float getLandingGripPenalty() { return landingGripPenalty; }
    public float getVerticalVelocity() { return verticalVelocity; }

    public static class PhysicsResult {
        public final float velocityX;
        public final float velocityY;
        public final float velocityZ;
        public final float yawDelta;
        public final float fzFront;
        public final float fzRear;
        public final float pitchAngle;  // nose up/down from weight transfer (rad)
        public final float rollAngle;   // body lean from lateral forces (rad)
        public final float steeringAngle; // current steering wheel angle (rad)

        public PhysicsResult(float velocityX, float velocityY, float velocityZ,
                             float yawDelta, float fzFront, float fzRear,
                             float pitchAngle, float rollAngle, float steeringAngle) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.yawDelta = yawDelta;
            this.fzFront = fzFront;
            this.fzRear = fzRear;
            this.pitchAngle = pitchAngle;
            this.rollAngle = rollAngle;
            this.steeringAngle = steeringAngle;
        }
    }
}
