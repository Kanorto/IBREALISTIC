package dev.o7moon.openboatutils.physics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SurfaceProperties {

    public final float muPeak;
    public final float muSlide;
    public final float corneringStiffness;
    public final float relaxationLength;
    public final float rollingResistance;
    public final float peakSlipAngleDeg;
    public final float slipAngleFalloff;
    public final float loadSensitivity;

    public SurfaceProperties(float muPeak, float muSlide, float corneringStiffness,
                             float relaxationLength, float rollingResistance,
                             float peakSlipAngleDeg, float slipAngleFalloff,
                             float loadSensitivity) {
        this.muPeak = muPeak;
        this.muSlide = muSlide;
        this.corneringStiffness = corneringStiffness;
        this.relaxationLength = relaxationLength;
        this.rollingResistance = rollingResistance;
        this.peakSlipAngleDeg = peakSlipAngleDeg;
        this.slipAngleFalloff = slipAngleFalloff;
        this.loadSensitivity = loadSensitivity;
    }

    public SurfaceProperties copy() {
        return new SurfaceProperties(muPeak, muSlide, corneringStiffness, relaxationLength,
                rollingResistance, peakSlipAngleDeg, slipAngleFalloff, loadSensitivity);
    }

    // ─── SURFACE PRESETS ───

    public static final SurfaceProperties ASPHALT_DRY = new SurfaceProperties(
            0.85f, 0.70f, 65000f, 0.06f, 0.012f, 8.0f, 0.7f, 0.10f
    );

    public static final SurfaceProperties ASPHALT_WET = new SurfaceProperties(
            0.55f, 0.40f, 50000f, 0.08f, 0.015f, 10.0f, 0.6f, 0.12f
    );

    public static final SurfaceProperties GRAVEL = new SurfaceProperties(
            0.55f, 0.50f, 30000f, 0.15f, 0.030f, 14.0f, 0.3f, 0.15f
    );

    public static final SurfaceProperties DIRT = new SurfaceProperties(
            0.45f, 0.40f, 25000f, 0.18f, 0.035f, 16.0f, 0.25f, 0.18f
    );

    public static final SurfaceProperties MUD = new SurfaceProperties(
            0.30f, 0.25f, 16000f, 0.25f, 0.050f, 18.0f, 0.2f, 0.20f
    );

    public static final SurfaceProperties SNOW = new SurfaceProperties(
            0.30f, 0.22f, 22000f, 0.12f, 0.025f, 12.0f, 0.35f, 0.16f
    );

    public static final SurfaceProperties ICE = new SurfaceProperties(
            0.10f, 0.07f, 10000f, 0.10f, 0.008f, 6.0f, 0.5f, 0.08f
    );

    public static final SurfaceProperties BLUE_ICE = new SurfaceProperties(
            0.06f, 0.04f, 7000f, 0.10f, 0.008f, 6.0f, 0.5f, 0.08f
    );

    public static final SurfaceProperties SAND = new SurfaceProperties(
            0.40f, 0.35f, 18000f, 0.20f, 0.060f, 15.0f, 0.2f, 0.22f
    );

    // ─── DEFAULT SURFACE FOR UNMAPPED BLOCKS ───

    private static volatile SurfaceProperties defaultSurface = ASPHALT_DRY;

    // ─── ADDITIONAL SURFACE PRESETS FOR ALL-TERRAIN ───

    public static final SurfaceProperties WOOD = new SurfaceProperties(
            0.50f, 0.42f, 35000f, 0.10f, 0.020f, 12.0f, 0.5f, 0.12f
    );

    public static final SurfaceProperties CONCRETE = new SurfaceProperties(
            0.80f, 0.65f, 60000f, 0.07f, 0.013f, 9.0f, 0.65f, 0.11f
    );

    public static final SurfaceProperties TERRACOTTA = new SurfaceProperties(
            0.65f, 0.55f, 45000f, 0.09f, 0.018f, 11.0f, 0.55f, 0.13f
    );

    public static final SurfaceProperties METAL = new SurfaceProperties(
            0.70f, 0.55f, 55000f, 0.06f, 0.010f, 8.0f, 0.6f, 0.09f
    );

    public static final SurfaceProperties GLASS = new SurfaceProperties(
            0.35f, 0.25f, 40000f, 0.05f, 0.008f, 7.0f, 0.65f, 0.08f
    );

    public static final SurfaceProperties WOOL = new SurfaceProperties(
            0.70f, 0.60f, 28000f, 0.20f, 0.045f, 14.0f, 0.35f, 0.14f
    );

    public static final SurfaceProperties BRICK = new SurfaceProperties(
            0.75f, 0.60f, 55000f, 0.08f, 0.016f, 9.0f, 0.6f, 0.12f
    );

    public static final SurfaceProperties NETHER = new SurfaceProperties(
            0.50f, 0.40f, 35000f, 0.12f, 0.025f, 13.0f, 0.4f, 0.15f
    );

    public static final SurfaceProperties VEGETATION = new SurfaceProperties(
            0.40f, 0.35f, 22000f, 0.18f, 0.040f, 15.0f, 0.3f, 0.18f
    );

    public static void setDefaultSurface(SurfaceProperties surface) {
        defaultSurface = surface;
    }

    public static SurfaceProperties getDefaultSurface() {
        return defaultSurface;
    }

    public static void setDefaultSurfaceByName(String name) {
        defaultSurface = getSurfaceByName(name);
    }

    public static SurfaceProperties getSurfaceByName(String name) {
        return switch (name.toUpperCase()) {
            case "ASPHALT_DRY" -> ASPHALT_DRY;
            case "ASPHALT_WET" -> ASPHALT_WET;
            case "GRAVEL" -> GRAVEL;
            case "DIRT" -> DIRT;
            case "MUD" -> MUD;
            case "SNOW" -> SNOW;
            case "ICE" -> ICE;
            case "BLUE_ICE" -> BLUE_ICE;
            case "SAND" -> SAND;
            case "WOOD" -> WOOD;
            case "CONCRETE" -> CONCRETE;
            case "TERRACOTTA" -> TERRACOTTA;
            case "METAL" -> METAL;
            case "GLASS" -> GLASS;
            case "WOOL" -> WOOL;
            case "BRICK" -> BRICK;
            case "NETHER" -> NETHER;
            case "VEGETATION" -> VEGETATION;
            default -> ASPHALT_DRY;
        };
    }

    // ─── BLOCK → SURFACE MAPPING ───

    private static volatile Map<String, SurfaceProperties> blockSurfaceMap;

    public static Map<String, SurfaceProperties> getBlockSurfaceMap() {
        Map<String, SurfaceProperties> map = blockSurfaceMap;
        if (map != null) return map;
        synchronized (SurfaceProperties.class) {
            map = blockSurfaceMap;
            if (map != null) return map;
            map = new ConcurrentHashMap<>();

            // ─── ASPHALT-LIKE (smooth stone variants) ───
            for (String block : new String[]{
                    "minecraft:stone", "minecraft:deepslate", "minecraft:blackstone",
                    "minecraft:polished_blackstone", "minecraft:polished_deepslate",
                    "minecraft:smooth_stone", "minecraft:stone_bricks",
                    "minecraft:polished_andesite", "minecraft:polished_diorite",
                    "minecraft:polished_granite", "minecraft:obsidian",
                    "minecraft:quartz_block", "minecraft:smooth_quartz",
                    "minecraft:smooth_stone_slab", "minecraft:stone_brick_slab",
                    "minecraft:polished_blackstone_slab", "minecraft:polished_deepslate_slab",
                    "minecraft:end_stone_bricks", "minecraft:purpur_block",
                    "minecraft:purpur_pillar", "minecraft:smooth_sandstone",
                    "minecraft:smooth_red_sandstone", "minecraft:chiseled_stone_bricks",
                    "minecraft:chiseled_deepslate", "minecraft:chiseled_polished_blackstone",
                    "minecraft:chiseled_quartz_block"
            }) {
                map.put(block, ASPHALT_DRY);
            }

            // ─── WET ASPHALT-LIKE (rough stone variants) ───
            for (String block : new String[]{
                    "minecraft:cobblestone", "minecraft:mossy_cobblestone",
                    "minecraft:mossy_stone_bricks", "minecraft:andesite",
                    "minecraft:diorite", "minecraft:granite", "minecraft:tuff",
                    "minecraft:prismarine", "minecraft:dark_prismarine",
                    "minecraft:cobblestone_slab", "minecraft:mossy_cobblestone_slab",
                    "minecraft:prismarine_slab", "minecraft:dark_prismarine_slab",
                    "minecraft:cobbled_deepslate", "minecraft:cobbled_deepslate_slab",
                    "minecraft:polished_blackstone_bricks", "minecraft:cracked_stone_bricks",
                    "minecraft:cracked_deepslate_bricks", "minecraft:cracked_polished_blackstone_bricks",
                    "minecraft:end_stone", "minecraft:calcite", "minecraft:dripstone_block",
                    "minecraft:tuff_slab", "minecraft:tuff_bricks", "minecraft:polished_tuff",
                    "minecraft:polished_tuff_slab", "minecraft:chiseled_tuff",
                    "minecraft:chiseled_tuff_bricks"
            }) {
                map.put(block, ASPHALT_WET);
            }

            // ─── CONCRETE (fast, high grip) ───
            for (String block : new String[]{
                    "minecraft:white_concrete", "minecraft:orange_concrete",
                    "minecraft:magenta_concrete", "minecraft:light_blue_concrete",
                    "minecraft:yellow_concrete", "minecraft:lime_concrete",
                    "minecraft:pink_concrete", "minecraft:gray_concrete",
                    "minecraft:light_gray_concrete", "minecraft:cyan_concrete",
                    "minecraft:purple_concrete", "minecraft:blue_concrete",
                    "minecraft:brown_concrete", "minecraft:green_concrete",
                    "minecraft:red_concrete", "minecraft:black_concrete"
            }) {
                map.put(block, CONCRETE);
            }

            // ─── TERRACOTTA (medium grip, slightly rough) ───
            for (String block : new String[]{
                    "minecraft:terracotta",
                    "minecraft:white_terracotta", "minecraft:orange_terracotta",
                    "minecraft:magenta_terracotta", "minecraft:light_blue_terracotta",
                    "minecraft:yellow_terracotta", "minecraft:lime_terracotta",
                    "minecraft:pink_terracotta", "minecraft:gray_terracotta",
                    "minecraft:light_gray_terracotta", "minecraft:cyan_terracotta",
                    "minecraft:purple_terracotta", "minecraft:blue_terracotta",
                    "minecraft:brown_terracotta", "minecraft:green_terracotta",
                    "minecraft:red_terracotta", "minecraft:black_terracotta",
                    "minecraft:white_glazed_terracotta", "minecraft:orange_glazed_terracotta",
                    "minecraft:magenta_glazed_terracotta", "minecraft:light_blue_glazed_terracotta",
                    "minecraft:yellow_glazed_terracotta", "minecraft:lime_glazed_terracotta",
                    "minecraft:pink_glazed_terracotta", "minecraft:gray_glazed_terracotta",
                    "minecraft:light_gray_glazed_terracotta", "minecraft:cyan_glazed_terracotta",
                    "minecraft:purple_glazed_terracotta", "minecraft:blue_glazed_terracotta",
                    "minecraft:brown_glazed_terracotta", "minecraft:green_glazed_terracotta",
                    "minecraft:red_glazed_terracotta", "minecraft:black_glazed_terracotta"
            }) {
                map.put(block, TERRACOTTA);
            }

            // ─── WOOD (medium grip, moderate rolling resistance) ───
            for (String block : new String[]{
                    "minecraft:oak_planks", "minecraft:spruce_planks",
                    "minecraft:birch_planks", "minecraft:jungle_planks",
                    "minecraft:acacia_planks", "minecraft:dark_oak_planks",
                    "minecraft:mangrove_planks", "minecraft:cherry_planks",
                    "minecraft:bamboo_planks", "minecraft:bamboo_mosaic",
                    "minecraft:crimson_planks", "minecraft:warped_planks",
                    "minecraft:oak_slab", "minecraft:spruce_slab",
                    "minecraft:birch_slab", "minecraft:jungle_slab",
                    "minecraft:acacia_slab", "minecraft:dark_oak_slab",
                    "minecraft:mangrove_slab", "minecraft:cherry_slab",
                    "minecraft:bamboo_slab", "minecraft:bamboo_mosaic_slab",
                    "minecraft:crimson_slab", "minecraft:warped_slab",
                    "minecraft:oak_log", "minecraft:spruce_log",
                    "minecraft:birch_log", "minecraft:jungle_log",
                    "minecraft:acacia_log", "minecraft:dark_oak_log",
                    "minecraft:mangrove_log", "minecraft:cherry_log",
                    "minecraft:crimson_stem", "minecraft:warped_stem",
                    "minecraft:stripped_oak_log", "minecraft:stripped_spruce_log",
                    "minecraft:stripped_birch_log", "minecraft:stripped_jungle_log",
                    "minecraft:stripped_acacia_log", "minecraft:stripped_dark_oak_log",
                    "minecraft:stripped_mangrove_log", "minecraft:stripped_cherry_log",
                    "minecraft:stripped_crimson_stem", "minecraft:stripped_warped_stem",
                    "minecraft:oak_wood", "minecraft:spruce_wood",
                    "minecraft:birch_wood", "minecraft:jungle_wood",
                    "minecraft:acacia_wood", "minecraft:dark_oak_wood",
                    "minecraft:mangrove_wood", "minecraft:cherry_wood",
                    "minecraft:crimson_hyphae", "minecraft:warped_hyphae",
                    "minecraft:stripped_oak_wood", "minecraft:stripped_spruce_wood",
                    "minecraft:stripped_birch_wood", "minecraft:stripped_jungle_wood",
                    "minecraft:stripped_acacia_wood", "minecraft:stripped_dark_oak_wood",
                    "minecraft:stripped_mangrove_wood", "minecraft:stripped_cherry_wood",
                    "minecraft:stripped_crimson_hyphae", "minecraft:stripped_warped_hyphae",
                    "minecraft:bamboo_block", "minecraft:stripped_bamboo_block"
            }) {
                map.put(block, WOOD);
            }

            // ─── WOOL (high grip but slow, like carpet/soft surface) ───
            for (String block : new String[]{
                    "minecraft:white_wool", "minecraft:orange_wool",
                    "minecraft:magenta_wool", "minecraft:light_blue_wool",
                    "minecraft:yellow_wool", "minecraft:lime_wool",
                    "minecraft:pink_wool", "minecraft:gray_wool",
                    "minecraft:light_gray_wool", "minecraft:cyan_wool",
                    "minecraft:purple_wool", "minecraft:blue_wool",
                    "minecraft:brown_wool", "minecraft:green_wool",
                    "minecraft:red_wool", "minecraft:black_wool",
                    "minecraft:white_carpet", "minecraft:orange_carpet",
                    "minecraft:magenta_carpet", "minecraft:light_blue_carpet",
                    "minecraft:yellow_carpet", "minecraft:lime_carpet",
                    "minecraft:pink_carpet", "minecraft:gray_carpet",
                    "minecraft:light_gray_carpet", "minecraft:cyan_carpet",
                    "minecraft:purple_carpet", "minecraft:blue_carpet",
                    "minecraft:brown_carpet", "minecraft:green_carpet",
                    "minecraft:red_carpet", "minecraft:black_carpet",
                    "minecraft:moss_carpet", "minecraft:moss_block"
            }) {
                map.put(block, WOOL);
            }

            // ─── BRICK (good grip, close to asphalt) ───
            for (String block : new String[]{
                    "minecraft:bricks", "minecraft:brick_slab",
                    "minecraft:nether_bricks", "minecraft:red_nether_bricks",
                    "minecraft:nether_brick_slab", "minecraft:red_nether_brick_slab",
                    "minecraft:deepslate_bricks", "minecraft:deepslate_brick_slab",
                    "minecraft:polished_blackstone_brick_slab",
                    "minecraft:mud_bricks", "minecraft:mud_brick_slab",
                    "minecraft:stone_brick_wall", "minecraft:brick_wall",
                    "minecraft:nether_brick_wall", "minecraft:red_nether_brick_wall",
                    "minecraft:deepslate_brick_wall"
            }) {
                map.put(block, BRICK);
            }

            // ─── METAL (medium-high grip, very low rolling resistance) ───
            for (String block : new String[]{
                    "minecraft:iron_block", "minecraft:gold_block",
                    "minecraft:copper_block", "minecraft:exposed_copper",
                    "minecraft:weathered_copper", "minecraft:oxidized_copper",
                    "minecraft:waxed_copper_block", "minecraft:waxed_exposed_copper",
                    "minecraft:waxed_weathered_copper", "minecraft:waxed_oxidized_copper",
                    "minecraft:cut_copper", "minecraft:exposed_cut_copper",
                    "minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper",
                    "minecraft:waxed_cut_copper", "minecraft:waxed_exposed_cut_copper",
                    "minecraft:waxed_weathered_cut_copper", "minecraft:waxed_oxidized_cut_copper",
                    "minecraft:cut_copper_slab", "minecraft:exposed_cut_copper_slab",
                    "minecraft:weathered_cut_copper_slab", "minecraft:oxidized_cut_copper_slab",
                    "minecraft:waxed_cut_copper_slab", "minecraft:waxed_exposed_cut_copper_slab",
                    "minecraft:waxed_weathered_cut_copper_slab", "minecraft:waxed_oxidized_cut_copper_slab",
                    "minecraft:netherite_block", "minecraft:diamond_block",
                    "minecraft:emerald_block", "minecraft:lapis_block",
                    "minecraft:redstone_block", "minecraft:raw_iron_block",
                    "minecraft:raw_gold_block", "minecraft:raw_copper_block",
                    "minecraft:heavy_core", "minecraft:chain",
                    "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil",
                    "minecraft:lodestone", "minecraft:lightning_rod"
            }) {
                map.put(block, METAL);
            }

            // ─── GLASS (low grip, slippery) ───
            for (String block : new String[]{
                    "minecraft:glass", "minecraft:tinted_glass",
                    "minecraft:white_stained_glass", "minecraft:orange_stained_glass",
                    "minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass",
                    "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass",
                    "minecraft:pink_stained_glass", "minecraft:gray_stained_glass",
                    "minecraft:light_gray_stained_glass", "minecraft:cyan_stained_glass",
                    "minecraft:purple_stained_glass", "minecraft:blue_stained_glass",
                    "minecraft:brown_stained_glass", "minecraft:green_stained_glass",
                    "minecraft:red_stained_glass", "minecraft:black_stained_glass",
                    "minecraft:glass_pane",
                    "minecraft:white_stained_glass_pane", "minecraft:orange_stained_glass_pane",
                    "minecraft:magenta_stained_glass_pane", "minecraft:light_blue_stained_glass_pane",
                    "minecraft:yellow_stained_glass_pane", "minecraft:lime_stained_glass_pane",
                    "minecraft:pink_stained_glass_pane", "minecraft:gray_stained_glass_pane",
                    "minecraft:light_gray_stained_glass_pane", "minecraft:cyan_stained_glass_pane",
                    "minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane",
                    "minecraft:brown_stained_glass_pane", "minecraft:green_stained_glass_pane",
                    "minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane",
                    "minecraft:sea_lantern"
            }) {
                map.put(block, GLASS);
            }

            // ─── NETHER (moderate grip, alien surface) ───
            for (String block : new String[]{
                    "minecraft:netherrack", "minecraft:nether_wart_block",
                    "minecraft:warped_wart_block", "minecraft:basalt",
                    "minecraft:polished_basalt", "minecraft:smooth_basalt",
                    "minecraft:magma_block", "minecraft:crying_obsidian",
                    "minecraft:gilded_blackstone", "minecraft:shroomlight",
                    "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore",
                    "minecraft:ancient_debris", "minecraft:glowstone"
            }) {
                map.put(block, NETHER);
            }

            // ─── VEGETATION (soft, slow, high rolling resistance) ───
            for (String block : new String[]{
                    "minecraft:hay_block", "minecraft:dried_kelp_block",
                    "minecraft:sponge", "minecraft:wet_sponge",
                    "minecraft:honeycomb_block", "minecraft:slime_block",
                    "minecraft:honey_block"
            }) {
                map.put(block, VEGETATION);
            }

            // ─── CONCRETE POWDER → mapped to SAND surface (loose, gravity-affected) ───
            for (String block : new String[]{
                    "minecraft:white_concrete_powder", "minecraft:orange_concrete_powder",
                    "minecraft:magenta_concrete_powder", "minecraft:light_blue_concrete_powder",
                    "minecraft:yellow_concrete_powder", "minecraft:lime_concrete_powder",
                    "minecraft:pink_concrete_powder", "minecraft:gray_concrete_powder",
                    "minecraft:light_gray_concrete_powder", "minecraft:cyan_concrete_powder",
                    "minecraft:purple_concrete_powder", "minecraft:blue_concrete_powder",
                    "minecraft:brown_concrete_powder", "minecraft:green_concrete_powder",
                    "minecraft:red_concrete_powder", "minecraft:black_concrete_powder"
            }) {
                map.put(block, SAND);
            }

            // Gravel
            map.put("minecraft:gravel", GRAVEL);

            // ─── DIRT VARIANTS ───
            for (String block : new String[]{
                    "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
                    "minecraft:dirt_path", "minecraft:farmland", "minecraft:podzol",
                    "minecraft:mycelium", "minecraft:grass_block",
                    "minecraft:clay", "minecraft:packed_mud"
            }) {
                map.put(block, DIRT);
            }

            // Mud
            for (String block : new String[]{
                    "minecraft:mud", "minecraft:soul_sand", "minecraft:soul_soil",
                    "minecraft:muddy_mangrove_roots", "minecraft:mangrove_roots"
            }) {
                map.put(block, MUD);
            }

            // Snow
            for (String block : new String[]{
                    "minecraft:snow_block", "minecraft:powder_snow",
                    "minecraft:snow"
            }) {
                map.put(block, SNOW);
            }

            // Ice
            for (String block : new String[]{
                    "minecraft:ice", "minecraft:packed_ice", "minecraft:frosted_ice"
            }) {
                map.put(block, ICE);
            }

            // Blue ice (even less grip)
            map.put("minecraft:blue_ice", BLUE_ICE);

            // Sand
            for (String block : new String[]{
                    "minecraft:sand", "minecraft:red_sand", "minecraft:suspicious_sand",
                    "minecraft:suspicious_gravel"
            }) {
                map.put(block, SAND);
            }

            // ─── SPECIAL BLOCKS (ores, misc) → use stone-like defaults ───
            for (String block : new String[]{
                    "minecraft:coal_ore", "minecraft:iron_ore", "minecraft:gold_ore",
                    "minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:lapis_ore",
                    "minecraft:redstone_ore", "minecraft:copper_ore",
                    "minecraft:deepslate_coal_ore", "minecraft:deepslate_iron_ore",
                    "minecraft:deepslate_gold_ore", "minecraft:deepslate_diamond_ore",
                    "minecraft:deepslate_emerald_ore", "minecraft:deepslate_lapis_ore",
                    "minecraft:deepslate_redstone_ore", "minecraft:deepslate_copper_ore",
                    "minecraft:coal_block", "minecraft:amethyst_block",
                    "minecraft:budding_amethyst", "minecraft:sandstone",
                    "minecraft:red_sandstone", "minecraft:cut_sandstone",
                    "minecraft:cut_red_sandstone", "minecraft:chiseled_sandstone",
                    "minecraft:chiseled_red_sandstone"
            }) {
                map.put(block, ASPHALT_DRY);
            }

            // ─── SHULKER BOXES → WOOD ───
            for (String block : new String[]{
                    "minecraft:shulker_box",
                    "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
                    "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box",
                    "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
                    "minecraft:pink_shulker_box", "minecraft:gray_shulker_box",
                    "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box",
                    "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
                    "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
                    "minecraft:red_shulker_box", "minecraft:black_shulker_box"
            }) {
                map.put(block, WOOD);
            }

            // ─── BEDS → WOOL (soft surface) ───
            for (String block : new String[]{
                    "minecraft:white_bed", "minecraft:orange_bed",
                    "minecraft:magenta_bed", "minecraft:light_blue_bed",
                    "minecraft:yellow_bed", "minecraft:lime_bed",
                    "minecraft:pink_bed", "minecraft:gray_bed",
                    "minecraft:light_gray_bed", "minecraft:cyan_bed",
                    "minecraft:purple_bed", "minecraft:blue_bed",
                    "minecraft:brown_bed", "minecraft:green_bed",
                    "minecraft:red_bed", "minecraft:black_bed"
            }) {
                map.put(block, WOOL);
            }
            blockSurfaceMap = map;
            return map;
        }
    }

    public static SurfaceProperties getSurfaceForBlock(String blockId) {
        SurfaceProperties surface = getBlockSurfaceMap().get(blockId);
        return surface != null ? surface : defaultSurface;
    }

    public static synchronized void resetBlockSurfaceMap() {
        blockSurfaceMap = null;
        defaultSurface = ASPHALT_DRY;
    }

    public static void setBlockSurface(String blockId, SurfaceProperties surface) {
        getBlockSurfaceMap().put(blockId, surface);
    }

    public static SurfaceProperties interpolate(SurfaceProperties a, SurfaceProperties b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new SurfaceProperties(
                a.muPeak + (b.muPeak - a.muPeak) * t,
                a.muSlide + (b.muSlide - a.muSlide) * t,
                a.corneringStiffness + (b.corneringStiffness - a.corneringStiffness) * t,
                a.relaxationLength + (b.relaxationLength - a.relaxationLength) * t,
                a.rollingResistance + (b.rollingResistance - a.rollingResistance) * t,
                a.peakSlipAngleDeg + (b.peakSlipAngleDeg - a.peakSlipAngleDeg) * t,
                a.slipAngleFalloff + (b.slipAngleFalloff - a.slipAngleFalloff) * t,
                a.loadSensitivity + (b.loadSensitivity - a.loadSensitivity) * t
        );
    }

    // ─── MUTABLE ACCUMULATOR FOR SURFACE AVERAGING ───
    // Reusable per-engine instance to avoid per-tick GC allocation in detectSurface()

    /**
     * Mutable accumulator for averaging multiple surface properties without allocating
     * a new SurfaceProperties object every tick. Create one per physics engine instance.
     */
    public static class SurfaceAccumulator {
        private float totalMu, totalMuSlide, totalCs, totalRelax;
        private float totalRolling, totalPeak, totalFalloff, totalLoadSens;
        private int count;

        // Track whether all accumulated surfaces are the same reference (uniform surface)
        private SurfaceProperties firstSurface;
        private boolean uniform;

        public void reset() {
            totalMu = 0f;
            totalMuSlide = 0f;
            totalCs = 0f;
            totalRelax = 0f;
            totalRolling = 0f;
            totalPeak = 0f;
            totalFalloff = 0f;
            totalLoadSens = 0f;
            count = 0;
            firstSurface = null;
            uniform = true;
        }

        public void accumulate(SurfaceProperties surface) {
            totalMu += surface.muPeak;
            totalMuSlide += surface.muSlide;
            totalCs += surface.corneringStiffness;
            totalRelax += surface.relaxationLength;
            totalRolling += surface.rollingResistance;
            totalPeak += surface.peakSlipAngleDeg;
            totalFalloff += surface.slipAngleFalloff;
            totalLoadSens += surface.loadSensitivity;
            if (count == 0) {
                firstSurface = surface;
            } else if (uniform && firstSurface != surface) {
                uniform = false;
            }
            count++;
        }

        /**
         * Returns the surface result. If all accumulated samples reference the same
         * preset instance (uniform surface), returns that preset directly with zero
         * allocation. Otherwise, creates a new averaged SurfaceProperties.
         */
        public SurfaceProperties getResult() {
            if (count == 0) return ASPHALT_DRY;
            if (uniform) return firstSurface;
            float inv = 1.0f / count;
            return new SurfaceProperties(
                    totalMu * inv, totalMuSlide * inv, totalCs * inv,
                    totalRelax * inv, totalRolling * inv, totalPeak * inv,
                    totalFalloff * inv, totalLoadSens * inv
            );
        }
    }
}
