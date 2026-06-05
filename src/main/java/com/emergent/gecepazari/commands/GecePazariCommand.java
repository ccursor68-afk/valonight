package com.emergent.gecepazari.commands;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.util.ColorUtil;
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

    private static final String LANG_PERM = "enightmarket.lang";

    private final GecePazariPlugin plugin;
    private final LanguageManager lang;
    private final MarketManager manager;
    private final MarketGUI gui;

    public GecePazariCommand(GecePazariPlugin plugin,
                             LanguageManager lang,
                             MarketManager manager,
                             MarketGUI gui) {
        this.plugin = plugin;
        this.lang = lang;
        this.manager = manager;
        this.gui = gui;
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
        if (sub.equals("lang") || sub.equals("language") || sub.equals("dil")) {
            handleLang(sender, args);
        } else {
            ColorUtil.send(sender, lang.get(sender, "unknown-subcommand"));
        }
        return true;
    }

    private void handleOpenGui(CommandSender sender) {
        MarketCommandUtil.openGui(sender, lang, manager, gui);
    }

    private void handleLang(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ColorUtil.send(sender, lang.get(sender, "player-only"));
            return;
        }
        if (!player.hasPermission(LANG_PERM)) {
            ColorUtil.send(player, lang.get(player, "no-permission"));
            return;
        }
        if (args.length < 2) {
            // Mevcut dili goster + kullanim
            String current = lang.getLangFor(player);
            ColorUtil.send(player,
                    lang.get(player, "lang-current").replace("{lang}", current));
            ColorUtil.send(player, lang.get(player, "lang-usage"));
            return;
        }
        String code = args[1].toLowerCase();
        if (!lang.isSupported(code)) {
            String langs = String.join(", ", lang.getSupportedCodes());
            ColorUtil.send(player,
                    lang.get(player, "lang-invalid").replace("{langs}", langs));
            return;
        }
        lang.setLang(player.getUniqueId(), code);
        // Yeni dilde tesekkur mesaji
        ColorUtil.send(player, lang.get(player, "lang-changed"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("lang"), args[0]);
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
