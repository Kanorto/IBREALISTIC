package dev.kanorto.ibrealistic;

import dev.kanorto.ibrealistic.physics.SurfaceProperties;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Utility class for surface debug display names.
 * Extracted from BoatMixin to avoid static initializer blocks in mixin classes.
 */
public final class SurfaceDebugHelper {

    private static final Map<SurfaceProperties, String> SURFACE_NAMES = new IdentityHashMap<>();

    static {
        SURFACE_NAMES.put(SurfaceProperties.ASPHALT_DRY, "ASPHALT_DRY");
        SURFACE_NAMES.put(SurfaceProperties.ASPHALT_WET, "ASPHALT_WET");
        SURFACE_NAMES.put(SurfaceProperties.GRAVEL, "GRAVEL");
        SURFACE_NAMES.put(SurfaceProperties.DIRT, "DIRT");
        SURFACE_NAMES.put(SurfaceProperties.MUD, "MUD");
        SURFACE_NAMES.put(SurfaceProperties.SNOW, "SNOW");
        SURFACE_NAMES.put(SurfaceProperties.ICE, "ICE");
        SURFACE_NAMES.put(SurfaceProperties.BLUE_ICE, "BLUE_ICE");
        SURFACE_NAMES.put(SurfaceProperties.SAND, "SAND");
        SURFACE_NAMES.put(SurfaceProperties.WOOD, "WOOD");
        SURFACE_NAMES.put(SurfaceProperties.CONCRETE, "CONCRETE");
        SURFACE_NAMES.put(SurfaceProperties.TERRACOTTA, "TERRACOTTA");
        SURFACE_NAMES.put(SurfaceProperties.METAL, "METAL");
        SURFACE_NAMES.put(SurfaceProperties.GLASS, "GLASS");
        SURFACE_NAMES.put(SurfaceProperties.WOOL, "WOOL");
        SURFACE_NAMES.put(SurfaceProperties.BRICK, "BRICK");
        SURFACE_NAMES.put(SurfaceProperties.NETHER, "NETHER");
        SURFACE_NAMES.put(SurfaceProperties.VEGETATION, "VEGETATION");
    }

    private SurfaceDebugHelper() {}

    public static String getSurfaceName(SurfaceProperties surface) {
        if (surface == null) return "?";
        String name = SURFACE_NAMES.get(surface);
        return name != null ? name : "CUSTOM";
    }
}
