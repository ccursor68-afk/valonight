package com.emergent.gecepazari.integration;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.schedule.ScheduleManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI eklentisi tespit edilirse kayit edilir.
 * Saglanan placeholderlar:
 *   %enightmarket_active%            -> true / false
 *   %enightmarket_status%            -> "Aktif" / "Kapali" (oyuncu diline gore)
 *   %enightmarket_next_open_day%     -> "FRIDAY" veya "Her gun" (lokalize)
 *   %enightmarket_next_open_time%    -> "20:00"
 *   %enightmarket_next_open_date%    -> "07/06 20:00"
 *   %enightmarket_time_remaining%    -> "2d 5h 30m" veya "HH:MM:SS"
 */
public final class PlaceholderHook extends PlaceholderExpansion {

    private final GecePazariPlugin plugin;
    private final MarketManager marketManager;
    private final ScheduleManager scheduleManager;
    private final LanguageManager langManager;

    public PlaceholderHook(GecePazariPlugin plugin,
                           MarketManager marketManager,
                           ScheduleManager scheduleManager,
                           LanguageManager langManager) {
        this.plugin = plugin;
        this.marketManager = marketManager;
        this.scheduleManager = scheduleManager;
        this.langManager = langManager;
    }

    @Override
    public @NotNull String getIdentifier() { return "enightmarket"; }

    @Override
    public @NotNull String getAuthor() { return "ArtfulMiner"; }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String langCode = player != null
                ? langManager.getLangFor(player.getUniqueId())
                : langManager.getDefaultLang();

        switch (params.toLowerCase()) {
            case "active":
                return marketManager.isEventActive() ? "true" : "false";
            case "status":
                return marketManager.isEventActive()
                        ? langManager.getRaw(langCode, "schedule-status-active")
                        : langManager.getRaw(langCode, "schedule-status-inactive");
            case "next_open_day":
                return scheduleManager.getNextOpenDayString(langCode);
            case "next_open_time":
                return scheduleManager.getNextOpenTimeString();
            case "next_open_date":
                return scheduleManager.getNextOpenDateString();
            case "time_remaining":
                return scheduleManager.getTimeRemainingString();
            default:
                return null;
        }
    }
}
