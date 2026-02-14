package dev.kanorto.ibrealistic.mixin;

import dev.kanorto.ibrealistic.IBRealistic;
import dev.kanorto.ibrealistic.client.SteeringWheelRenderer;
import dev.kanorto.ibrealistic.client.WheelRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle. /*$ boat >>*/ BoatEntity ;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? <=1.21 {
@Mixin(net.minecraft.client.render.entity.BoatEntityRenderer.class)
//?}
//? >=1.21.3 {
/*@Mixin(net.minecraft.client.render.entity.AbstractBoatEntityRenderer.class)
*///?}
public class BoatEntityRendererMixin {

    // ─── VISUAL LIFT ───
    /** Vertical offset to raise the boat visually when realistic physics is active.
     *  Positive value = lift up (applied as negative Y in the inverted coordinate space). */
    private static final float VISUAL_LIFT = 0.25f;

    //? <=1.21 {
    @Inject(method = "render(Lnet/minecraft/entity/vehicle/BoatEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/BoatEntity;interpolateBubbleWobble(F)F",
                    ordinal = 0))
    private void applyRealisticRoll(BoatEntity boat, float yaw, float tickDelta,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!isPlayerBoat(boat)) return;

        // Lift the boat visually so it appears to sit on its wheels
        // Note: At this injection point (after scale(-1,-1,1)), Y axis is inverted (negative = up)
        matrices.translate(0f, -VISUAL_LIFT, 0f);

        float rollAngle = IBRealistic.visualRollAngle;
        if (Math.abs(rollAngle) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollAngle));
        }
    }

    @Inject(method = "render(Lnet/minecraft/entity/vehicle/BoatEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
                    ordinal = 0))
    private void renderWheels(BoatEntity boat, float yaw, float tickDelta,
                               MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               int light, CallbackInfo ci) {
        if (!isPlayerBoat(boat)) return;

        WheelRenderer.renderWheels(matrices, vertexConsumers, light,
                IBRealistic.visualSteeringAngle,
                IBRealistic.fourWheelPhysics.getVx(), tickDelta,
                IBRealistic.visualHandbrake);
        SteeringWheelRenderer.renderSteeringWheel(matrices, vertexConsumers, light,
                IBRealistic.visualSteeringAngle);
    }

    private static boolean isPlayerBoat(BoatEntity boat) {
        if (!IBRealistic.fourWheelPhysics.isEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;
        Entity vehicle = mc.player.getVehicle();
        return vehicle instanceof BoatEntity && vehicle.equals(boat);
    }
    //?}

    //? >=1.21.3 {
    /*
    // Note: In 1.21.3, BoatEntityRenderState does not provide access to the boat entity,
    // so we cannot check if it's the player's boat. Effects apply to all boats when enabled.
    // This is a Minecraft API limitation - BoatEntityRenderState only has yaw, damage, and wobble.
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/BoatEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V",
                    ordinal = 0))
    private void applyRealisticRoll(net.minecraft.client.render.entity.state.BoatEntityRenderState state,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!IBRealistic.fourWheelPhysics.isEnabled()) return;

        // Lift the boat visually so it appears to sit on its wheels
        // Note: At this injection point (before scale(-1,-1,1)), Y axis is standard (positive = up)
        matrices.translate(0f, VISUAL_LIFT, 0f);

        float rollAngle = IBRealistic.visualRollAngle;
        if (Math.abs(rollAngle) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollAngle));
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/BoatEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V",
                    ordinal = 0))
    private void renderWheels(net.minecraft.client.render.entity.state.BoatEntityRenderState state,
                               MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                               int light, CallbackInfo ci) {
        if (!IBRealistic.fourWheelPhysics.isEnabled()) return;

        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
        WheelRenderer.renderWheels(matrices, vertexConsumers, light,
                IBRealistic.visualSteeringAngle,
                IBRealistic.fourWheelPhysics.getVx(), tickDelta,
                IBRealistic.visualHandbrake);
        SteeringWheelRenderer.renderSteeringWheel(matrices, vertexConsumers, light,
                IBRealistic.visualSteeringAngle);
    }
    *///?}
}
