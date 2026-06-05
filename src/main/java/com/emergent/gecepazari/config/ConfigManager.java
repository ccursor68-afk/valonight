package com.emergent.gecepazari.config;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.data.MarketItemTemplate;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * config.yml'i okur ve guclu sekilde tipli erisim saglar.
 */
public final class ConfigManager {

    private final GecePazariPlugin plugin;

    private Map<String, MarketItemTemplate> itemPool = Collections.emptyMap();

    public ConfigManager(GecePazariPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        loadItemPool();
    }

    public void reload() {
        load();
    }

    private void loadItemPool() {
        Map<String, MarketItemTemplate> map = new LinkedHashMap<>();
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection section = cfg.getConfigurationSection("items");
        if (section == null) {
            plugin.getLogger().warning("config.yml icinde 'items' bolumu bulunamadi.");
            itemPool = map;
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            String matName = s.getString("material", "STONE");
            Material material;
            try {
                material = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().log(Level.WARNING, "Gecersiz material: " + matName + " (esya: " + key + ")");
                continue;
            }

            String displayName = s.getString("display-name", key);
            List<String> lore = s.getStringList("lore");
            double basePrice = s.getDouble("base-price", 100.0);
            int chance = s.getInt("chance", 10);
            int maxStock = Math.max(1, s.getInt("max-stock", 1));
            int minDiscount = s.getInt("discount.min", 10);
            int maxDiscount = s.getInt("discount.max", 50);
            if (minDiscount < 0) minDiscount = 0;
            if (maxDiscount > 100) maxDiscount = 100;
            if (maxDiscount < minDiscount) maxDiscount = minDiscount;

            String typeStr = s.getString("reward.type", "ITEM").toUpperCase();
            MarketItemTemplate.RewardType type;
            try {
                type = MarketItemTemplate.RewardType.valueOf(typeStr);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Gecersiz reward.type: " + typeStr + " (esya: " + key + ")");
                type = MarketItemTemplate.RewardType.ITEM;
            }
            int amount = Math.max(1, s.getInt("reward.amount", 1));
            String command = s.getString("reward.command", "");

            map.put(key, new MarketItemTemplate(
                    key, material, displayName, lore,
                    basePrice, chance, maxStock,
                    minDiscount, maxDiscount,
                    type, amount, command
            ));
        }
        itemPool = Collections.unmodifiableMap(map);
        plugin.getLogger().info("Yuklenen esya sayisi: " + itemPool.size());
    }

    public Map<String, MarketItemTemplate> getItemPool() {
        return itemPool;
    }

    public MarketItemTemplate getTemplate(String id) {
        return itemPool.get(id);
    }

    public List<MarketItemTemplate> getItemPoolList() {
        return new ArrayList<>(itemPool.values());
    }

    public String getMessage(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString("messages." + key, "");
        return prefix + msg;
    }

    public String getRawMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public String getDiscordWebhookUrl() {
        return plugin.getConfig().getString("discord-webhook-url", "");
    }

    public String getDiscordUsername() {
        return plugin.getConfig().getString("discord.username", "Gece Pazari");
    }

    public String getDiscordAvatarUrl() {
        return plugin.getConfig().getString("discord.avatar-url", "");
    }

    public String getDiscordEmbedTitle() {
        return plugin.getConfig().getString("discord.embed.title", "Gece Pazari Acildi!");
    }

    public String getDiscordEmbedDescription() {
        return plugin.getConfig().getString("discord.embed.description", "");
    }

    public int getDiscordEmbedColor() {
        return plugin.getConfig().getInt("discord.embed.color", 13369599);
    }

    public String getDiscordEmbedFooter() {
        return plugin.getConfig().getString("discord.embed.footer", "");
    }

    public String getDiscordEmbedThumbnail() {
        return plugin.getConfig().getString("discord.embed.thumbnail-url", "");
    }

    public String getGuiTitle() {
        return plugin.getConfig().getString("gui.title", "&8&lGece Pazari");
    }

    public Material getGuiOpenButtonMaterial() {
        String name = plugin.getConfig().getString("gui.open-button.material", "NETHER_STAR");
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Material.NETHER_STAR;
        }
    }

    public String getGuiOpenButtonName() {
        return plugin.getConfig().getString("gui.open-button.name", "&d&lPazari Ac");
    }

    public List<String> getGuiOpenButtonLore() {
        return plugin.getConfig().getStringList("gui.open-button.lore");
    }

    public double getRadius() { return plugin.getConfig().getDouble("market.radius", 3.0); }
    public double getHeightOffset() { return plugin.getConfig().getDouble("market.height-offset", 0.2); }
    public double getArcDegrees() { return plugin.getConfig().getDouble("market.arc-degrees", 160.0); }
    public int getTeleportDuration() { return plugin.getConfig().getInt("market.teleport-duration", 3); }
    public int getUpdateIntervalTicks() { return plugin.getConfig().getInt("market.update-interval-ticks", 2); }
    public double getRotationSpeed() { return plugin.getConfig().getDouble("market.rotation-speed", 25.0); }
}
