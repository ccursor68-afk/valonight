package com.emergent.gecepazari.commands;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.discord.DiscordWebhook;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
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

    private static final String ADMIN_PERM = "gecepazari.admin";
    private static final String USE_PERM = "gecepazari.use";

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final MarketManager manager;
    private final MarketGUI gui;
    private final DiscordWebhook webhook;

    public GecePazariCommand(GecePazariPlugin plugin,
                             ConfigManager config,
                             MarketManager manager,
                             MarketGUI gui,
                             DiscordWebhook webhook) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
        this.gui = gui;
        this.webhook = webhook;
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
            default -> sender.sendMessage(ColorUtil.component(
                    "&7Kullanim: &f/gecepazari [baslat|durdur|reload]"));
        }
        return true;
    }

    private void handleOpenGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component(config.getMessage("player-only")));
            return;
        }
        if (!player.hasPermission(USE_PERM)) {
            player.sendMessage(ColorUtil.component(config.getMessage("no-permission")));
            return;
        }
        if (!manager.isEventActive()) {
            player.sendMessage(ColorUtil.component(config.getMessage("not-active")));
            return;
        }
        gui.open(player);
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ColorUtil.component(config.getMessage("no-permission")));
            return;
        }
        if (!manager.startEvent()) {
            sender.sendMessage(ColorUtil.component(config.getMessage("event-already-active")));
            return;
        }
        // Tum oyuncuya duyur
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ColorUtil.component(config.getMessage("event-started")));
        }
        // Discord webhook (asenkron)
        webhook.sendEventStartedEmbed();
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ColorUtil.component(config.getMessage("no-permission")));
            return;
        }
        if (!manager.stopEvent()) {
            sender.sendMessage(ColorUtil.component(config.getMessage("event-not-active")));
            return;
        }
        sender.sendMessage(ColorUtil.component(config.getMessage("event-stopped")));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage(ColorUtil.component(config.getMessage("no-permission")));
            return;
        }
        config.reload();
        sender.sendMessage(ColorUtil.component(config.getMessage("reload-success")));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>();
            if (sender.hasPermission(ADMIN_PERM)) {
                base.addAll(Arrays.asList("baslat", "durdur", "reload"));
            }
            List<String> filtered = new ArrayList<>();
            for (String s : base) {
                if (s.startsWith(args[0].toLowerCase())) filtered.add(s);
            }
            return filtered;
        }
        return Collections.emptyList();
    }
}
