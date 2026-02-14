package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.ClientboundPackets;
import dev.kanorto.ibrealistic.IBRealistic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class IBRealisticClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientboundPackets.registerHandlers();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IBRealistic.resetAll();
            RaceCountdownRenderer.reset();
            IBRealistic.sendVersionPacket();
        });

        // Register countdown tick handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RaceCountdownRenderer.tick();
        });

        // Register countdown world renderer
        WorldRenderEvents.LAST.register(context -> {
            //? <=1.20.4 {
            float delta = context.tickDelta();
            //?} else {
            /*float delta = context.tickCounter().getTickDelta(true);
            *///?}
            RaceCountdownRenderer.render(
                    context.matrixStack(),
                    context.camera(),
                    delta
            );
        });
    }
}
