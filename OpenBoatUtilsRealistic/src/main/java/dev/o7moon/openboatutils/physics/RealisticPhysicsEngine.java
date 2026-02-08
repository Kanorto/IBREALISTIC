package dev.o7moon.openboatutils.physics;

import net.minecraft.block.Block;
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

    private static final float GRAVITY = 9.81f;
    private static final float TICK_TIME = 0.05f; // 20 TPS = 50ms per tick
    private static final float MIN_MU_PEAK = 0.01f;
    private static final float YAW_RATE_DAMPING = 0.995f;

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
    }

    public VehicleConfig getConfig() {
        return config;
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

        for (int step = 0; step < config.substeps; step++) {
            // ── 1. STEERING with rate limiting and speed-dependent ratio ──
            float targetSteering = steeringInput * config.maxSteeringAngle;
            float steeringDelta = targetSteering - steeringAngle;
            float maxSteerChange = config.steeringSpeed * dt;
            steeringAngle += MathHelper.clamp(steeringDelta, -maxSteerChange, maxSteerChange);

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

            // Lateral transfer
            float deltaFzLat = (config.mass * ayPrev * config.cgHeight) / config.trackWidth;
            // Apply lateral load transfer to axle loads (reduces effective grip under cornering)
            fzFront -= Math.abs(deltaFzLat) * config.rollStiffnessRatioFront;
            fzRear -= Math.abs(deltaFzLat) * (1.0f - config.rollStiffnessRatioFront);

            // Clamp loads to non-negative
            fzFront = Math.max(0f, fzFront);
            fzRear = Math.max(0f, fzRear);

            // ── 3. LOAD SENSITIVITY ──
            float muFront = TireModel.computeEffectiveMu(fzFront, fzNominalFront, currentSurface);
            float muRear = TireModel.computeEffectiveMu(fzRear, fzNominalRear, currentSurface);

            // Precompute slide scaling from base surface properties
            float baseMuPeak = currentSurface.muPeak;
            float baseMuSlide = currentSurface.muSlide;
            float slideScale = baseMuSlide / Math.max(MIN_MU_PEAK, baseMuPeak);

            // ── 4. SLIP ANGLES ──
            float alphaFront = TireModel.computeSlipAngle(vy, vx, yawRate, Lf, effectiveSteering);
            float alphaRear = TireModel.computeSlipAngle(vy, vx, yawRate, -Lr, 0f);

            // ── 5. LATERAL FORCES (with Fiala model) ──
            // Temporarily override mu for front axle
            currentSurface.muPeak = muFront;
            currentSurface.muSlide = muFront * slideScale;
            float fyFrontTarget = TireModel.computeLateralForce(alphaFront, fzFront, currentSurface);

            // Temporarily override mu for rear axle
            currentSurface.muPeak = muRear;
            currentSurface.muSlide = muRear * slideScale;
            float fyRearTarget = TireModel.computeLateralForce(alphaRear, fzRear, currentSurface);

            // Restore base surface properties
            currentSurface.muPeak = baseMuPeak;
            currentSurface.muSlide = baseMuSlide;

            // Apply relaxation length
            fyFrontActual = TireModel.applyRelaxation(fyFrontActual, fyFrontTarget,
                    Math.abs(vx), dt, currentSurface.relaxationLength);
            fyRearActual = TireModel.applyRelaxation(fyRearActual, fyRearTarget,
                    Math.abs(vx), dt, currentSurface.relaxationLength);

            // ── 6. LONGITUDINAL FORCES ──
            float driveForce = throttleInput * config.engineForce;
            float brakeForceFront = brakeInput * config.brakingForce * config.brakeBias;
            float brakeForceRear = brakeInput * config.brakingForce * (1.0f - config.brakeBias);

            // Handbrake locks rear wheels
            if (handbrake) {
                brakeForceRear = config.brakingForce * 0.8f;
            }

            // Engine braking when no throttle
            float engineBrake = 0f;
            if (throttleInput < 0.01f && Math.abs(vx) > 0.1f) {
                engineBrake = config.engineBraking * Math.signum(vx);
            }

            // Drivetrain: distribute drive force between front and rear axles
            float frontDriveRatio = config.drivetrain.getFrontDriveRatio();
            float driveForceFront = driveForce * frontDriveRatio;
            float driveForceRear = driveForce * (1.0f - frontDriveRatio);

            // Temporarily set per-axle mu for longitudinal force computation
            currentSurface.muPeak = muFront;
            float fxFront = TireModel.computeLongitudinalForce(driveForceFront, brakeForceFront, fzFront, currentSurface, vx);
            currentSurface.muPeak = muRear;
            float fxRear = TireModel.computeLongitudinalForce(driveForceRear, brakeForceRear, fzRear, currentSurface, vx);
            currentSurface.muPeak = baseMuPeak;

            // ── 7. FRICTION CIRCLE CONSTRAINT ──
            TireModel.FrictionCircleResult frontForces = TireModel.applyFrictionCircle(fxFront, fyFrontActual, fzFront, muFront);
            fxFront = frontForces.fx;
            fyFrontActual = frontForces.fy;
            TireModel.FrictionCircleResult rearForces = TireModel.applyFrictionCircle(fxRear, fyRearActual, fzRear, muRear);
            fxRear = rearForces.fx;
            fyRearActual = rearForces.fy;

            // ── 8. AERODYNAMIC DRAG ──
            float dragForce = -0.5f * config.dragCoefficient * 2.0f * 1.225f * vx * Math.abs(vx);
            // Rolling resistance
            float rollingResForce = -currentSurface.rollingResistance * config.mass * GRAVITY * Math.signum(vx);

            // ── 9. SUM FORCES AND COMPUTE ACCELERATIONS ──
            float totalFx = fxFront + fxRear + dragForce + rollingResForce - engineBrake;
            float totalFy = fyFrontActual + fyRearActual;

            // Linear accelerations in vehicle frame
            float ax = totalFx / config.mass + yawRate * vy;
            float ay = totalFy / config.mass - yawRate * vx;

            // Yaw moment: front lateral force * Lf - rear lateral force * Lr
            float yawMoment = fyFrontActual * Lf - fyRearActual * Lr;
            float inertia = config.mass * config.wheelbase * config.wheelbase / 12.0f; // simplified moment of inertia
            float yawAccel = yawMoment / inertia;

            // ── 10. INTEGRATE ──
            vx += ax * dt;
            vy += ay * dt;
            yawRate += yawAccel * dt;

            // Dampen yaw rate slightly (numerical stability)
            yawRate *= YAW_RATE_DAMPING;

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

        // Visual angles from weight transfer
        // Pitch: based on longitudinal weight transfer (positive ax = nose up, negative ax = nose down)
        float pitchAngle = 0f;
        if (config.mass > 0f) {
            pitchAngle = -(axPrev / GRAVITY) * 0.25f; // intermediate radian value, scaled to degrees in BoatMixin
        }
        // Roll: based on lateral acceleration (cornering lean)
        float rollAngle = 0f;
        if (config.mass > 0f) {
            rollAngle = (ayPrev / GRAVITY) * 0.20f; // intermediate radian value, scaled to degrees in BoatMixin
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

        // Accumulate weighted surface properties
        float totalMu = 0f;
        float totalMuSlide = 0f;
        float totalCs = 0f;
        float totalRelax = 0f;
        float totalRolling = 0f;
        float totalPeak = 0f;
        float totalFalloff = 0f;
        float totalLoadSens = 0f;
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
                totalMu / count,
                totalMuSlide / count,
                totalCs / count,
                totalRelax / count,
                totalRolling / count,
                totalPeak / count,
                totalFalloff / count,
                totalLoadSens / count
        );
    }

    // ─── GETTERS FOR DEBUG/DISPLAY ───

    public float getVx() { return vx; }
    public float getVy() { return vy; }
    public float getYawRate() { return yawRate; }
    public float getSteeringAngle() { return steeringAngle; }
    public float getFzFront() { return fzFront; }
    public float getFzRear() { return fzRear; }
    public SurfaceProperties getCurrentSurface() { return currentSurface; }

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
