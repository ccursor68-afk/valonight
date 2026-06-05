package com.emergent.gecepazari.commands;

import com.emergent.gecepazari.GecePazariPlugin;
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

public final class GecePazariCommand implements CommandExecutor, TabCompleter {

    private static final String USE_PERM = "enightmarket.use";
    private static final String ADMIN_PERM = "enightmarket.admin";
    private static final String LANG_PERM = "enightmarket.lang";
    private static final String NOTIFY_PERM = "enightmarket.notify";
    private static final String BYPASS_PERM = "enightmarket.bypass";

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final LanguageManager lang;
    private final MarketManager manager;
    private final MarketGUI gui;
    private final DiscordWebhook webhook;
    private final ScheduleManager scheduler;

    public GecePazariCommand(GecePazariPlugin plugin,
                             ConfigManager config,
                             LanguageManager lang,
                             MarketManager manager,
                             MarketGUI gui,
                             DiscordWebhook webhook,
                             ScheduleManager scheduler) {
        this.plugin = plugin;
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
            handleOpenGui(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "baslat", "start" -> handleStart(sender);
            case "durdur", "stop" -> handleStop(sender);
            case "reload" -> handleReload(sender);
            case "lang", "language", "dil" -> handleLang(sender, args);
            default -> sender.sendMessage(ColorUtil.component(lang.get(sender, "unknown-subcommand")));
        }
        return true;
    }

    private void handleOpenGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "player-only")));
            return;
        }
        if (!player.hasPermission(USE_PERM)) {
            player.sendMessage(ColorUtil.component(lang.get(player, "no-permission")));
            return;
        }
        if (!manager.isEventActive() && !player.hasPermission(BYPASS_PERM)) {
            player.sendMessage(ColorUtil.component(lang.get(player, "not-active")));
            return;
        }
        gui.open(player);
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "no-permission")));
            return;
        }
        if (!manager.startEvent()) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "event-already-active")));
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission(NOTIFY_PERM)) continue;
            p.sendMessage(ColorUtil.component(lang.get(p, "event-started")));
        }
        webhook.sendEventStartedEmbed();
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "no-permission")));
            return;
        }
        if (!manager.stopEvent()) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "event-not-active")));
            return;
        }
        sender.sendMessage(ColorUtil.component(lang.get(sender, "event-stopped")));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "no-permission")));
            return;
        }
        config.reload();
        lang.reload(config.getDefaultLanguage());
        scheduler.stop();
        scheduler.start();
        sender.sendMessage(ColorUtil.component(lang.get(sender, "reload-success")));
    }

    private void handleLang(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component(lang.get(sender, "player-only")));
            return;
        }
        if (!player.hasPermission(LANG_PERM)) {
            player.sendMessage(ColorUtil.component(lang.get(player, "no-permission")));
            return;
        }
        if (args.length < 2) {
            // Mevcut dili goster + kullanim
            String current = lang.getLangFor(player);
            player.sendMessage(ColorUtil.component(
                    lang.get(player, "lang-current").replace("{lang}", current)));
            player.sendMessage(ColorUtil.component(lang.get(player, "lang-usage")));
            return;
        }
        String code = args[1].toLowerCase();
        if (!lang.isSupported(code)) {
            String langs = String.join(", ", lang.getSupportedCodes());
            player.sendMessage(ColorUtil.component(
                    lang.get(player, "lang-invalid").replace("{langs}", langs)));
            return;
        }
        lang.setLang(player.getUniqueId(), code);
        // Yeni dilde tesekkur mesaji
        player.sendMessage(ColorUtil.component(lang.get(player, "lang-changed")));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            base.add("lang");
            if (sender.hasPermission(ADMIN_PERM)) {
                base.addAll(Arrays.asList("baslat", "durdur", "reload"));
            }
            return filter(base, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("lang")) {
            return filter(Arrays.asList(lang.getSupportedCodes()), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String s : options) if (s.toLowerCase().startsWith(lower)) out.add(s);
        return out;
    }
}
