package dev.kanorto.ibrealistic.client;

import dev.kanorto.ibrealistic.ClientboundPackets;
import dev.kanorto.ibrealistic.IBRealistic;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class IBRealisticClient implements ClientModInitializer {

    /** Tracks whether telemetry was already started for the current countdown */
    private static boolean telemetryStartedForCountdown = false;

    /** Tracks whether ghost was already requested for the current countdown */
    private static boolean ghostRequestedForCountdown = false;

    @Override
    public void onInitializeClient() {
        ClientboundPackets.registerHandlers();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            IBRealistic.resetAll();
            RaceCountdownRenderer.reset();
            HudNotificationRenderer.clear();
            DamageParticleRenderer.reset();
            VehicleParticleRenderer.reset();
            VehicleSoundRenderer.reset();
            GhostRenderer.reset();
            dev.kanorto.ibrealistic.ghost.GhostDataManager.reset();
            telemetryStartedForCountdown = false;
            ghostRequestedForCountdown = false;
            IBRealistic.sendVersionPacket();
        });

        // Stop telemetry on disconnect to prevent data loss
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (IBRealistic.telemetryRecorder.isRecording()) {
                IBRealistic.stopTelemetryRecording(0, false);
            }
            dev.kanorto.ibrealistic.ghost.GhostDataManager.stopPlayback();
            dev.kanorto.ibrealistic.ghost.GhostDataManager.reset();
            telemetryStartedForCountdown = false;
            ghostRequestedForCountdown = false;
        });

        // Register countdown tick handler + damage HUD update
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            RaceCountdownRenderer.tick();

            // ── TELEMETRY AUTO-START/STOP ──
            // Start recording when countdown reaches GO
            if (IBRealistic.countdownActive && IBRealistic.isCountdownGo() && !telemetryStartedForCountdown) {
                telemetryStartedForCountdown = true;
                byte raceType = IBRealistic.serverRealisticVersion != null
                        ? dev.kanorto.ibrealistic.telemetry.TelemetryHeader.RACE_MULTIPLAYER
                        : dev.kanorto.ibrealistic.telemetry.TelemetryHeader.RACE_SOLO;
                IBRealistic.startTelemetryRecording(IBRealistic.currentTrackId, raceType);
                // Start ghost playback when GO
                dev.kanorto.ibrealistic.ghost.GhostDataManager.startPlayback();
            }
            // ── GHOST AUTO-REQUEST ──
            // Request ghost data during countdown (before GO) so it arrives in time
            if (IBRealistic.countdownActive && !IBRealistic.isCountdownGo() && !ghostRequestedForCountdown
                    && IBRealistic.serverRealisticVersion != null && IBRealistic.currentTrackId > 0) {
                ghostRequestedForCountdown = true;
                // Request ghost with default mode (server will use player's settings)
                IBRealistic.sendGhostRequest(IBRealistic.currentTrackId, 0);
            }
            // Reset flags when countdown is deactivated (for next race)
            if (!IBRealistic.countdownActive && telemetryStartedForCountdown) {
                telemetryStartedForCountdown = false;
                ghostRequestedForCountdown = false;
            }
            // Stop recording if player exits vehicle while recording
            if (IBRealistic.telemetryRecorder.isRecording() && client.player != null
                    && client.player.getVehicle() == null) {
                boolean onServer = IBRealistic.serverRealisticVersion != null;
                IBRealistic.stopTelemetryRecording(0, onServer);
                dev.kanorto.ibrealistic.ghost.GhostDataManager.stopPlayback();
            }
            // ── GHOST TICK ──
            dev.kanorto.ibrealistic.ghost.GhostDataManager.tick();
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

            // Spawn driving surface/drift particles (even without damage system)
            VehicleParticleRenderer.tick();

            // Play driving sounds
            VehicleSoundRenderer.tick();
        });

        // Register HUD overlay renderer
        //? <=1.20.4 {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            HudNotificationRenderer.render(drawContext, tickDelta);
            GhostRenderer.renderOverlay(drawContext);
        });
        //?}
        //? >=1.21 {
        /*HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float tickDelta = tickCounter.getTickDelta(true);
            HudNotificationRenderer.render(drawContext, tickDelta);
            GhostRenderer.renderOverlay(drawContext);
        });
        *///?}

        // Register countdown + ghost world renderer
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
            GhostRenderer.render(
                    context.matrixStack(),
                    context.camera(),
                    delta
            );
        });
    }
}
