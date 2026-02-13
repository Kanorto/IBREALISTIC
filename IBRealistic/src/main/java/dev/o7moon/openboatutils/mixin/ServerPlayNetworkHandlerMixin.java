package dev.o7moon.openboatutils.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Inject(method = "isMovementInvalid", at = @At("HEAD"), cancellable = true)
    private static void isMovementInvalid(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);// !!! this disables the movement anti-cheat, but is necessary for stepping to work in singleplayer
    }

    // Force the flag that triggers moved wrongly to false
    @ModifyVariable(method = "onVehicleMove", at = @At("STORE"), ordinal = 2)
    private boolean onVehicleMove_WronglyFlag(boolean original) {
        return false;
    }

    // Skip the "moved "wrongly" warn. also skips "moved too quickly"
    // Intentionally not calling original.call() to suppress the Logger.warn invocation
    @WrapOperation(method = "onVehicleMove", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private void preventMovedWronglyLog(Logger instance, String s, Object[] objects, Operation<Void> original) {}
}
