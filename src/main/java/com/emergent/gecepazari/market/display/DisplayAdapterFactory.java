package com.emergent.gecepazari.market.display;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.compat.ServerVersion;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.lang.LanguageManager;
import org.bukkit.entity.Player;

/**
 * Surume gore uygun backend'i yukler.
 * Modern sinif yalnizca 1.19.4+ sunucularda Class.forName ile yuklenir (1.18'de ItemDisplay sinifi yok).
 */
public final class DisplayAdapterFactory {

    private static final String MODERN_CLASS =
            "com.emergent.gecepazari.market.display.ModernDisplayAdapter";

    private DisplayAdapterFactory() {}

    public static MarketDisplayAdapter create(GecePazariPlugin plugin,
                                              ConfigManager config,
                                              LanguageManager lang,
                                              Player owner,
                                              PlayerMarketData data) {
        if (ServerVersion.supportsDisplayEntities()) {
            try {
                Class<?> clazz = Class.forName(MODERN_CLASS);
                return (MarketDisplayAdapter) clazz
                        .getConstructor(GecePazariPlugin.class, ConfigManager.class,
                                LanguageManager.class, Player.class, PlayerMarketData.class)
                        .newInstance(plugin, config, lang, owner, data);
            } catch (Exception ex) {
                plugin.getLogger().warning("Modern display backend yuklenemedi, ArmorStand kullaniliyor: "
                        + ex.getMessage());
            }
        }
        return new LegacyArmorStandAdapter(plugin, config, lang, owner, data);
    }
}
