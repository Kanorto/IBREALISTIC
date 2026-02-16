package me.makkuusen.timing.system;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import me.makkuusen.timing.system.boatutils.BoatUtilsManager;
import me.makkuusen.timing.system.telemetry.TelemetryReceiver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class PluginMessageReceiver implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (channel.equalsIgnoreCase("openboatutils:settings") || channel.equalsIgnoreCase("ibrealistic:settings")){
            // Peek at packet ID to route telemetry/ghost packets
            if (channel.equalsIgnoreCase("ibrealistic:settings") && message.length >= 2) {
                short packetId = (short) ((message[0] << 8) | (message[1] & 0xFF));
                if (packetId >= 80 && packetId <= 88) {
                    ByteArrayDataInput in = ByteStreams.newDataInput(message);
                    in.readShort(); // consume packet ID
                    switch (packetId) {
                        case 80: TelemetryReceiver.handleTelemetryStart(player, in); return;
                        case 81: TelemetryReceiver.handleTelemetryChunk(player, in); return;
                        case 82: TelemetryReceiver.handleTelemetryEnd(player, in); return;
                        case 88: TelemetryReceiver.handleGhostRequest(player, in); return;
                        default: return;
                    }
                }
            }
            BoatUtilsManager.pluginMessageListener(channel, player, message);
            return;
        }

        System.out.println("Unknown channel: " + channel);
    }
}
