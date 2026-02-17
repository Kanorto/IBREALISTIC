package dev.kanorto.ibrealistic.ghost;

/**
 * Ghost display modes matching the server-side enum.
 */
public enum GhostDisplayMode {
    OFF(0),
    LINE(1),
    BOAT(2),
    COMPETITION(3);

    private final int id;

    GhostDisplayMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static GhostDisplayMode fromId(int id) {
        for (GhostDisplayMode mode : values()) {
            if (mode.id == id) return mode;
        }
        return OFF;
    }
}
