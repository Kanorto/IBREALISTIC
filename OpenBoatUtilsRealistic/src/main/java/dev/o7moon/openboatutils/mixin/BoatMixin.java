package dev.o7moon.openboatutils.mixin;

import dev.o7moon.openboatutils.CollisionMode;
import dev.o7moon.openboatutils.GetStepHeight;
import dev.o7moon.openboatutils.OpenBoatUtils;
import dev.o7moon.openboatutils.client.WheelRenderer;
import dev.o7moon.openboatutils.physics.RealisticPhysicsEngine;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle. /*$ boat >>*/ BoatEntity ;
//? >=1.21.3 {
/*import net.minecraft.entity.vehicle.BoatEntity;
*///?}
import net.minecraft.registry.Registries;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? <=1.21 {
@Mixin(BoatEntity.class)
//?}
//? >=1.21.3 {
/*@Mixin(net.minecraft.entity.vehicle.AbstractBoatEntity.class)
*///?}
public abstract class BoatMixin implements GetStepHeight {
    @Shadow
    abstract boolean checkBoatInWater();

    @Shadow
    @Nullable
    //? <=1.21 {
    abstract BoatEntity.Location getUnderWaterLocation();
    //?}
    //? >=1.21.3 {
    /*abstract net.minecraft.entity.vehicle.AbstractBoatEntity.Location getUnderWaterLocation();
    *///?}

    @Shadow
    //? <=1.21 {
    abstract BoatEntity.Location checkLocation();
    //?}
    //? >=1.21.3 {
    /*abstract net.minecraft.entity.vehicle.AbstractBoatEntity.Location checkLocation();
    *///?}
    @Shadow
    //? <=1.21 {
    BoatEntity.Location location;
    //?}
    //? >=1.21.3 {
    /*net.minecraft.entity.vehicle.AbstractBoatEntity.Location location;
    *///?}
    @Shadow
    float velocityDecay;
    @Shadow
    float nearbySlipperiness;
    @Shadow
    double waterLevel;

    @Shadow
    float yawVelocity;
    @Shadow
    boolean pressingForward;
    @Shadow
    boolean pressingBack;

    //? >=1.21 {
    /*float openboatutils_step_height;
    public float getStepHeight() {
        return openboatutils_step_height;
    }
    *///?}

    @Unique
    public void set_step_height(float f) {
        //? >=1.21 {
        /*openboatutils_step_height = f;
        *///?}
        //? <=1.20.4 {
        ((BoatEntity) (Object) this).setStepHeight(f);
        //?}
    }

    //? <=1.21 {
    void oncePerTick(BoatEntity instance, BoatEntity.Location loc, MinecraftClient minecraft) {
        if ((loc ==  BoatEntity.Location.UNDER_FLOWING_WATER || loc == BoatEntity.Location.UNDER_WATER) && minecraft.options.jumpKey.isPressed() && OpenBoatUtils.swimForce != 0.0f) {
    //?}
    //? >=1.21.3 {
    /*void oncePerTick(net.minecraft.entity.vehicle.AbstractBoatEntity instance, net.minecraft.entity.vehicle.AbstractBoatEntity.Location loc, MinecraftClient minecraft) {
        if ((loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.UNDER_FLOWING_WATER || loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.UNDER_WATER) && minecraft.options.jumpKey.isPressed() && OpenBoatUtils.swimForce != 0.0f) {
    *///?}
            Vec3d velocity = instance.getVelocity();
            instance.setVelocity(velocity.x, velocity.y + OpenBoatUtils.swimForce, velocity.z);
        }

        //? <=1.21 {
        if (loc == BoatEntity.Location.ON_LAND || (OpenBoatUtils.waterJumping && loc == BoatEntity.Location.IN_WATER)) {
        //?}
        //? >=1.21.3 {
        /*if (loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.ON_LAND || (OpenBoatUtils.waterJumping && loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.IN_WATER)) {
        *///?}
            OpenBoatUtils.coyoteTimer = OpenBoatUtils.coyoteTime;
        } else {
            OpenBoatUtils.coyoteTimer--;
        }

        //? <=1.21 {
        float jumpForce = OpenBoatUtils.GetJumpForce((BoatEntity)(Object)this);
        //?}
        //? >=1.21.3 {
        /*float jumpForce = OpenBoatUtils.GetJumpForce((net.minecraft.entity.vehicle.AbstractBoatEntity)(Object)this);
        *///?}

        // When realistic physics is active, spacebar is handbrake only (not jump)
        boolean realisticActive = OpenBoatUtils.fourWheelPhysics.isEnabled();
        if (!realisticActive && OpenBoatUtils.coyoteTimer >= 0 && jumpForce > 0f && minecraft.options.jumpKey.isPressed()) {
            Vec3d velocity = instance.getVelocity();
            instance.setVelocity(velocity.x, jumpForce, velocity.z);
            OpenBoatUtils.coyoteTimer = -1;// cant jump again until grounded
        }

        // ── REALISTIC PHYSICS ENGINE ──
        // Allow realistic physics in air (when airControl is enabled) to prevent
        // the boat from stopping when transitioning between blocks at different heights
        //? <=1.21 {
        boolean realisticOnGround = loc == BoatEntity.Location.ON_LAND;
        boolean realisticInAir = OpenBoatUtils.airControl && loc == BoatEntity.Location.IN_AIR;
        //?}
        //? >=1.21.3 {
        /*boolean realisticOnGround = loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.ON_LAND;
        boolean realisticInAir = OpenBoatUtils.airControl && loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.IN_AIR;
        *///?}
        if (OpenBoatUtils.fourWheelPhysics.isEnabled() && (realisticOnGround || realisticInAir)) {
            // Set airborne state so the physics engine can skip tire forces
            OpenBoatUtils.fourWheelPhysics.setAirborne(realisticInAir && !realisticOnGround);

            float steeringInput = 0f;
            if (minecraft.options.leftKey.isPressed()) steeringInput += 1f;
            if (minecraft.options.rightKey.isPressed()) steeringInput -= 1f;

            float throttleInput = 0f;
            if (this.pressingForward) throttleInput = 1f;

            float brakeInput = 0f;
            if (this.pressingBack) brakeInput = 1f;

            // Spacebar = handbrake (rear axle lock for drifting), only on ground
            boolean handbrake = !realisticInAir && minecraft.options.jumpKey.isPressed();

            RealisticPhysicsEngine.PhysicsResult result = OpenBoatUtils.fourWheelPhysics.update(
                    instance, steeringInput, throttleInput, brakeInput, handbrake);

            if (result != null) {
                instance.setVelocity(result.velocityX, result.velocityY, result.velocityZ);
                instance.setYaw(instance.getYaw() + result.yawDelta);

                // Visual pitch: nose dips when braking, rises when accelerating
                float visualPitch = -result.pitchAngle * 25.0f; // scale to degrees
                float clampedPitch = MathHelper.clamp(visualPitch, -30.0f, 30.0f);
                instance.setPitch(clampedPitch);

                // Store roll and steering angles for renderer mixin
                OpenBoatUtils.visualRollAngle = result.rollAngle * 15.0f; // scale to degrees
                OpenBoatUtils.visualSteeringAngle = result.steeringAngle;

                // Update wheel spin once per tick (frame-rate independent)
                WheelRenderer.tickWheelSpin(OpenBoatUtils.fourWheelPhysics.getVx());
            }
        }
    }

    //? <=1.21 {
    @Redirect(method = {"getPaddleSoundEvent"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;checkLocation()Lnet/minecraft/entity/vehicle/BoatEntity$Location;"))
    BoatEntity.Location paddleHook(BoatEntity instance) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = {"getPaddleSound"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;checkLocation()Lnet/minecraft/entity/vehicle/AbstractBoatEntity$Location;"))
    net.minecraft.entity.vehicle.AbstractBoatEntity.Location paddleHook(net.minecraft.entity.vehicle.AbstractBoatEntity instance) {
    *///?}
        return hookCheckLocation(instance, false);
    }
    //? <=1.21 {
    @Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;checkLocation()Lnet/minecraft/entity/vehicle/BoatEntity$Location;"))
    BoatEntity.Location tickHook(BoatEntity instance) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;checkLocation()Lnet/minecraft/entity/vehicle/AbstractBoatEntity$Location;"))
    net.minecraft.entity.vehicle.AbstractBoatEntity.Location tickHook(net.minecraft.entity.vehicle.AbstractBoatEntity instance) {
    *///?}
        return hookCheckLocation(instance, true);
    }

    //? <=1.21 {
    BoatEntity.Location hookCheckLocation(BoatEntity instance, boolean is_tick) {
    //?}
    //? >=1.21.3 {
    /*net.minecraft.entity.vehicle.AbstractBoatEntity.Location hookCheckLocation(net.minecraft.entity.vehicle.AbstractBoatEntity instance, boolean is_tick) {
    *///?}
        BoatMixin mixedInstance = (BoatMixin) (Object) instance;
        mixedInstance.set_step_height(0f);

        //? <=1.21 {
        BoatEntity.Location loc = this.checkLocation();
        BoatEntity.Location original_loc = loc;
        //?}
        //? >=1.21.3 {
        /*net.minecraft.entity.vehicle.AbstractBoatEntity.Location loc = this.checkLocation();
        net.minecraft.entity.vehicle.AbstractBoatEntity.Location original_loc = loc;
        *///?}
        if (!OpenBoatUtils.enabled) return loc;
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft == null) return loc;
        PlayerEntity player = minecraft.player;
        if (player == null) return loc;
        Entity vehicle = player.getVehicle();
        //? <=1.21 {
        if (!(vehicle instanceof BoatEntity)) return loc;
        BoatEntity boat = (BoatEntity)vehicle;
        //?}
        //? >=1.21.3 {
        /*if (!(vehicle instanceof net.minecraft.entity.vehicle.AbstractBoatEntity)) return loc;
        net.minecraft.entity.vehicle.AbstractBoatEntity boat = (net.minecraft.entity.vehicle.AbstractBoatEntity)vehicle;
        *///?}

        if (!boat.equals(instance)) return loc;
        if (is_tick) oncePerTick(instance, loc, minecraft);

        mixedInstance.set_step_height(OpenBoatUtils.getStepSize());

        //? <=1.21 {
        if (loc == BoatEntity.Location.UNDER_WATER || loc == BoatEntity.Location.UNDER_FLOWING_WATER) {
        //?}
        //? >=1.21.3 {
        /*if (loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.UNDER_WATER || loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.UNDER_FLOWING_WATER) {
        *///?}
            if (OpenBoatUtils.waterElevation) {
                instance.setPosition(instance.getX(), this.waterLevel += 1.0, instance.getZ());
                Vec3d velocity = instance.getVelocity();
                instance.setVelocity(velocity.x, 0f, velocity.z);// parity with old boatutils, but maybe in the future
                // there should be an implementation with different y velocities here.
                //? <=1.21 {
                return BoatEntity.Location.IN_WATER;
                //?}
                //? >=1.21.3 {
                /*return net.minecraft.entity.vehicle.AbstractBoatEntity.Location.IN_WATER;
                *///?}
            }
            return loc;
        }

        if (this.checkBoatInWater()) {
            if (OpenBoatUtils.waterElevation) {
                Vec3d velocity = instance.getVelocity();
                instance.setVelocity(velocity.x, 0.0, velocity.z);
            }
            //? <=1.21 {
            loc = BoatEntity.Location.IN_WATER;
            //?}
            //? >=1.21.3 {
            /*loc = net.minecraft.entity.vehicle.AbstractBoatEntity.Location.IN_WATER;
            *///?}
        }

        //? <=1.21 {
        if (original_loc == BoatEntity.Location.IN_AIR && OpenBoatUtils.airControl) {
        //?}
        //? >=1.21.3 {
        /*if (original_loc == net.minecraft.entity.vehicle.AbstractBoatEntity.Location.IN_AIR && OpenBoatUtils.airControl) {
        *///?}
            this.nearbySlipperiness = OpenBoatUtils.getBlockSlipperiness("minecraft:air");
            //? <=1.21 {
            loc = BoatEntity.Location.ON_LAND;
            //?}
            //? >=1.21.3 {
            /*loc = net.minecraft.entity.vehicle.AbstractBoatEntity.Location.ON_LAND;
            *///?}
        }

        return loc;
    }

    @Redirect(method = "getNearbySlipperiness", at = @At(value="INVOKE",target="Lnet/minecraft/block/Block;getSlipperiness()F"))
    float getFriction(Block block) {
        if (!OpenBoatUtils.enabled) return block.getSlipperiness();
        return OpenBoatUtils.getBlockSlipperiness(Registries.BLOCK.getId(block).toString());
    }

    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    void canCollideHook(Entity other, CallbackInfoReturnable<Boolean> ci) {
        if (!OpenBoatUtils.enabled) return;
        CollisionMode mode = OpenBoatUtils.getCollisionMode();
        if (mode == CollisionMode.VANILLA) return;
        if ((mode == CollisionMode.NO_BOATS_OR_PLAYERS || mode == CollisionMode.NO_BOATS_OR_PLAYERS_PLUS_FILTER) && (other instanceof BoatEntity || other instanceof PlayerEntity)) {
            ci.setReturnValue(false);
            ci.cancel();
            return;
        }
        if (mode == CollisionMode.NO_ENTITIES) {
            ci.setReturnValue(false);
            ci.cancel();
            return;
        }
        if ((mode == CollisionMode.ENTITYTYPE_FILTER || mode == CollisionMode.NO_BOATS_OR_PLAYERS_PLUS_FILTER) && (OpenBoatUtils.entityIsInCollisionFilter(other))) {
            ci.setReturnValue(false);
            ci.cancel();
            return;
        }
    }

    @Inject(method = "fall", at = @At("HEAD"), cancellable = true)
    void fallHook(CallbackInfo ci) {
        if (!OpenBoatUtils.fallDamage) ci.cancel();
    }

    //? <=1.20.4 {
    @ModifyVariable(method = "updateVelocity", at = @At(value = "STORE"), ordinal = 1)
    private double updateVelocityHook(double e){
        if (!OpenBoatUtils.enabled) return e;

        return OpenBoatUtils.gravityForce;
    }
    //?}

    //? >=1.21.3 {
    /*@ModifyVariable(method = "updateTrackedPositionAndAngles", at = @At("HEAD"), ordinal = 0)
    int interpolationStepsHook(int interpolationSteps) {
        if (!OpenBoatUtils.interpolationCompat) return interpolationSteps;
        return 10;
    }
    *///?}

    //? >=1.21 {
    /*@Inject(method = "getGravity", at = @At("HEAD"), cancellable = true)
    public void onGetGravity(CallbackInfoReturnable<Double> cir) {
        if (!OpenBoatUtils.enabled) return;

        cir.setReturnValue(-OpenBoatUtils.gravityForce);
        cir.cancel();
    }
    *///?}

    //? <=1.21 {
    @Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;yawVelocity:F", opcode = Opcodes.PUTFIELD))
    private void redirectYawVelocityIncrement(BoatEntity boat, float yawVelocity) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;yawVelocity:F", opcode = Opcodes.PUTFIELD))
    private void redirectYawVelocityIncrement(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float yawVelocity) {
    *///?}
        if (!OpenBoatUtils.enabled) {
            this.yawVelocity = yawVelocity;
            return;
        }
        // When realistic physics is active, suppress vanilla yaw changes entirely
        if (OpenBoatUtils.fourWheelPhysics.isEnabled()) {
            this.yawVelocity = 0f;
            return;
        }
        float original_delta = yawVelocity - this.yawVelocity;
        // sign isn't needed here because the vanilla acceleration is exactly 1,
        // but I suppose this helps if mojang ever decides to change that value for some reason
        //? <=1.21 {
        this.yawVelocity += MathHelper.sign(original_delta) * OpenBoatUtils.GetYawAccel((BoatEntity)(Object)this);
        //?}
        //? >=1.21.3 {
        /*this.yawVelocity += MathHelper.sign(original_delta) * OpenBoatUtils.GetYawAccel((net.minecraft.entity.vehicle.AbstractBoatEntity)(Object)this);
        *///?}
    }

    // a whole lotta modifyconstants because mojang put the acceleration values in literals
    @ModifyConstant(method = "updatePaddles", constant = @Constant(floatValue = 0.04f, ordinal = 0))
    private float forwardsAccel(float original) {
        if (!OpenBoatUtils.enabled) return original;
        // When realistic physics is active, suppress vanilla acceleration entirely
        if (OpenBoatUtils.fourWheelPhysics.isEnabled()) return 0f;
        //? <=1.21 {
        return OpenBoatUtils.GetForwardAccel((BoatEntity)(Object)this);
        //?}
        //? >=1.21.3 {
        /*return OpenBoatUtils.GetForwardAccel((net.minecraft.entity.vehicle.AbstractBoatEntity)(Object)this);
        *///?}
    }

    @ModifyConstant(method = "updatePaddles", constant = @Constant(floatValue = 0.005f, ordinal = 0))
    private float turnAccel(float original) {
        if (!OpenBoatUtils.enabled) return original;
        if (OpenBoatUtils.fourWheelPhysics.isEnabled()) return 0f;
        //? <=1.21 {
        return OpenBoatUtils.GetTurnForwardAccel((BoatEntity)(Object)this);
        //?}
        //? >=1.21.3 {
        /*return OpenBoatUtils.GetTurnForwardAccel((net.minecraft.entity.vehicle.AbstractBoatEntity)(Object)this);
        *///?}
    }

    @ModifyConstant(method = "updatePaddles", constant = @Constant(floatValue = 0.005f, ordinal = 1))
    private float backwardsAccel(float original) {
        if (!OpenBoatUtils.enabled) return original;
        if (OpenBoatUtils.fourWheelPhysics.isEnabled()) return 0f;
        //? <=1.21 {
        return OpenBoatUtils.GetBackwardAccel((BoatEntity)(Object)this);
        //?}
        //? >=1.21.3 {
        /*return OpenBoatUtils.GetBackwardAccel((net.minecraft.entity.vehicle.AbstractBoatEntity)(Object)this);
        *///?}
    }

    //? <=1.21 {
    @Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;pressingForward:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private boolean pressingForwardHook(BoatEntity instance) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingForward:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private boolean pressingForwardHook(net.minecraft.entity.vehicle.AbstractBoatEntity instance) {
    *///?}
        if (!OpenBoatUtils.enabled || !OpenBoatUtils.allowAccelStacking) return this.pressingForward;
        return false;
    }

    //? <=1.21 {
    @Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;pressingBack:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private boolean pressingBackHook(BoatEntity instance) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = "updatePaddles", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;pressingBack:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private boolean pressingBackHook(net.minecraft.entity.vehicle.AbstractBoatEntity instance) {
    *///?}
        if (!OpenBoatUtils.enabled || !OpenBoatUtils.allowAccelStacking) return this.pressingBack;
        return false;
    }

    // ON_LAND velocity decay — when realistic physics is active, skip vanilla decay (physics engine handles drag)
    //? <=1.21 {
    @Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void velocityDecayOnLand(BoatEntity boat, float orig) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void velocityDecayOnLand(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float orig) {
    *///?}
        if (OpenBoatUtils.fourWheelPhysics.isEnabled()) {
            velocityDecay = 1.0f;
        } else {
            velocityDecay = orig;
        }
    }

    // UNDER_FLOWING_WATER velocity decay
    //? <=1.21 {
    @Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void velocityDecayHook1(BoatEntity boat, float orig) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void velocityDecayHook1(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float orig) {
    *///?}
        if (!OpenBoatUtils.enabled || !OpenBoatUtils.underwaterControl) velocityDecay = orig;
        else velocityDecay = OpenBoatUtils.getBlockSlipperiness("minecraft:water");
    }

    // UNDER_WATER velocity decay
    //? <=1.21 {
    @Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 3))
    private void velocityDecayHook2(BoatEntity boat, float orig) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 3))
    private void velocityDecayHook2(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float orig) {
    *///?}
        if (!OpenBoatUtils.enabled || !OpenBoatUtils.underwaterControl) velocityDecay = orig;
        else velocityDecay = OpenBoatUtils.getBlockSlipperiness("minecraft:water");
    }

    // IN_WATER velocity decay
    //? <=1.21 {
    @Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/BoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void velocityDecayHook3(BoatEntity boat, float orig) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method="updateVelocity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;velocityDecay:F", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void velocityDecayHook3(net.minecraft.entity.vehicle.AbstractBoatEntity boat, float orig) {
    *///?}
        if (!OpenBoatUtils.enabled || !OpenBoatUtils.surfaceWaterControl) velocityDecay = orig;
        else velocityDecay = OpenBoatUtils.getBlockSlipperiness("minecraft:water");
    }

    // Increase resolution for wall priority by running move() multiple times in smaller increments
    //? <=1.21 {
    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/BoatEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void moveHook(BoatEntity instance, MovementType movementType, Vec3d vec3d) {
    //?}
    //? >=1.21.3 {
    /*@Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/vehicle/AbstractBoatEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void moveHook(net.minecraft.entity.vehicle.AbstractBoatEntity instance, MovementType movementType, Vec3d vec3d) {
    *///?}
        if (!OpenBoatUtils.enabled || OpenBoatUtils.collisionResolution < 1 || OpenBoatUtils.collisionResolution > 50) {
            instance.move(movementType, vec3d);
            return;
        }
        Vec3d subMoveVel = instance.getVelocity().multiply(1d / OpenBoatUtils.collisionResolution);
        for(int i = 0; i < OpenBoatUtils.collisionResolution; i++) {
            instance.move(movementType, subMoveVel);
        }
    }
}
