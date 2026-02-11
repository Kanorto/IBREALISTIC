package me.makkuusen.timing.system.boatutils;

import java.util.Arrays;

public enum BoatUtilsMode {
    VANILLA(-1, 0, false),
    BROKEN_SLIME_RALLY(0, 0, false),
    BROKEN_SLIME_RALLY_BLUE(1, 0, false),
    BROKEN_SLIME_BA_NOFD(2, 0, false),
    BROKEN_SLIME_PARKOUR(3, 0, false),
    BROKEN_SLIME_BA_BLUE_NOFD(4,2, false),
    BROKEN_SLIME_PARKOUR_BLUE(5, 4, false),
    BROKEN_SLIME_BA(6, 4, false),
    BROKEN_SLIME_BA_BLUE(7, 4, false),
    RALLY(8, 5, false),
    RALLY_BLUE(9, 5, false),
    BA_NOFD(10, 5, false),
    PARKOUR(11, 5, false),
    BA_BLUE_NOFD(12, 5, false),
    PARKOUR_BLUE(13, 5, false),
    BA(14, 5, false),
    BA_BLUE(15, 5, false),
    JUMP_BLOCKS(16, 6, false),
    BOOSTER_BLOCKS(17,6, false),
    DEFAULT_ICE(18,6, false),
    DEFAULT_BLUE_ICE(24,6, false),
    NOCOL_BOATS_AND_PLAYERS(20,10, false),
    NOCOL_ALL_ENTITIES(21,10, false),
    BA_JANKLESS(22,11, false),
    BA_BLUE_JANKLESS(23,11, false),
    REALISTIC(25,18, true),
    REALISTIC_WRC(26,18, true),
    REALISTIC_GROUP_B(27,18, true),
    REALISTIC_CLASSIC(28,18, true),
    REALISTIC_LIGHTWEIGHT(29,18, true),
    REALISTIC_TRUCK(30,18, true),
    REALISTIC_ALLTERRAIN(31,18, true),
    ;

    private final short id;
    private final short version;
    private final boolean requiresRealisticMod;

    BoatUtilsMode(int id, int version, boolean requiresRealisticMod) {
        this.id = (short) id;
        this.version = (short) version;
        this.requiresRealisticMod = requiresRealisticMod;
    }

    public short getId(){
        return id;
    }

    public short getRequiredVersion() {
        return version;
    }

    public boolean requiresRealisticMod() {
        return requiresRealisticMod;
    }

    public static BoatUtilsMode getMode(int id) {
        return Arrays.stream(BoatUtilsMode.values()).filter(boatUtilsMode -> boatUtilsMode.getId() == id).findFirst().orElse(VANILLA);
    }
}
