package com.emergent.gecepazari.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Oyuncularin dil tercihlerini playerlang.yml dosyasinda saklar.
 * Yapi: &lt;uuid&gt;: &lt;lang-code&gt;
 */
public final class PlayerLangStore {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public PlayerLangStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerlang.yml");
        load();
    }

    private void load() {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.WARNING, "playerlang.yml olusturulamadi", ex);
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    public String getLang(UUID uuid, String fallback) {
        return yaml.getString(uuid.toString(), fallback);
    }

    public void setLang(UUID uuid, String code) {
        yaml.set(uuid.toString(), code);
        save();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "playerlang.yml kaydedilemedi", ex);
        }
    }
}
