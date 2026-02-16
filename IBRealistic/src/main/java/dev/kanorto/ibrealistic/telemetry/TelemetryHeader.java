package dev.kanorto.ibrealistic.telemetry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Metadata header for a telemetry recording.
 * Written once at the start of each .ibrt file.
 */
public class TelemetryHeader {

    // ─── FORMAT ───
    public static final int CURRENT_FORMAT_VERSION = 1;

    // ─── RACE TYPE CONSTANTS ───
    public static final byte RACE_SOLO = 0;
    public static final byte RACE_MULTIPLAYER = 1;

    // ─── CAR TYPE CONSTANTS ───
    public static final byte CAR_SYSTEM = 0;
    public static final byte CAR_CUSTOM = 1;

    // ─── FIELDS ───
    public int formatVersion = CURRENT_FORMAT_VERSION;
    public String playerUUID = "";
    public String playerName = "";
    public int trackId;
    public byte raceType;
    public byte carType;
    public byte vehicleType;
    public long startTimestamp;
    public int totalTicks;
    public long finishTimeMs;
    public long checksum;

    // ─── SERIALIZATION ───

    /**
     * Serialize this header to a binary stream.
     */
    public void writeTo(DataOutputStream dos) throws IOException {
        dos.writeInt(formatVersion);
        dos.writeUTF(playerUUID);
        dos.writeUTF(playerName);
        dos.writeInt(trackId);
        dos.writeByte(raceType);
        dos.writeByte(carType);
        dos.writeByte(vehicleType);
        dos.writeLong(startTimestamp);
        dos.writeInt(totalTicks);
        dos.writeLong(finishTimeMs);
        dos.writeLong(checksum);
    }

    /**
     * Deserialize a header from a binary stream.
     */
    public static TelemetryHeader readFrom(DataInputStream dis) throws IOException {
        TelemetryHeader header = new TelemetryHeader();
        header.formatVersion = dis.readInt();
        header.playerUUID = dis.readUTF();
        header.playerName = dis.readUTF();
        header.trackId = dis.readInt();
        header.raceType = dis.readByte();
        header.carType = dis.readByte();
        header.vehicleType = dis.readByte();
        header.startTimestamp = dis.readLong();
        header.totalTicks = dis.readInt();
        header.finishTimeMs = dis.readLong();
        header.checksum = dis.readLong();
        return header;
    }
}
