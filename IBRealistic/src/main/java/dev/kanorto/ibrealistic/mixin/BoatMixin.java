package dev.kanorto.ibrealistic.mixin;

import dev.kanorto.ibrealistic.IBRealistic;
import dev.kanorto.ibrealistic.SurfaceDebugHelper;
import dev.kanorto.ibrealistic.client.WheelRenderer;
import dev.kanorto.ibrealistic.physics.RealisticPhysicsEngine;
import dev.o7moon.openboatutils.OpenBoatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle. /*$ boat >>*/ BoatEntity ;
//? >=1.21.3 {
/*import net.minecraft.entity.vehicle.BoatEntity;
*///?}
//? <=1.20.4 {
import org.joml.Vector3f;
//?}
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.objectweb.asm.Opcodes;

//? <=1.21 {
@Mixin(BoatEntity.class)
//?}
//? >=1.21.3 {
/*@Mixin(net.minecraft.entity.vehicle.AbstractBoatEntity.class)
*///?}
public abstract class BoatMixin {

    @Shadow
    boolean pressingForward;
    @Shadow
    boolean pressingBack;
    @Shadow
    float velocityDecay;

    // ── PASSENGER VISUAL LIFT ──
    @Unique
    private static final float PASSENGER_LIFT = 0.25f;

    // ── GROUND CONTACT MAINTENANCE ──
    /** Small downward velocity applied each tick to maintain Entity.isOnGround() state.
     *  Ensures Entity.move() detects ground collision for reliable isOnGround() on next tick. */
    @Unique
    private static final float GROUND_SNAP_VELOCITY = -0.04f;
    /** Distance below bounding box to check for ground blocks */
    @Unique
    private static final double GROUND_CHECK_DEPTH = 0.25;

    //? <=1.20.4 {
    @Inject(method = "getPassengerAttachmentPos", at = @At("RETURN"), cancellable = true)
    private void liftPassenger(Entity passenger, EntityDimensions dimensions, float scaleFactor,
                               CallbackInfoReturnable<Vector3f> cir) {
        if (!IBRealistic.fourWheelPhysics.isEnabled()) return;
        Vector3f pos = cir.getReturnValue();
        cir.setReturnValue(new Vector3f(pos.x, pos.y + PASSENGER_LIFT, pos.z));
    }
    //?}

    //? >=1.21 {
    /*@Inject(method = "getPassengerAttachmentPos", at = @At("RETURN"), cancellable = true)
    private void liftPassenger(Entity passenger, EntityDimensions dimensions, float scaleFactor,
                               CallbackInfoReturnable<Vec3d> cir) {
        if (!IBRealistic.fourWheelPhysics.isEnabled()) return;
        Vec3d pos = cir.getReturnValue();
        cir.setReturnValue(new Vec3d(pos.x, pos.y + PASSENGER_LIFT, pos.z));
    }
    *///?}

    // ── REALISTIC PHYSICS TICK ──
    //? <=1.21 {
    @Inject(method = "tick", at = @At("HEAD"))
    private void realisticPhysicsTick(CallbackInfo ci) {
        BoatEntity instance = (BoatEntity) (Object) this;
    //?}
    //? >=1.21.3 {
    /*@Inject(method = "tick", at = @At("HEAD"))
    private void realisticPhysicsTick(CallbackInfo ci) {
        net.minecraft.entity.vehicle.AbstractBoatEntity instance = (net.minecraft.entity.vehicle.AbstractBoatEntity) (Object) this;
    *///?}
        if (!IBRealistic.fourWheelPhysics.isEnabled()) return;

        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null || minecraft.player == null) return;
        Entity vehicle = minecraft.player.getVehicle();
        //? <=1.21 {
        if (!(vehicle instanceof BoatEntity) || !vehicle.equals(instance)) return;
        //?}
        //? >=1.21.3 {
        /*if (!(vehicle instanceof net.minecraft.entity.vehicle.AbstractBoatEntity) || !vehicle.equals(instance)) return;
        *///?}

        // ── GROUND DETECTION ──
        // isOnGround() can become false between ticks due to precision issues or
        // Entity.move() not detecting ground collision. Fallback: check for solid
        // blocks directly below the boat.
        boolean onGround = instance.isOnGround() || hasSolidBlockBelow(instance);
        boolean realisticInAir = OpenBoatUtils.airControl && !onGround;

        if (!onGround && !realisticInAir) {
            // Reset visual effects when airborne without air control — prevents
            // stale roll/pitch from last ground tick persisting during flight
            IBRealistic.visualRollAngle = 0f;
            instance.setPitch(0f);
            return;
        }

        // Set airborne state so the physics engine can skip tire forces
        IBRealistic.fourWheelPhysics.setAirborne(!onGround);

        float steeringInput = 0f;
        if (minecraft.options.leftKey.isPressed()) steeringInput += 1f;
        if (minecraft.options.rightKey.isPressed()) steeringInput -= 1f;

        float throttleInput = this.pressingForward ? 1f : 0f;
        float brakeInput = this.pressingBack ? 1f : 0f;

        // Spacebar = handbrake (rear axle lock for drifting), only on ground
        boolean handbrake = !realisticInAir && minecraft.options.jumpKey.isPressed();

        RealisticPhysicsEngine.PhysicsResult result = IBRealistic.fourWheelPhysics.update(
                instance, steeringInput, throttleInput, brakeInput, handbrake);

        if (result != null) {
            // ── GROUND CONTACT GRAVITY ──
            // Apply a small downward velocity when on ground so Entity.move() detects
            // ground collision and sets onGround=true for the next tick.
            float velY = result.velocityY;
            if (onGround && velY > GROUND_SNAP_VELOCITY) {
                velY = GROUND_SNAP_VELOCITY;
            }
            instance.setVelocity(result.velocityX, velY, result.velocityZ);
            instance.setYaw(instance.getYaw() + result.yawDelta);

            // Visual pitch: nose dips when braking, rises when accelerating
            float visualPitch = -result.pitchAngle * 25.0f;
            instance.setPitch(MathHelper.clamp(visualPitch, -30.0f, 30.0f));

            // Store roll, steering, and handbrake state for renderer mixin
            IBRealistic.visualRollAngle = result.rollAngle * 15.0f;
            IBRealistic.visualSteeringAngle = result.steeringAngle;
            IBRealistic.visualHandbrake = handbrake;

            // Update wheel spin once per tick (frame-rate independent)
            WheelRenderer.tickWheelSpin(IBRealistic.fourWheelPhysics.getVx());

            // ── TELEMETRY RECORDING ──
            if (IBRealistic.telemetryRecorder.isRecording()) {
                var engine = IBRealistic.fourWheelPhysics;
                float speedKmh = Math.abs(engine.getVx()) * 3.6f; // m/s to km/h
                float gLat = engine.getAyPrev() / 9.81f;
                float gLong = engine.getAxPrev() / 9.81f;
                IBRealistic.telemetryRecorder.recordTick(
                        (float) instance.getX(), (float) instance.getY(), (float) instance.getZ(),
                        engine.getVx(), engine.getVy(),
                        engine.getYawAngle(), engine.getYawRate(),
                        engine.getSteeringAngle(),
                        throttleInput, brakeInput,
                        handbrake, engine.isAirborne(),
                        engine.getCurrentSurface().getSurfaceId(),
                        result.pitchAngle, result.rollAngle,
                        engine.getSlipAngle(0), engine.getSlipAngle(1),
                        engine.getSlipAngle(2), engine.getSlipAngle(3),
                        speedKmh, gLat, gLong
                );
            }
        }

        // Block spacebar jump when realistic physics is active (handbrake only)
        if (minecraft.options.jumpKey.isPressed()) {
            OpenBoatUtils.coyoteTimer = -1;
        }

        // ── REALISTIC DEBUG HUD ──
        if (IBRealistic.realisticDebugHud) {
            float dbgVx = IBRealistic.fourWheelPhysics.getVx();
            float dbgVy = IBRealistic.fourWheelPhysics.getVy();
            float dbgYawRate = IBRealistic.fourWheelPhysics.getYawRate();
            boolean dbgAir = IBRealistic.fourWheelPhysics.isAirborne();
            String surfaceName = SurfaceDebugHelper.getSurfaceName(
                    IBRealistic.fourWheelPhysics.getCurrentSurface());
            String debugText = String.format("vx=%.2f vy=%.2f yr=%.3f %s surf=%s",
                    dbgVx, dbgVy, dbgYawRate, dbgAir ? "AIR" : "GND", surfaceName);
            if (minecraft.inGameHud != null) {
                minecraft.inGameHud.setOverlayMessage(Text.literal(debugText), false);
            }
        }
    }

    // ── FALLBACK GROUND DETECTION ──
    // Checks if there are solid blocks directly below the boat's bounding box.
    // Used as a fallback when Entity.isOnGround() is unreliable due to missing gravity.
    @Unique
    private static boolean hasSolidBlockBelow(Entity boat) {
        Box box = boat.getBoundingBox();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);
        int checkY = MathHelper.floor(box.minY - GROUND_CHECK_DEPTH);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                pos.set(x, checkY, z);
                BlockState state = boat.getWorld().getBlockState(pos);
                if (!state.getCollisionShape(boat.getWorld(), pos).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ── CANCEL VANILLA/OBU PHYSICS WHEN REALISTIC IS ACTIVE ──
    // When the four-wheel physics engine is enabled, vanilla updatePaddles() acceleration
    // is allowed (OBU hooks set appropriate values), but vanilla yaw from OBU's
    // redirectYawVelocityIncrement also applies — this is intentional (PR #22 architecture).

    // ── VELOCITY DECAY HOOKS (PR #22 STYLE) ──
    // Instead of cancelling updateVelocity() entirely (which removes gravity),
    // we hook specific velocityDecay assignments to set them to 1.0 when realistic
    // physics is active. This preserves vanilla gravity while disabling velocity decay.

    // ON_LAND velocity decay (ordinal=5) — when realistic physics is active, set to 1.0
    //? <=1.21 {
    @Redirect(method = "updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 5))
    private void velocityDecayOnLand(BoatEntity boat, float orig) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = "updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 5))
    private void velocityDecayOnLand(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float orig) {
    *///?}
        if (IBRealistic.fourWheelPhysics.isEnabled()) {
            velocityDecay = 1.0f;
        } else {
            velocityDecay = orig;
        }
    }

    // IN_AIR velocity decay (ordinal=4) — when realistic physics is active, set to 1.0
    //? <=1.21 {
    @Redirect(method = "updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 4))
    private void velocityDecayInAir(BoatEntity boat, float orig) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = "updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 4))
    private void velocityDecayInAir(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float orig) {
    *///?}
        if (IBRealistic.fourWheelPhysics.isEnabled()) {
            velocityDecay = 1.0f;
        } else {
            velocityDecay = orig;
        }
    }

    // ── LANDING SPEED PRESERVATION ──
    @Unique
    private static final float LANDING_SPEED_LOSS_THRESHOLD = 0.2f;
    @Unique
    private static final float LANDING_SPEED_RESTORE_FACTOR = 0.90f;
    @Unique
    private static final double WALL_DETECTION_THRESHOLD = 0.05;

    //? <=1.21 {
    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void moveHook(BoatEntity instance, MovementType movementType, Vec3d vec3d, Operation<Void> original) {
    //?}
    //? >=1.21.3 {
    /*@WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void moveHook(net.minecraft.entity.vehicle.AbstractBoatEntity instance, MovementType movementType, Vec3d vec3d, Operation<Void> original) {
    *///?}
        // Save pre-move state for landing speed preservation
        Vec3d preMoveVel = instance.getVelocity();
        double preMoveHorizSpeedSq = preMoveVel.x * preMoveVel.x + preMoveVel.z * preMoveVel.z;
        boolean preMoveWasFalling = preMoveVel.y < -0.01;

        // Delegate to OBU's collision resolution chain (or vanilla move if OBU not loaded)
        original.call(instance, movementType, vec3d);

        // When realistic physics boat lands from a fall, move() may clip horizontal velocity.
        // Restore most of the horizontal speed to prevent sudden stops on landing.
        if (!IBRealistic.fourWheelPhysics.isEnabled() || !preMoveWasFalling || preMoveHorizSpeedSq <= 0.001) {
            return;
        }

        Vec3d postMoveVel = instance.getVelocity();
        double postMoveHorizSpeedSq = postMoveVel.x * postMoveVel.x + postMoveVel.z * postMoveVel.z;

        boolean landed = postMoveVel.y > preMoveVel.y + 0.01;
        if (!landed || postMoveHorizSpeedSq >= preMoveHorizSpeedSq * (1.0 - LANDING_SPEED_LOSS_THRESHOLD)) {
            return;
        }

        double preHorizSpeed = Math.sqrt(preMoveHorizSpeedSq);
        double dirPreX = preMoveVel.x / preHorizSpeed;
        double dirPreZ = preMoveVel.z / preHorizSpeed;
        double forwardComponent = postMoveVel.x * dirPreX + postMoveVel.z * dirPreZ;

        // If forward component is negative or very small, it's a wall hit — don't restore
        if (forwardComponent <= preHorizSpeed * WALL_DETECTION_THRESHOLD) {
            return;
        }

        // Ground landing — restore speed in the current direction
        double postHorizSpeed = Math.sqrt(postMoveHorizSpeedSq);
        double restoredSpeed = preHorizSpeed * LANDING_SPEED_RESTORE_FACTOR;

        double newVx, newVz;
        if (postHorizSpeed > 0.001) {
            newVx = (postMoveVel.x / postHorizSpeed) * restoredSpeed;
            newVz = (postMoveVel.z / postHorizSpeed) * restoredSpeed;
        } else {
            newVx = dirPreX * restoredSpeed;
            newVz = dirPreZ * restoredSpeed;
        }
        instance.setVelocity(newVx, postMoveVel.y, newVz);
    }
}
