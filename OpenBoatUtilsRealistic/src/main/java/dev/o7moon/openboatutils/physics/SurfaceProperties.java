package dev.o7moon.openboatutils.physics;

import java.util.HashMap;

public class SurfaceProperties {

    public float muPeak;
    public float muSlide;
    public float corneringStiffness;
    public float relaxationLength;
    public float rollingResistance;
    public float peakSlipAngleDeg;
    public float slipAngleFalloff;
    public float loadSensitivity;

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

    public static SurfaceProperties ASPHALT_DRY = new SurfaceProperties(
            0.85f, 0.70f, 65000f, 0.06f, 0.012f, 8.0f, 0.7f, 0.10f
    );

    public static SurfaceProperties ASPHALT_WET = new SurfaceProperties(
            0.55f, 0.40f, 50000f, 0.08f, 0.015f, 10.0f, 0.6f, 0.12f
    );

    public static SurfaceProperties GRAVEL = new SurfaceProperties(
            0.55f, 0.50f, 30000f, 0.15f, 0.030f, 14.0f, 0.3f, 0.15f
    );

    public static SurfaceProperties DIRT = new SurfaceProperties(
            0.45f, 0.40f, 25000f, 0.18f, 0.035f, 16.0f, 0.25f, 0.18f
    );

    public static SurfaceProperties MUD = new SurfaceProperties(
            0.30f, 0.25f, 16000f, 0.25f, 0.050f, 18.0f, 0.2f, 0.20f
    );

    public static SurfaceProperties SNOW = new SurfaceProperties(
            0.30f, 0.22f, 22000f, 0.12f, 0.025f, 12.0f, 0.35f, 0.16f
    );

    public static SurfaceProperties ICE = new SurfaceProperties(
            0.10f, 0.07f, 10000f, 0.10f, 0.008f, 6.0f, 0.5f, 0.08f
    );

    public static SurfaceProperties SAND = new SurfaceProperties(
            0.40f, 0.35f, 18000f, 0.20f, 0.060f, 15.0f, 0.2f, 0.22f
    );

    // ─── DEFAULT SURFACE FOR UNMAPPED BLOCKS ───

    private static SurfaceProperties defaultSurface = ASPHALT_DRY;

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
            case "SAND" -> SAND;
            default -> ASPHALT_DRY;
        };
    }

    // ─── BLOCK → SURFACE MAPPING ───

    private static HashMap<String, SurfaceProperties> blockSurfaceMap;

    public static HashMap<String, SurfaceProperties> getBlockSurfaceMap() {
        if (blockSurfaceMap == null) {
            blockSurfaceMap = new HashMap<>();

            // Asphalt-like (stone variants)
            for (String block : new String[]{
                    "minecraft:stone", "minecraft:deepslate", "minecraft:blackstone",
                    "minecraft:polished_blackstone", "minecraft:polished_deepslate",
                    "minecraft:smooth_stone", "minecraft:stone_bricks",
                    "minecraft:polished_andesite", "minecraft:polished_diorite",
                    "minecraft:polished_granite", "minecraft:obsidian",
                    "minecraft:quartz_block", "minecraft:smooth_quartz"
            }) {
                blockSurfaceMap.put(block, ASPHALT_DRY);
            }

            // Wet asphalt-like (cobblestone variants)
            for (String block : new String[]{
                    "minecraft:cobblestone", "minecraft:mossy_cobblestone",
                    "minecraft:mossy_stone_bricks", "minecraft:andesite",
                    "minecraft:diorite", "minecraft:granite", "minecraft:tuff",
                    "minecraft:prismarine", "minecraft:dark_prismarine"
            }) {
                blockSurfaceMap.put(block, ASPHALT_WET);
            }

            // Gravel
            blockSurfaceMap.put("minecraft:gravel", GRAVEL);

            // Dirt variants
            for (String block : new String[]{
                    "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
                    "minecraft:dirt_path", "minecraft:farmland", "minecraft:podzol",
                    "minecraft:mycelium", "minecraft:grass_block"
            }) {
                blockSurfaceMap.put(block, DIRT);
            }

            // Mud
            for (String block : new String[]{
                    "minecraft:mud", "minecraft:soul_sand", "minecraft:soul_soil",
                    "minecraft:muddy_mangrove_roots"
            }) {
                blockSurfaceMap.put(block, MUD);
            }

            // Snow
            for (String block : new String[]{
                    "minecraft:snow_block", "minecraft:powder_snow",
                    "minecraft:snow"
            }) {
                blockSurfaceMap.put(block, SNOW);
            }

            // Ice
            for (String block : new String[]{
                    "minecraft:ice", "minecraft:packed_ice", "minecraft:frosted_ice"
            }) {
                blockSurfaceMap.put(block, ICE);
            }

            // Blue ice (even less grip)
            SurfaceProperties blueIce = ICE.copy();
            blueIce.muPeak = 0.06f;
            blueIce.muSlide = 0.04f;
            blueIce.corneringStiffness = 7000f;
            blockSurfaceMap.put("minecraft:blue_ice", blueIce);

            // Sand
            for (String block : new String[]{
                    "minecraft:sand", "minecraft:red_sand", "minecraft:suspicious_sand"
            }) {
                blockSurfaceMap.put(block, SAND);
            }
        }
        return blockSurfaceMap;
    }

    public static SurfaceProperties getSurfaceForBlock(String blockId) {
        SurfaceProperties surface = getBlockSurfaceMap().get(blockId);
        return surface != null ? surface : defaultSurface;
    }

    public static void resetBlockSurfaceMap() {
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
}
