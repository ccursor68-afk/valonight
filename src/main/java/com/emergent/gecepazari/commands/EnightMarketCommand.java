package com.emergent.gecepazari.commands;

import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.discord.DiscordWebhook;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.schedule.ScheduleManager;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class EnightMarketCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERM = "enightmarket.admin";
    private static final String NOTIFY_PERM = "enightmarket.notify";

    private final ConfigManager config;
    private final LanguageManager lang;
    private final MarketManager manager;
    private final MarketGUI gui;
    private final DiscordWebhook webhook;
    private final ScheduleManager scheduler;

    public EnightMarketCommand(ConfigManager config,
                               LanguageManager lang,
                               MarketManager manager,
                               MarketGUI gui,
                               DiscordWebhook webhook,
                               ScheduleManager scheduler) {
        this.config = config;
        this.lang = lang;
        this.manager = manager;
        this.gui = gui;
        this.webhook = webhook;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            MarketCommandUtil.openGui(sender, lang, manager, gui);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start", "baslat", "ac" -> handleStart(sender);
            case "stop", "durdur", "kapat", "close" -> handleStop(sender);
            case "reload" -> handleReload(sender);
            default -> ColorUtil.send(sender, lang.get(sender, "admin-unknown-subcommand"));
        }
        return true;
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            ColorUtil.send(sender, lang.get(sender, "no-permission"));
            return;
        }
        if (!manager.startEvent()) {
            ColorUtil.send(sender, lang.get(sender, "event-already-active"));
            return;
        }
        ColorUtil.send(sender, lang.get(sender, "admin-event-started"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission(NOTIFY_PERM)) continue;
            ColorUtil.send(p, lang.get(p, "event-started"));
        }
        webhook.sendEventStartedEmbed();
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            ColorUtil.send(sender, lang.get(sender, "no-permission"));
            return;
        }
        boolean wasActive = manager.stopEvent();
        if (wasActive) {
            ColorUtil.send(sender, lang.get(sender, "admin-event-stopped"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission(NOTIFY_PERM)) continue;
                ColorUtil.send(p, lang.get(p, "event-stopped"));
            }
        } else {
            ColorUtil.send(sender, lang.get(sender, "event-not-active"));
        }
        scheduler.stop();
        scheduler.start();
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            ColorUtil.send(sender, lang.get(sender, "no-permission"));
            return;
        }
        config.reload();
        lang.reload(config.getDefaultLanguage());
        scheduler.stop();
        scheduler.start();
        ColorUtil.send(sender, lang.get(sender, "reload-success"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission(ADMIN_PERM)) {
            return filter(Arrays.asList("start", "stop", "reload"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String s : options) {
            if (s.toLowerCase().startsWith(lower)) out.add(s);
        }
        return out;
    }
}
