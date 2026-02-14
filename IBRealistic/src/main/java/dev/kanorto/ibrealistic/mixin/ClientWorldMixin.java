package dev.kanorto.ibrealistic.mixin;

import dev.kanorto.ibrealistic.IBRealistic;
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
        IBRealistic.resetRealisticState();
        IBRealistic.resetServerInfo();
    }
}
