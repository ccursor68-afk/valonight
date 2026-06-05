package com.emergent.gecepazari.schedule;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.discord.DiscordWebhook;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Config'deki gun + saat ayarina gore Gece Pazari'ni otomatik baslatir/durdurur.
 * Ayrica "sonraki acilis" zamanini PlaceholderAPI gibi disari sunmak icin hesaplar.
 */
public final class ScheduleManager {

    private static final String NOTIFY_PERM = "enightmarket.notify";

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final MarketManager marketManager;
    private final DiscordWebhook webhook;
    private final LanguageManager langManager;

    private final File stateFile;
    private YamlConfiguration stateYaml;

    private BukkitTask checkTask;
    private BukkitTask autoCloseTask;

    public ScheduleManager(GecePazariPlugin plugin,
                           ConfigManager config,
                           MarketManager marketManager,
                           DiscordWebhook webhook,
                           LanguageManager langManager) {
        this.plugin = plugin;
        this.config = config;
        this.marketManager = marketManager;
        this.webhook = webhook;
        this.langManager = langManager;
        this.stateFile = new File(plugin.getDataFolder(), "schedule-state.yml");
        loadState();
    }

    private void loadState() {
        if (!stateFile.exists()) {
            try {
                if (!stateFile.getParentFile().exists()) stateFile.getParentFile().mkdirs();
                stateFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "schedule-state.yml olusturulamadi", ex);
            }
        }
        stateYaml = YamlConfiguration.loadConfiguration(stateFile);
    }

    private void saveState() {
        try {
            stateYaml.save(stateFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "schedule-state.yml kaydedilemedi", ex);
        }
    }

    public void start() {
        if (checkTask != null) checkTask.cancel();
        if (!config.isScheduleEnabled()) {
            plugin.getLogger().info("Otomatik plan kapali.");
            return;
        }
        // Her 20 saniyede kontrol (yeterince hassas)
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::check, 100L, 400L);
        plugin.getLogger().info("Otomatik plan etkin: " + config.getScheduleDay()
                + " @ " + config.getScheduleTime());

        // Eger sunucu, planli kapanis saatinden once acildiysa, otomatik kapanis taskini yeniden olustur
        rescheduleAutoCloseIfPending();
    }

    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
            autoCloseTask = null;
        }
    }

    private void check() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek today = now.getDayOfWeek();
            LocalTime configTime = parseTime(config.getScheduleTime());
            if (configTime == null) return;

            DayOfWeek configDay = parseDay(config.getScheduleDay());

            boolean dayMatches = (configDay == null) || (configDay == today);
            boolean inMinute = now.getHour() == configTime.getHour()
                    && now.getMinute() == configTime.getMinute();

            if (!dayMatches || !inMinute) return;
            if (marketManager.isEventActive()) return;

            String todayKey = LocalDate.now().toString();
            String lastTrigger = stateYaml.getString("last-trigger-date", "");
            if (todayKey.equals(lastTrigger)) return;

            triggerAutoOpen();
            stateYaml.set("last-trigger-date", todayKey);
            stateYaml.set("last-trigger-at", System.currentTimeMillis());
            saveState();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Plan kontrolu sirasinda hata", ex);
        }
    }

    private void triggerAutoOpen() {
        if (!marketManager.startEvent()) return;
        webhook.sendEventStartedEmbed();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission(NOTIFY_PERM)) continue;
            p.sendMessage(ColorUtil.component(langManager.get(p, "event-auto-started")));
        }
        scheduleAutoClose();
        plugin.getLogger().info("Plan tetiklendi: Gece Pazari otomatik olarak basladi.");
    }

    private void scheduleAutoClose() {
        long hours = config.getAutoCloseAfterHours();
        if (hours <= 0) return;
        long ticks = hours * 60L * 60L * 20L;
        if (autoCloseTask != null) autoCloseTask.cancel();
        autoCloseTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (marketManager.isEventActive()) {
                marketManager.stopEvent();
                plugin.getLogger().info("Plan kapanisi: Gece Pazari otomatik olarak kapatildi.");
            }
        }, ticks);
    }

    private void rescheduleAutoCloseIfPending() {
        long hours = config.getAutoCloseAfterHours();
        if (hours <= 0) return;
        long lastTriggerAt = stateYaml.getLong("last-trigger-at", 0L);
        if (lastTriggerAt <= 0) return;
        long autoCloseAt = lastTriggerAt + hours * 3600_000L;
        long remainingMs = autoCloseAt - System.currentTimeMillis();
        if (remainingMs <= 0) return;
        long ticks = remainingMs / 50L;
        if (autoCloseTask != null) autoCloseTask.cancel();
        autoCloseTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (marketManager.isEventActive()) marketManager.stopEvent();
        }, ticks);
    }

    // -------------------- "Sonraki acilis" hesabi (placeholder icin) --------------------

    public LocalDateTime getNextOpenTime() {
        if (!config.isScheduleEnabled()) return null;
        LocalTime configTime = parseTime(config.getScheduleTime());
        if (configTime == null) return null;
        DayOfWeek configDay = parseDay(config.getScheduleDay());
        LocalDateTime now = LocalDateTime.now();

        if (configDay == null) {
            // Her gun
            LocalDateTime candidate = LocalDateTime.of(LocalDate.now(), configTime);
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
            return candidate;
        }

        int diff = (configDay.getValue() - now.getDayOfWeek().getValue() + 7) % 7;
        LocalDateTime candidate = LocalDateTime.of(LocalDate.now().plusDays(diff), configTime);
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(7);
        return candidate;
    }

    public String getNextOpenDayString(String langCode) {
        LocalDateTime next = getNextOpenTime();
        if (next == null) return langManager.getRaw(langCode, "schedule-no-schedule");
        DayOfWeek configDay = parseDay(config.getScheduleDay());
        if (configDay == null) return langManager.getRaw(langCode, "schedule-day-any");
        return next.getDayOfWeek().name();
    }

    public String getNextOpenTimeString() {
        LocalDateTime next = getNextOpenTime();
        if (next == null) return "-";
        return String.format("%02d:%02d", next.getHour(), next.getMinute());
    }

    public String getNextOpenDateString() {
        LocalDateTime next = getNextOpenTime();
        if (next == null) return "-";
        return next.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }

    public String getTimeRemainingString() {
        LocalDateTime next = getNextOpenTime();
        if (next == null) return "-";
        long sec = Duration.between(LocalDateTime.now(), next).getSeconds();
        if (sec <= 0) return "00:00:00";
        long days = sec / 86400;
        long hours = (sec % 86400) / 3600;
        long mins = (sec % 3600) / 60;
        long secs = sec % 60;
        if (days > 0) return days + "d " + hours + "h " + mins + "m";
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String[] parts = s.trim().split(":");
            if (parts.length < 2) return null;
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            return LocalTime.of(h, m);
        } catch (Exception ex) {
            return null;
        }
    }

    private DayOfWeek parseDay(String s) {
        if (s == null) return null;
        String norm = s.trim().toUpperCase();
        if (norm.isEmpty() || norm.equals("ANY") || norm.equals("EVERYDAY") || norm.equals("ALL")) {
            return null;
        }
        try {
            return DayOfWeek.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
