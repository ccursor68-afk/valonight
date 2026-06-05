package com.emergent.gecepazari.data;

import com.emergent.gecepazari.GecePazariPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Her oyuncunun pazar verisini YAML dosyasi halinde saklar:
 *   plugins/GecePazari/playerdata/&lt;uuid&gt;.yml
 */
public final class PlayerDataManager {

    private final GecePazariPlugin plugin;
    private final File dataFolder;

    public PlayerDataManager(GecePazariPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("playerdata klasoru olusturulamadi: " + dataFolder.getPath());
        }
    }

    private File fileFor(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    public boolean has(UUID uuid) {
        return fileFor(uuid).exists();
    }

    public PlayerMarketData load(UUID uuid) {
        File file = fileFor(uuid);
        if (!file.exists()) return null;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        long createdAt = yaml.getLong("created-at", System.currentTimeMillis());
        List<MarketItemInstance> items = new ArrayList<>();
        ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection s = itemsSection.getConfigurationSection(key);
                if (s == null) continue;
                String tid = s.getString("template-id", "");
                int discount = s.getInt("discount", 0);
                double price = s.getDouble("final-price", 0.0);
                int stock = s.getInt("stock", 0);
                int initialStock = s.getInt("initial-stock", stock);
                boolean revealed = s.getBoolean("revealed", false);
                if (tid.isEmpty()) continue;
                items.add(new MarketItemInstance(tid, discount, price, initialStock, stock, revealed));
            }
        }
        return new PlayerMarketData(uuid, items, createdAt);
    }

    public void save(PlayerMarketData data) {
        File file = fileFor(data.getPlayerId());
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("created-at", data.getCreatedAt());
        int idx = 0;
        for (MarketItemInstance inst : data.getItems()) {
            String key = "items.slot-" + idx;
            yaml.set(key + ".template-id", inst.getTemplateId());
            yaml.set(key + ".discount", inst.getDiscountPercent());
            yaml.set(key + ".final-price", inst.getFinalPrice());
            yaml.set(key + ".stock", inst.getRemainingStock());
            yaml.set(key + ".initial-stock", inst.getInitialStock());
            yaml.set(key + ".revealed", inst.isRevealed());
            idx++;
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Oyuncu verisi kaydedilemedi: " + file.getName(), ex);
        }
    }

    public void delete(UUID uuid) {
        File f = fileFor(uuid);
        if (f.exists() && !f.delete()) {
            plugin.getLogger().warning("Oyuncu verisi silinemedi: " + f.getName());
        }
    }

    /** Tum playerdata dosyalarini siler. Etkinlik durdurulurken kullanilir. */
    public void deleteAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            if (!f.delete()) {
                plugin.getLogger().warning("Silinemedi: " + f.getName());
            }
        }
    }
}
