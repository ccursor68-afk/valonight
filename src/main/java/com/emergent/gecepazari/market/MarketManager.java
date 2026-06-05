package com.emergent.gecepazari.market;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerDataManager;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.economy.EconomyHook;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aktif oyuncu pazarlarini, etkinligin acik mi kapali mi oldugunu ve etkinlik durumunun
 * kalici hale getirilmesini yonetir.
 */
public final class MarketManager {

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final PlayerDataManager dataManager;
    private final MarketRoller roller;
    private final EconomyHook economy;

    private final Map<UUID, PlayerMarket> activeMarkets = new ConcurrentHashMap<>();
    private boolean eventActive = false;

    private final File eventFile;

    public MarketManager(GecePazariPlugin plugin,
                         ConfigManager config,
                         PlayerDataManager dataManager,
                         MarketRoller roller,
                         EconomyHook economy) {
        this.plugin = plugin;
        this.config = config;
        this.dataManager = dataManager;
        this.roller = roller;
        this.economy = economy;
        this.eventFile = new File(plugin.getDataFolder(), "event.yml");
        loadEventState();
    }

    private void loadEventState() {
        if (!eventFile.exists()) {
            eventActive = false;
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(eventFile);
        eventActive = y.getBoolean("active", false);
    }

    private void saveEventState() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("active", eventActive);
        try {
            if (!eventFile.getParentFile().exists()) eventFile.getParentFile().mkdirs();
            y.save(eventFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("event.yml kaydedilemedi: " + ex.getMessage());
        }
    }

    public boolean isEventActive() { return eventActive; }

    public boolean startEvent() {
        if (eventActive) return false;
        eventActive = true;
        saveEventState();
        return true;
    }

    public boolean stopEvent() {
        if (!eventActive) return false;
        eventActive = false;
        saveEventState();
        // Tum aktif pazarlari kapat
        for (PlayerMarket m : new HashMap<>(activeMarkets).values()) {
            closeMarket(m.getOwner(), false);
        }
        // Tum playerdata'yi sil
        dataManager.deleteAll();
        return true;
    }

    /**
     * Oyuncunun pazarini fiziksel olarak acar. Eger oyuncunun kaydedilmis verisi yoksa RNG ile uretir.
     */
    public PlayerMarket openMarket(Player player) {
        UUID id = player.getUniqueId();
        if (activeMarkets.containsKey(id)) {
            return activeMarkets.get(id);
        }

        PlayerMarketData data;
        if (dataManager.has(id)) {
            data = dataManager.load(id);
        } else {
            data = roller.roll(id);
            dataManager.save(data);
        }

        PlayerMarket market = new PlayerMarket(plugin, config, player, data);
        activeMarkets.put(id, market);
        market.spawn();
        return market;
    }

    public PlayerMarket getActiveMarket(Player player) {
        return activeMarkets.get(player.getUniqueId());
    }

    public PlayerMarket getActiveMarketByInteractionId(UUID interactionId) {
        for (PlayerMarket m : activeMarkets.values()) {
            if (m.findByInteraction(interactionId) != null) return m;
        }
        return null;
    }

    public void closeMarket(Player player, boolean withSmoke) {
        PlayerMarket m = activeMarkets.remove(player.getUniqueId());
        if (m == null) return;
        m.close(withSmoke);
    }

    public void closeAll(boolean withSmoke) {
        for (PlayerMarket m : new HashMap<>(activeMarkets).values()) {
            m.close(withSmoke);
        }
        activeMarkets.clear();
    }

    public Iterable<PlayerMarket> getActiveMarkets() {
        return activeMarkets.values();
    }

    /**
     * Bir slot icin tek tiklama olayini isler:
     *  - Slot sealed ise reveal olayini tetikler ve {@link InteractionResult#REVEAL} doner.
     *  - Slot acik ise satin alma sonucunu doner.
     */
    public InteractionResult handleInteraction(Player player, UUID interactionId) {
        PlayerMarket market = getActiveMarketByInteractionId(interactionId);
        if (market == null || !market.getOwner().getUniqueId().equals(player.getUniqueId())) {
            return new InteractionResult(InteractionType.IGNORED, null, null, 0);
        }
        PlayerMarket.MarketSlotView slot = market.findByInteraction(interactionId);
        if (slot == null) return new InteractionResult(InteractionType.IGNORED, null, null, 0);

        if (slot.sealed()) {
            if (market.revealSlot(interactionId)) {
                // Reveal kalici olmali: YAML'a kaydet ki ikinci acilista direkt esya gozuksun.
                dataManager.save(market.getData());
                return new InteractionResult(InteractionType.REVEAL, slot.template(), null, 0);
            }
            return new InteractionResult(InteractionType.IGNORED, null, null, 0);
        }

        PurchaseResult pr = attemptPurchase(player, interactionId);
        return new InteractionResult(InteractionType.PURCHASE, pr.template(), pr.status(), pr.price());
    }

    public enum InteractionType { REVEAL, PURCHASE, IGNORED }

    public record InteractionResult(InteractionType type,
                                    MarketItemTemplate template,
                                    PurchaseStatus purchaseStatus,
                                    double price) {}

    /**
     * Bir slot icin satin alma denemesi. Tum ekonomi, stok ve odul mantigini yurutur.
     */
    public PurchaseResult attemptPurchase(Player player, UUID interactionId) {
        PlayerMarket market = getActiveMarketByInteractionId(interactionId);
        if (market == null || !market.getOwner().getUniqueId().equals(player.getUniqueId())) {
            return new PurchaseResult(PurchaseStatus.NOT_OWNER, null, 0);
        }
        PlayerMarket.MarketSlotView slot = market.findByInteraction(interactionId);
        if (slot == null) return new PurchaseResult(PurchaseStatus.NOT_FOUND, null, 0);

        MarketItemInstance inst = slot.instance();
        MarketItemTemplate template = slot.template();
        if (inst.isSoldOut()) return new PurchaseResult(PurchaseStatus.OUT_OF_STOCK, template, 0);

        double price = inst.getFinalPrice();
        if (!economy.has(player, price)) {
            return new PurchaseResult(PurchaseStatus.INSUFFICIENT_FUNDS, template, price);
        }
        if (!economy.withdraw(player, price)) {
            return new PurchaseResult(PurchaseStatus.INSUFFICIENT_FUNDS, template, price);
        }

        // Odul ver
        if (template.getRewardType() == MarketItemTemplate.RewardType.COMMAND) {
            String cmd = template.getCommand().replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
            ItemStack give = new ItemStack(template.getMaterial(), template.getAmount());
            ItemMeta meta = give.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtil.component(template.getDisplayName()));
                if (!template.getLore().isEmpty()) meta.lore(ColorUtil.components(template.getLore()));
                if (template.hasCustomModelData()) meta.setCustomModelData(template.getCustomModelData());
                give.setItemMeta(meta);
            }
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(give);
            for (ItemStack rem : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }
        }

        inst.decrementStock();
        market.refreshSlotHologram(interactionId);
        dataManager.save(market.getData());

        return new PurchaseResult(PurchaseStatus.SUCCESS, template, price);
    }

    public enum PurchaseStatus {
        SUCCESS, INSUFFICIENT_FUNDS, OUT_OF_STOCK, NOT_FOUND, NOT_OWNER
    }

    public record PurchaseResult(PurchaseStatus status, MarketItemTemplate template, double price) {}
}
