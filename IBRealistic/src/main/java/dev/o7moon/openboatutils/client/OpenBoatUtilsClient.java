package dev.o7moon.openboatutils.client;

import dev.o7moon.openboatutils.ClientboundPackets;
import dev.o7moon.openboatutils.OpenBoatUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class OpenBoatUtilsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientboundPackets.registerHandlers();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            OpenBoatUtils.resetAll();
            RaceCountdownRenderer.reset();
            OpenBoatUtils.sendVersionPacket();
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
