package me.makkuusen.timing.system.boatutils;

/**
 * Feature flags for realistic mod capabilities.
 * Used in version negotiation between server and client.
 * Each feature has a unique bit position in a bitfield.
 * This is a mirror of the client-side enum for the plugin.
 */
public enum RealisticFeature {
    FOUR_WHEEL(0),
    WEATHER(1),
    ECONOMY(2),
    SOLO_RACE(3),
    CUSTOM_CARS(4),
    MULTIPLAYER_RACE(5);

    private final int bit;

    RealisticFeature(int bit) {
        this.bit = bit;
    }

    public int getMask() {
        return 1 << bit;
    }

    public static int encode(RealisticFeature... features) {
        int bitfield = 0;
        for (RealisticFeature f : features) {
            bitfield |= f.getMask();
        }
        return bitfield;
    }

    public static boolean hasFeature(int bitfield, RealisticFeature feature) {
        return (bitfield & feature.getMask()) != 0;
    }
}
