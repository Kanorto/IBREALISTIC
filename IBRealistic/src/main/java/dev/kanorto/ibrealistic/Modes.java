package dev.kanorto.ibrealistic;

import dev.kanorto.ibrealistic.physics.VehicleType;

public enum Modes {
    BROKEN_SLIME_RALLY,//0
    BROKEN_SLIME_RALLY_BLUE,//1
    BROKEN_SLIME_BA_NOFD,//2
    BROKEN_SLIME_PARKOUR,//3
    BROKEN_SLIME_BA_BLUE_NOFD,//4
    BROKEN_SLIME_PARKOUR_BLUE,//5
    BROKEN_SLIME_BA,//6
    BROKEN_SLIME_BA_BLUE,//7
    RALLY,//8
    RALLY_BLUE,//9
    BA_NOFD,//10
    PARKOUR,//11
    BA_BLUE_NOFD,//12
    PARKOUR_BLUE,//13
    BA,//14
    BA_BLUE,//15
    JUMP_BLOCKS,//16
    BOOSTER_BLOCKS,//17
    DEFAULT_ICE,//18
    DEFAULT_NINE_EIGHT_FIVE,//19
    NOCOL_BOATS_AND_PLAYERS,//20
    NOCOL_ALL_ENTITIES,//21
    BA_JANKLESS,//22
    BA_BLUE_JANKLESS,//23
    DEFAULT_BLUE_ICE,//24
    REALISTIC,//25
    REALISTIC_WRC,//26
    REALISTIC_GROUP_B,//27
    REALISTIC_CLASSIC,//28
    REALISTIC_LIGHTWEIGHT,//29
    REALISTIC_TRUCK,//30
    REALISTIC_ALLTERRAIN//31
    ;

    public static void setMode(Modes mode) {
        switch (mode){
            case RALLY:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                return;
            case RALLY_BLUE:
                IBRealistic.setAllBlocksSlipperiness(0.989f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                return;
            case BA_NOFD:
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.98f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                return;
            case PARKOUR:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setJumpForce(0.36f);
                IBRealistic.setStepSize(0.5f);
                return;
            case BA_BLUE_NOFD:
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.989f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                return;
            case PARKOUR_BLUE:
                IBRealistic.setAllBlocksSlipperiness(0.989f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setJumpForce(0.36f);
                IBRealistic.setStepSize(0.5f);
                return;
            case BA:
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.98f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                return;
            case BA_BLUE:
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.989f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                return;
            case BROKEN_SLIME_RALLY:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_RALLY_BLUE:
                IBRealistic.setAllBlocksSlipperiness(0.989f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_BA_NOFD:
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.98f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_PARKOUR:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setJumpForce(0.36f);
                IBRealistic.setStepSize(0.5f);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_BA_BLUE_NOFD:
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.989f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_PARKOUR_BLUE:
                IBRealistic.setAllBlocksSlipperiness(0.989f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setJumpForce(0.36f);
                IBRealistic.setStepSize(0.5f);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_BA:
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.98f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                IBRealistic.breakSlimePlease();
                return;
            case BROKEN_SLIME_BA_BLUE:
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.989f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                IBRealistic.breakSlimePlease();
                return;
            case JUMP_BLOCKS:
                IBRealistic.setBlockSetting(IBRealistic.PerBlockSettingType.jumpForce, "minecraft:orange_concrete", 0.36f);// ~1 block
                IBRealistic.setBlockSetting(IBRealistic.PerBlockSettingType.jumpForce, "minecraft:black_concrete", 0.0f);// no jump
                IBRealistic.setBlockSetting(IBRealistic.PerBlockSettingType.jumpForce, "minecraft:green_concrete", 0.5f);// ~2-3 block
                IBRealistic.setBlockSetting(IBRealistic.PerBlockSettingType.jumpForce, "minecraft:yellow_concrete", 0.18f);// ~0.5 blocks
                return;
            case BOOSTER_BLOCKS:
                IBRealistic.setBlockSetting(IBRealistic.PerBlockSettingType.forwardsAccel, "minecraft:magenta_glazed_terracotta", 0.08f);// double accel
                IBRealistic.setBlockSetting(IBRealistic.PerBlockSettingType.yawAccel, "minecraft:light_gray_glazed_terracotta", 0.08f);// double yaw accel
                return;
            case DEFAULT_ICE:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                return;
            case DEFAULT_NINE_EIGHT_FIVE:
                IBRealistic.setAllBlocksSlipperiness(0.985f);
                return;
            case NOCOL_BOATS_AND_PLAYERS:
                IBRealistic.setCollisionMode(CollisionMode.NO_BOATS_OR_PLAYERS);
                return;
            case NOCOL_ALL_ENTITIES:
                IBRealistic.setCollisionMode(CollisionMode.NO_ENTITIES);
                return;
            case BA_JANKLESS:
                IBRealistic.setCanStepWhileFalling(true);
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.98f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                return;
            case BA_BLUE_JANKLESS:
                IBRealistic.setCanStepWhileFalling(true);
                IBRealistic.setAirControl(true);
                IBRealistic.setBlockSlipperiness("minecraft:air", 0.989f);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setWaterElevation(true);
                return;
            case DEFAULT_BLUE_ICE:
                IBRealistic.setAllBlocksSlipperiness(0.989f);
                return;
            case REALISTIC:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setBackwardsAcceleration(0.01f);
                IBRealistic.setVehicleType(VehicleType.WRC_CAR);
                return;
            case REALISTIC_WRC:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setVehicleType(VehicleType.WRC_CAR);
                return;
            case REALISTIC_GROUP_B:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setVehicleType(VehicleType.GROUP_B);
                return;
            case REALISTIC_CLASSIC:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setVehicleType(VehicleType.CLASSIC_RALLY);
                return;
            case REALISTIC_LIGHTWEIGHT:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setVehicleType(VehicleType.LIGHTWEIGHT);
                return;
            case REALISTIC_TRUCK:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setVehicleType(VehicleType.TRUCK);
                return;
            case REALISTIC_ALLTERRAIN:
                IBRealistic.setAllBlocksSlipperiness(0.98f);
                IBRealistic.setFallDamage(false);
                IBRealistic.setAirControl(true);
                IBRealistic.setStepSize(1.25f);
                IBRealistic.setCanStepWhileFalling(true);
                IBRealistic.setVehicleType(VehicleType.WRC_CAR);
                return;
        }
    }
}
