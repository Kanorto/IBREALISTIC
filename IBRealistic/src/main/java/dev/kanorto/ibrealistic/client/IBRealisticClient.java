package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.ClientboundPackets;
import dev.kanorto.ibrealistic.IBRealistic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class IBRealisticClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientboundPackets.registerHandlers();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IBRealistic.resetAll();
            RaceCountdownRenderer.reset();
            HudNotificationRenderer.clear();
            DamageParticleRenderer.reset();
            IBRealistic.sendVersionPacket();
        });

        // Register countdown tick handler + damage HUD update
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RaceCountdownRenderer.tick();
            // Update damage state and HUD each tick
            if (IBRealistic.damageState.isDamageEnabled() && IBRealistic.fourWheelPhysics.isEnabled()) {
                // Client-side engine temperature prediction
                float vehicleSpeed = 0f;
                if (client.player != null && client.player.getVehicle() != null) {
                    vehicleSpeed = (float) client.player.getVehicle().getVelocity().horizontalLength();
                }
                IBRealistic.damageState.clientTick(vehicleSpeed);

                // Client-side notification threshold checking
                Object[] notif = IBRealistic.damageState.checkNotifications();
                if (notif != null) {
                    HudNotificationRenderer.addNotification((String) notif[0], (int) notif[1], 3000);
                }

                HudNotificationRenderer.setDamageHud(
                        IBRealistic.damageState.getTireWear(),
                        IBRealistic.damageState.getEngineTemp(),
                        IBRealistic.damageState.getBodyDamage(),
                        IBRealistic.damageState.isInServiceZone(),
                        IBRealistic.damageState.getRepairProgress()
                );
                // Spawn damage particles
                DamageParticleRenderer.tick();
            }
        });

        // Register HUD overlay renderer
        //? <=1.20.4 {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            HudNotificationRenderer.render(drawContext, tickDelta);
        });
        //?}
        //? >=1.21 {
        /*HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float tickDelta = tickCounter.getTickDelta(true);
            HudNotificationRenderer.render(drawContext, tickDelta);
        });
        *///?}

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
