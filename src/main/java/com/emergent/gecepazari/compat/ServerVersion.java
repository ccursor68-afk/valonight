package com.emergent.gecepazari.compat;

import org.bukkit.Bukkit;

/**
 * Sunucu Minecraft surumunu parse eder ve ozellik bayraklarini sunar.
 */
public final class ServerVersion {

    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;
    private static final String RAW;

    static {
        RAW = Bukkit.getBukkitVersion();
        int major = 0;
        int minor = 0;
        int patch = 0;
        try {
            String core = RAW.split("-")[0];
            String[] parts = core.split("\\.");
            major = Integer.parseInt(parts[0]);
            minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        } catch (Exception ignored) {
            // Bilinmeyen format: en azindan 1.18 varsay
            major = 1;
            minor = 18;
        }
        MAJOR = major;
        MINOR = minor;
        PATCH = patch;
    }

    private ServerVersion() {}

    public static String getRaw() { return RAW; }

    public static int getMajor() { return MAJOR; }

    public static int getMinor() { return MINOR; }

    public static int getPatch() { return PATCH; }

    public static boolean isSupported() {
        return MAJOR > 1 || (MAJOR == 1 && MINOR >= 18);
    }

    /** ItemDisplay / TextDisplay / Interaction (1.19.4+). */
    public static boolean supportsDisplayEntities() {
        if (MAJOR > 1) return true;
        if (MINOR >= 20) return true;
        return MINOR == 19 && PATCH >= 4;
    }

    public static String getDisplayString() {
        return MAJOR + "." + MINOR + (PATCH > 0 ? "." + PATCH : "");
    }
}
