package dev.kanorto.ibrealistic;

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
        /*PayloadTypeRegistry.playC2S().register(IBRealistic.BytePayload.ID, IBRealistic.BytePayload.CODEC);
        *///?}
    }

    public static void registerHandlers(){
        //? <=1.20.4 {
        ServerPlayNetworking.registerGlobalReceiver(IBRealistic.settingsChannel, (server, player, handler, buf, responseSender) -> {
            handlePacket(buf);
        });
        //?}
        //? >=1.21 {
        /*ServerPlayNetworking.registerGlobalReceiver(IBRealistic.BytePayload.ID, ((payload, context) ->
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
                    IBRealistic.LOG.info("IBRealistic version received by server: "+versionID+(isRealisticMod ? " (Realistic)" : ""));
                    return;
                case 1:
                    String clientVersion = packetBuf.readString();
                    int clientFeatures = packetBuf.readInt();
                    IBRealistic.LOG.info("Realistic client info received: version=" + clientVersion + " features=" + clientFeatures);
                    return;
            }
        } catch (Exception E) {
            IBRealistic.LOG.error("Error when handling serverbound ibrealistic packet: ");
            for (StackTraceElement e : E.getStackTrace()){
                IBRealistic.LOG.error(e.toString());
            }
        }
    }
}
