package dev.o7moon.openboatutils.mixin;

import dev.o7moon.openboatutils.OpenBoatUtils;
import dev.o7moon.openboatutils.physics.SurfaceProperties;
import dev.o7moon.openboatutils.physics.FourWheelPhysicsEngine;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void postWorldLoad(CallbackInfo ci){
        // Only reset IBRealistic-specific state.
        // OBU resets its own fields via its own ClientWorldMixin.
        OpenBoatUtils.fourWheelPhysics = new FourWheelPhysicsEngine();
        SurfaceProperties.resetBlockSurfaceMap();
        OpenBoatUtils.visualRollAngle = 0f;
        OpenBoatUtils.visualSteeringAngle = 0f;
        OpenBoatUtils.visualHandbrake = false;
        OpenBoatUtils.countdownActive = false;
        OpenBoatUtils.countdownGoTimeMs = 0;
        OpenBoatUtils.countdownSeconds = 0;
        OpenBoatUtils.realisticDebugHud = false;
        OpenBoatUtils.resetServerInfo();
    }
}
