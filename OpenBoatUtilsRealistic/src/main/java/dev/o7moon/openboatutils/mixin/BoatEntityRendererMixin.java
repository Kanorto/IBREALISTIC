package dev.o7moon.openboatutils.mixin;

import dev.o7moon.openboatutils.OpenBoatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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

    //? <=1.21 {
    @Inject(method = "render(Lnet/minecraft/entity/vehicle/BoatEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/vehicle/BoatEntity;interpolateBubbleWobble(F)F",
                    ordinal = 0))
    private void applyRealisticRoll(BoatEntity boat, float yaw, float tickDelta,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!OpenBoatUtils.fourWheelPhysics.isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof BoatEntity) || !vehicle.equals(boat)) return;

        float rollAngle = OpenBoatUtils.visualRollAngle;
        if (Math.abs(rollAngle) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollAngle));
        }
    }
    //?}

    //? >=1.21.3 {
    /*@Inject(method = "render(Lnet/minecraft/client/render/entity/state/BoatEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V",
                    ordinal = 0))
    private void applyRealisticRoll(net.minecraft.client.render.entity.state.BoatEntityRenderState state,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                     int light, CallbackInfo ci) {
        if (!OpenBoatUtils.fourWheelPhysics.isEnabled()) return;

        float rollAngle = OpenBoatUtils.visualRollAngle;
        if (Math.abs(rollAngle) > 0.01f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rollAngle));
        }
    }
    *///?}
}
