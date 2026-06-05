package com.emergent.gecepazari.compat;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper'a ozel API'leri (hideEntity vb.) reflection ile kullanir; Spigot/Bukkit'te guvenli sekilde atlar.
 */
public final class PlatformCompat {

    private static final Method HIDE_ENTITY;
    private static final Method SHOW_ENTITY;
    private static final boolean PAPER_VISIBILITY;
    private static boolean warnedNoVisibility;

    static {
        Method hide = null;
        Method show = null;
        try {
            hide = Player.class.getMethod("hideEntity", Plugin.class, Entity.class);
            show = Player.class.getMethod("showEntity", Plugin.class, Entity.class);
        } catch (NoSuchMethodException ignored) {
        }
        HIDE_ENTITY = hide;
        SHOW_ENTITY = show;
        PAPER_VISIBILITY = hide != null;
    }

    private PlatformCompat() {}

    public static boolean hasPerPlayerVisibility() {
        return PAPER_VISIBILITY;
    }

    public static void hideEntity(Player viewer, Plugin plugin, Entity entity) {
        if (!PAPER_VISIBILITY || entity == null) return;
        try {
            HIDE_ENTITY.invoke(viewer, plugin, entity);
        } catch (ReflectiveOperationException ex) {
            logOnce(plugin.getLogger(), "hideEntity basarisiz: " + ex.getMessage());
        }
    }

    public static void showEntity(Player viewer, Plugin plugin, Entity entity) {
        if (!PAPER_VISIBILITY || entity == null) return;
        try {
            SHOW_ENTITY.invoke(viewer, plugin, entity);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void logOnce(Logger logger, String message) {
        if (warnedNoVisibility) return;
        warnedNoVisibility = true;
        logger.log(Level.INFO, "[ENightMarket] Per-player entity gizleme desteklenmiyor (Spigot/Bukkit). "
                + "Pazar entity'leri diger oyunculara gorunebilir. " + message);
    }
}
