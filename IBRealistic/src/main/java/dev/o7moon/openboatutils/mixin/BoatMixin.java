package dev.o7moon.openboatutils.mixin;

import dev.o7moon.openboatutils.OpenBoatUtils;
import dev.o7moon.openboatutils.SurfaceDebugHelper;
import dev.o7moon.openboatutils.client.WheelRenderer;
import dev.o7moon.openboatutils.physics.RealisticPhysicsEngine;
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
import net.minecraft.entity.EntityDimensions;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

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

    // ── PASSENGER VISUAL LIFT ──
    @Unique
    private static final float PASSENGER_LIFT = 0.25f;

    //? <=1.20.4 {
    @Inject(method = "getPassengerAttachmentPos", at = @At("RETURN"), cancellable = true)
    private void liftPassenger(Entity passenger, EntityDimensions dimensions, float scaleFactor,
                               CallbackInfoReturnable<Vector3f> cir) {
        if (!OpenBoatUtils.fourWheelPhysics.isEnabled()) return;
        Vector3f pos = cir.getReturnValue();
        cir.setReturnValue(new Vector3f(pos.x, pos.y + PASSENGER_LIFT, pos.z));
    }
    //?}

    //? >=1.21 {
    /*@Inject(method = "getPassengerAttachmentPos", at = @At("RETURN"), cancellable = true)
    private void liftPassenger(Entity passenger, EntityDimensions dimensions, float scaleFactor,
                               CallbackInfoReturnable<Vec3d> cir) {
        if (!OpenBoatUtils.fourWheelPhysics.isEnabled()) return;
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
        if (!OpenBoatUtils.fourWheelPhysics.isEnabled()) return;

        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null || minecraft.player == null) return;
        Entity vehicle = minecraft.player.getVehicle();
        //? <=1.21 {
        if (!(vehicle instanceof BoatEntity) || !vehicle.equals(instance)) return;
        //?}
        //? >=1.21.3 {
        /*if (!(vehicle instanceof net.minecraft.entity.vehicle.AbstractBoatEntity) || !vehicle.equals(instance)) return;
        *///?}

        // Determine ground/air state from entity
        boolean onGround = instance.isOnGround();
        boolean realisticInAir = OpenBoatUtils.airControl && !onGround;

        if (!onGround && !realisticInAir) return;

        // Set airborne state so the physics engine can skip tire forces
        OpenBoatUtils.fourWheelPhysics.setAirborne(!onGround);

        float steeringInput = 0f;
        if (minecraft.options.leftKey.isPressed()) steeringInput += 1f;
        if (minecraft.options.rightKey.isPressed()) steeringInput -= 1f;

        float throttleInput = this.pressingForward ? 1f : 0f;
        float brakeInput = this.pressingBack ? 1f : 0f;

        // Spacebar = handbrake (rear axle lock for drifting), only on ground
        boolean handbrake = !realisticInAir && minecraft.options.jumpKey.isPressed();

        RealisticPhysicsEngine.PhysicsResult result = OpenBoatUtils.fourWheelPhysics.update(
                instance, steeringInput, throttleInput, brakeInput, handbrake);

        if (result != null) {
            instance.setVelocity(result.velocityX, result.velocityY, result.velocityZ);
            instance.setYaw(instance.getYaw() + result.yawDelta);

            // Visual pitch: nose dips when braking, rises when accelerating
            float visualPitch = -result.pitchAngle * 25.0f;
            instance.setPitch(MathHelper.clamp(visualPitch, -30.0f, 30.0f));

            // Store roll, steering, and handbrake state for renderer mixin
            OpenBoatUtils.visualRollAngle = result.rollAngle * 15.0f;
            OpenBoatUtils.visualSteeringAngle = result.steeringAngle;
            OpenBoatUtils.visualHandbrake = handbrake;

            // Update wheel spin once per tick (frame-rate independent)
            WheelRenderer.tickWheelSpin(OpenBoatUtils.fourWheelPhysics.getVx());
        }

        // Block spacebar jump when realistic physics is active (handbrake only)
        if (minecraft.options.jumpKey.isPressed()) {
            OpenBoatUtils.coyoteTimer = -1;
        }

        // ── REALISTIC DEBUG HUD ──
        if (OpenBoatUtils.realisticDebugHud) {
            float dbgVx = OpenBoatUtils.fourWheelPhysics.getVx();
            float dbgVy = OpenBoatUtils.fourWheelPhysics.getVy();
            float dbgYawRate = OpenBoatUtils.fourWheelPhysics.getYawRate();
            boolean dbgAir = OpenBoatUtils.fourWheelPhysics.isAirborne();
            String surfaceName = SurfaceDebugHelper.getSurfaceName(
                    OpenBoatUtils.fourWheelPhysics.getCurrentSurface());
            String debugText = String.format("vx=%.2f vy=%.2f yr=%.3f %s surf=%s",
                    dbgVx, dbgVy, dbgYawRate, dbgAir ? "AIR" : "GND", surfaceName);
            if (minecraft.inGameHud != null) {
                minecraft.inGameHud.setOverlayMessage(Text.literal(debugText), false);
            }
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
        if (!OpenBoatUtils.fourWheelPhysics.isEnabled() || !preMoveWasFalling || preMoveHorizSpeedSq <= 0.001) {
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
