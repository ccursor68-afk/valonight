package com.emergent.gecepazari.listeners;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.market.PlayerMarket;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;

/**
 * Tum pazar olaylarini yakalar:
 *  - GUI butonuna tiklama
 *  - Interaction entity'ye sag tik (satin alma)
 *  - Shift'e basma (pazari duman efektiyle kapatir)
 *  - Cikis (pazari temizler)
 *  - Yeni katilim (diger oyuncularin pazar entity'lerini gizler)
 */
public final class MarketListener implements Listener {

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final MarketManager manager;
    private final MarketGUI gui;

    public MarketListener(GecePazariPlugin plugin,
                          ConfigManager config,
                          MarketManager manager,
                          MarketGUI gui) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MarketGUI)) return;
        event.setCancelled(true);

        if (event.getRawSlot() != MarketGUI.BUTTON_SLOT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!manager.isEventActive()) {
            player.sendMessage(ColorUtil.component(config.getMessage("not-active")));
            return;
        }

        // GUI'yi kapat ve pazari fiziksel olarak ac
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            manager.openMarket(player);
            player.sendMessage(ColorUtil.component(config.getMessage("market-opened")));
        });
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Interaction interaction)) return;

        PlayerMarket market = manager.getActiveMarketByInteractionId(interaction.getUniqueId());
        if (market == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!market.getOwner().getUniqueId().equals(player.getUniqueId())) return;

        MarketManager.PurchaseResult result = manager.attemptPurchase(player, interaction.getUniqueId());
        switch (result.status()) {
            case SUCCESS -> {
                String msg = config.getMessage("purchase-success")
                        .replace("{item}", result.template().getDisplayName())
                        .replace("{price}", formatMoney(result.price()));
                player.sendMessage(ColorUtil.component(msg));
            }
            case INSUFFICIENT_FUNDS -> {
                String msg = config.getMessage("insufficient-funds")
                        .replace("{price}", formatMoney(result.price()));
                player.sendMessage(ColorUtil.component(msg));
            }
            case OUT_OF_STOCK -> player.sendMessage(ColorUtil.component(config.getMessage("out-of-stock")));
            default -> { /* sessiz */ }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        PlayerMarket market = manager.getActiveMarket(player);
        if (market == null) return;

        manager.closeMarket(player, true);
        player.sendMessage(ColorUtil.component(config.getMessage("market-closed")));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        manager.closeMarket(player, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        // Diger oyunculara ait aktif pazarlari yeni katilan oyuncudan gizle
        for (PlayerMarket m : manager.getActiveMarkets()) {
            if (m.getOwner().getUniqueId().equals(joiner.getUniqueId())) continue;
            m.hideFrom(joiner);
        }
    }

    private String formatMoney(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
