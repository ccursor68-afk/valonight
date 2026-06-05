package com.emergent.gecepazari.commands;

import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class MarketCommandUtil {

    private static final String USE_PERM = "enightmarket.use";

    private MarketCommandUtil() {}

    static void openGui(CommandSender sender, LanguageManager lang, MarketManager manager, MarketGUI gui) {
        if (!(sender instanceof Player player)) {
            ColorUtil.send(sender, lang.get(sender, "player-only"));
            return;
        }
        if (!player.hasPermission(USE_PERM)) {
            ColorUtil.send(player, lang.get(player, "no-permission"));
            return;
        }
        if (!manager.isEventActive()) {
            ColorUtil.send(player, lang.get(player, "not-active"));
            return;
        }
        gui.open(player);
    }
}
