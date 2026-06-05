package com.emergent.gecepazari.listeners;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.market.PlayerMarket;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;

public final class MarketListener implements Listener {

    private final GecePazariPlugin plugin;
    private final LanguageManager lang;
    private final MarketManager manager;
    private final MarketGUI gui;

    public MarketListener(GecePazariPlugin plugin,
                          LanguageManager lang,
                          MarketManager manager,
                          MarketGUI gui) {
        this.plugin = plugin;
        this.lang = lang;
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
            ColorUtil.send(player, lang.get(player, "not-active"));
            player.closeInventory();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            if (manager.openMarket(player) == null) {
                ColorUtil.send(player, lang.get(player, "not-active"));
                return;
            }
            ColorUtil.send(player, lang.get(player, "market-opened"));
        });
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleMarketClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleMarketClick(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleMarketClick(Player player, Entity clicked, org.bukkit.event.Cancellable event) {
        if (clicked == null) return;

        PlayerMarket market = manager.getActiveMarketByInteractionId(clicked.getUniqueId());
        if (market == null) return;

        event.setCancelled(true);
        if (!market.getOwner().getUniqueId().equals(player.getUniqueId())) return;

        if (!manager.isEventActive()) {
            ColorUtil.send(player, lang.get(player, "not-active"));
            manager.closeMarket(player, false);
            return;
        }

        MarketManager.InteractionResult result = manager.handleInteraction(player, clicked.getUniqueId());
        switch (result.type()) {
            case REVEAL -> { /* efektler backend icinde */ }
            case PURCHASE -> {
                if (result.purchaseStatus() == null) return;
                switch (result.purchaseStatus()) {
                    case SUCCESS -> {
                        String msg = lang.get(player, "purchase-success")
                                .replace("{item}", result.template().getDisplayName())
                                .replace("{price}", formatMoney(result.price()));
                        ColorUtil.send(player, msg);
                    }
                    case INSUFFICIENT_FUNDS -> {
                        String msg = lang.get(player, "insufficient-funds")
                                .replace("{price}", formatMoney(result.price()));
                        ColorUtil.send(player, msg);
                    }
                    case OUT_OF_STOCK -> ColorUtil.send(player, lang.get(player, "out-of-stock"));
                    case EVENT_INACTIVE -> ColorUtil.send(player, lang.get(player, "not-active"));
                    default -> { /* sessiz */ }
                }
            }
            case IGNORED -> { /* sessiz */ }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        PlayerMarket market = manager.getActiveMarket(player);
        if (market == null) return;

        manager.closeMarket(player, true);
        ColorUtil.send(player, lang.get(player, "market-closed"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.closeMarket(event.getPlayer(), false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
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
