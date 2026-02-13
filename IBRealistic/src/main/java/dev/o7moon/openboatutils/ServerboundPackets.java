package dev.o7moon.openboatutils;

import io.netty.buffer.ByteBuf;
//? >=1.21
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;*/
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;

public enum ServerboundPackets {
    VERSION,
    REALISTIC_CLIENT_INFO;

    public static void registerCodecs() {
        //? >=1.21 {
        /*PayloadTypeRegistry.playC2S().register(OpenBoatUtils.BytePayload.ID, OpenBoatUtils.BytePayload.CODEC);
        *///?}
    }

    public static void registerHandlers(){
        //? <=1.20.4 {
        ServerPlayNetworking.registerGlobalReceiver(OpenBoatUtils.settingsChannel, (server, player, handler, buf, responseSender) -> {
            handlePacket(buf);
        });
        //?}
        //? >=1.21 {
        /*ServerPlayNetworking.registerGlobalReceiver(OpenBoatUtils.BytePayload.ID, ((payload, context) ->
                context.server().execute(() ->
                    handlePacket(payload.data()) )));
        *///?}
    }

    public static void handlePacket(ByteBuf buf) {
        try {
            PacketByteBuf packetBuf = new PacketByteBuf(buf);
            short packetID = packetBuf.readShort();
            switch (packetID) {
                case 0:
                    int versionID = packetBuf.readInt();
                    boolean isRealisticMod = packetBuf.isReadable() && packetBuf.readBoolean();
                    OpenBoatUtils.LOG.info("OpenBoatUtils version received by server: "+versionID+(isRealisticMod ? " (Realistic)" : ""));
                    return;
                case 1:
                    String clientVersion = packetBuf.readString();
                    int clientFeatures = packetBuf.readInt();
                    OpenBoatUtils.LOG.info("Realistic client info received: version=" + clientVersion + " features=" + clientFeatures);
                    return;
            }
        } catch (Exception E) {
            OpenBoatUtils.LOG.error("Error when handling serverbound ibrealistic packet: ");
            for (StackTraceElement e : E.getStackTrace()){
                OpenBoatUtils.LOG.error(e.toString());
            }
        }
    }
}
