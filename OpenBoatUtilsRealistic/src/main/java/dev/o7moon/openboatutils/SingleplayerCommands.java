package dev.o7moon.openboatutils;

import com.mojang.brigadier.arguments.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SingleplayerCommands {
    public static void registerCommands(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(
                    literal("stepsize").then(
                            argument("size", FloatArgumentType.floatArg()).executes(ctx ->
                            {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_STEP_HEIGHT.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "size"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("reset").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.RESET.ordinal());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    })
            );

            dispatcher.register(
                    literal("defaultslipperiness").then(argument("slipperiness", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_DEFAULT_SLIPPERINESS.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "slipperiness"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("blockslipperiness").then(argument("slipperiness", FloatArgumentType.floatArg()).then(argument("blocks", StringArgumentType.greedyString()).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_BLOCKS_SLIPPERINESS.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx,"slipperiness"));
                        packet.writeString(StringArgumentType.getString(ctx,"blocks").trim());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    })))
            );

            dispatcher.register(
                    literal("aircontrol").then(argument("enabled", BoolArgumentType.bool()).executes(ctx-> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_AIR_CONTROL.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("waterelevation").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_BOAT_WATER_ELEVATION.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("falldamage").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_BOAT_FALL_DAMAGE.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("jumpforce").then(argument("force", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_BOAT_JUMP_FORCE.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "force"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("boatmode").then(argument("mode", StringArgumentType.string()).executes(ctx-> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        Modes mode;
                        try {
                            mode = Modes.valueOf(StringArgumentType.getString(ctx, "mode"));
                        } catch (Exception e) {
                            String valid_modes = "";
                            for (Modes m : Modes.values()) {
                                valid_modes += m.toString() + " ";
                            }
                            ctx.getSource().sendMessage(Text.literal("Invalid mode! Valid modes are: "+valid_modes));
                            return 0;
                        }
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_MODE.ordinal());
                        packet.writeShort(mode.ordinal());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("boatgravity").then(argument("gravity", DoubleArgumentType.doubleArg()).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_GRAVITY.ordinal());
                        packet.writeDouble(DoubleArgumentType.getDouble(ctx, "gravity"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("setyawaccel").then(argument("accel", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_YAW_ACCEL.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "accel"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("setforwardaccel").then(argument("accel", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_FORWARD_ACCEL.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "accel"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("setbackwardaccel").then(argument("accel", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_BACKWARD_ACCEL.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "accel"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("setturnforwardaccel").then(argument("accel", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_TURN_ACCEL.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "accel"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("allowaccelstacking").then(argument("allow", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.ALLOW_ACCEL_STACKING.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "allow"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(literal("sendversionpacket").executes(ctx->{
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) return 0;
                PacketByteBuf packet = PacketByteBufs.create();
                packet.writeShort(ClientboundPackets.RESEND_VERSION.ordinal());
                OpenBoatUtils.sendPacketS2C(player, packet);
                return 1;
            }));

            dispatcher.register(
                    literal("underwatercontrol").then(argument("enabled",BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_UNDERWATER_CONTROL.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("surfacewatercontrol").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_SURFACE_WATER_CONTROL.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("exclusiveboatmode").then(argument("mode",StringArgumentType.string()).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        Modes mode;
                        try {
                            mode = Modes.valueOf(StringArgumentType.getString(ctx, "mode"));
                        } catch (Exception e) {
                            String valid_modes = "";
                            for (Modes m : Modes.values()) {
                                valid_modes += m.toString() + " ";
                            }
                            ctx.getSource().sendMessage(Text.literal("Invalid mode! Valid modes are: "+valid_modes));
                            return 0;
                        }
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_EXCLUSIVE_MODE.ordinal());
                        packet.writeShort(mode.ordinal());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("coyotetime").then(argument("ticks", IntegerArgumentType.integer()).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        int time = IntegerArgumentType.getInteger(ctx,"ticks");
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_COYOTE_TIME.ordinal());
                        packet.writeInt(time);
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("waterjumping").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_WATER_JUMPING.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("swimforce").then(argument("force", FloatArgumentType.floatArg()).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_SWIM_FORCE.ordinal());
                                packet.writeFloat(FloatArgumentType.getFloat(ctx, "force"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("removeblockslipperiness").then(argument("blocks", StringArgumentType.greedyString()).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.REMOVE_BLOCKS_SLIPPERINESS.ordinal());
                        packet.writeString(StringArgumentType.getString(ctx,"blocks").trim());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("clearslipperiness").executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.CLEAR_SLIPPERINESS.ordinal());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    })
            );

            dispatcher.register(
                    literal("modeseries").then(argument("modes", StringArgumentType.greedyString()).executes(ctx-> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        Modes mode;
                        String[] strs = StringArgumentType.getString(ctx, "modes").split(",");
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.MODE_SERIES.ordinal());
                        packet.writeShort(strs.length);
                        for (String modeStr : strs) {
                            try {
                                mode = Modes.valueOf(modeStr.trim());
                            } catch (Exception e) {
                                String valid_modes = "";
                                for (Modes m : Modes.values()) {
                                    valid_modes += m.toString() + " ";
                                }
                                ctx.getSource().sendMessage(Text.literal("Invalid mode! Valid modes are: "+valid_modes));
                                return 0;
                            }
                            packet.writeShort(mode.ordinal());
                        }
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("exclusivemodeseries").then(argument("modes", StringArgumentType.greedyString()).executes(ctx-> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        Modes mode;
                        String[] strs = StringArgumentType.getString(ctx, "modes").split(",");
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.EXCLUSIVE_MODE_SERIES.ordinal());
                        packet.writeShort(strs.length);
                        for (String modeStr : strs) {
                            try {
                                mode = Modes.valueOf(modeStr.trim());
                            } catch (Exception e) {
                                String valid_modes = "";
                                for (Modes m : Modes.values()) {
                                    valid_modes += m.toString() + " ";
                                }
                                ctx.getSource().sendMessage(Text.literal("Invalid mode! Valid modes are: "+valid_modes));
                                return 0;
                            }
                            packet.writeShort(mode.ordinal());
                        }
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("setblocksetting").then(argument("setting", StringArgumentType.string()).then(argument("value", FloatArgumentType.floatArg()).then(argument("blocks", StringArgumentType.greedyString()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        OpenBoatUtils.PerBlockSettingType setting;
                        try {
                            setting = OpenBoatUtils.PerBlockSettingType.valueOf(StringArgumentType.getString(ctx, "setting"));
                        } catch (Exception e) {
                            String valid_settings = "";
                            for (OpenBoatUtils.PerBlockSettingType s : OpenBoatUtils.PerBlockSettingType.values()) {
                                valid_settings += s.toString() + " ";
                            }
                            ctx.getSource().sendMessage(Text.literal("Invalid setting! Valid settings are: "+valid_settings));
                            return 0;
                        }
                        float value = FloatArgumentType.getFloat(ctx, "value");
                        String blocks = StringArgumentType.getString(ctx, "blocks");
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_PER_BLOCK.ordinal());
                        packet.writeShort(setting.ordinal());
                        packet.writeFloat(value);
                        packet.writeString(blocks);
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))))
            );

            dispatcher.register(
                    literal("collisionmode").then(argument("ID", IntegerArgumentType.integer(0,CollisionMode.values().length - 1)).executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                if (player == null) return 0;
                                PacketByteBuf packet = PacketByteBufs.create();
                                packet.writeShort(ClientboundPackets.SET_COLLISION_MODE.ordinal());
                                packet.writeShort(IntegerArgumentType.getInteger(ctx, "ID"));
                                OpenBoatUtils.sendPacketS2C(player, packet);
                                return 1;
                            })
                    )
            );

            dispatcher.register(
                    literal("stepwhilefalling").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_STEP_WHILE_FALLING.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("setinterpolationten").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_INTERPOLATION_COMPAT.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("setcollisionresolution").then(argument("resolution", IntegerArgumentType.integer(1, 50)).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        int time = IntegerArgumentType.getInteger(ctx,"resolution");
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_COLLISION_RESOLUTION.ordinal());
                        packet.writeByte(time);
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("clearcollisionfilter").executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.CLEAR_COLLISION_ENTITYTYPE_FILTER.ordinal());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    })
            );

            dispatcher.register(
                    literal("addcollisionfilter").then(argument("entitytypes", StringArgumentType.greedyString()).executes(ctx->{
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        String entities = StringArgumentType.getString(ctx, "entitytypes").trim();
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.ADD_COLLISION_ENTITYTYPE_FILTER.ordinal());
                        packet.writeString(entities);
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    })
            ));

            // ── REALISTIC PHYSICS COMMANDS ──

            dispatcher.register(
                    literal("realisticphysics").then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_REALISTIC_PHYSICS.ordinal());
                        packet.writeBoolean(BoolArgumentType.getBool(ctx, "enabled"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehicletype").then(argument("type", StringArgumentType.string()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        dev.o7moon.openboatutils.physics.VehicleType type;
                        try {
                            type = dev.o7moon.openboatutils.physics.VehicleType.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase());
                        } catch (Exception e) {
                            StringBuilder valid = new StringBuilder();
                            for (dev.o7moon.openboatutils.physics.VehicleType t : dev.o7moon.openboatutils.physics.VehicleType.values()) {
                                valid.append(t.toString()).append(" ");
                            }
                            ctx.getSource().sendMessage(Text.literal("Invalid vehicle type! Valid types are: " + valid));
                            return 0;
                        }
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_TYPE.ordinal());
                        packet.writeShort(type.ordinal());
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclemass").then(argument("mass", FloatArgumentType.floatArg(100f, 5000f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_MASS.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "mass"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclewheelbase").then(argument("wheelbase", FloatArgumentType.floatArg(1f, 5f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_WHEELBASE.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "wheelbase"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclecgheight").then(argument("height", FloatArgumentType.floatArg(0.1f, 2f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_CG_HEIGHT.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "height"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclemaxsteering").then(argument("angle", FloatArgumentType.floatArg(0.1f, 1.2f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_MAX_STEERING.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "angle"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclesteeringspeed").then(argument("speed", FloatArgumentType.floatArg(0.5f, 10f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_STEERING_SPEED.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "speed"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehicleengineforce").then(argument("force", FloatArgumentType.floatArg(1000f, 20000f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_ENGINE_FORCE.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "force"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclebrakingforce").then(argument("force", FloatArgumentType.floatArg(1000f, 20000f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_BRAKING_FORCE.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "force"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclebrakebias").then(argument("bias", FloatArgumentType.floatArg(0f, 1f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_BRAKE_BIAS.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "bias"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclesubsteps").then(argument("steps", IntegerArgumentType.integer(1, 10)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_SUBSTEPS.ordinal());
                        packet.writeInt(IntegerArgumentType.getInteger(ctx, "steps"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehicleweightbias").then(argument("bias", FloatArgumentType.floatArg(0.3f, 0.7f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_FRONT_WEIGHT_BIAS.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "bias"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("setsurfacetype").then(argument("block", StringArgumentType.string()).then(argument("surface", StringArgumentType.string()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_BLOCK_SURFACE_TYPE.ordinal());
                        packet.writeString(StringArgumentType.getString(ctx, "block"));
                        packet.writeString(StringArgumentType.getString(ctx, "surface"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    })))
            );

            dispatcher.register(
                    literal("vehicledrivetrain").then(argument("type", StringArgumentType.string()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        String typeStr = StringArgumentType.getString(ctx, "type").toUpperCase();
                        short drivetrainId;
                        switch (typeStr) {
                            case "RWD": drivetrainId = 0; break;
                            case "FWD": drivetrainId = 1; break;
                            case "AWD": drivetrainId = 2; break;
                            default:
                                ctx.getSource().sendMessage(Text.literal("Invalid drivetrain! Valid types: RWD FWD AWD"));
                                return 0;
                        }
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_DRIVETRAIN.ordinal());
                        packet.writeShort(drivetrainId);
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("defaultsurface").then(argument("surface", StringArgumentType.string()).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_DEFAULT_SURFACE_TYPE.ordinal());
                        packet.writeString(StringArgumentType.getString(ctx, "surface"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            // ── MISSING VEHICLE PARAMETER COMMANDS ──

            dispatcher.register(
                    literal("vehicletrackwidth").then(argument("width", FloatArgumentType.floatArg(1f, 3f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_TRACK_WIDTH.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "width"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehicledrag").then(argument("drag", FloatArgumentType.floatArg(0f, 2f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_DRAG.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "drag"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclespeedsteeringfactor").then(argument("factor", FloatArgumentType.floatArg(0f, 0.1f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_SPEED_STEERING_FACTOR.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "factor"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehicleenginebraking").then(argument("braking", FloatArgumentType.floatArg(0f, 5000f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_ENGINE_BRAKING.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "braking"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );

            dispatcher.register(
                    literal("vehiclerollstiffness").then(argument("ratio", FloatArgumentType.floatArg(0f, 1f)).executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;
                        PacketByteBuf packet = PacketByteBufs.create();
                        packet.writeShort(ClientboundPackets.SET_VEHICLE_ROLL_STIFFNESS_RATIO.ordinal());
                        packet.writeFloat(FloatArgumentType.getFloat(ctx, "ratio"));
                        OpenBoatUtils.sendPacketS2C(player, packet);
                        return 1;
                    }))
            );
        });
    }
}
