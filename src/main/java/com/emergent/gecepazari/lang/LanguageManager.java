package com.emergent.gecepazari.lang;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Dil dosyalarini (en.yml, tr.yml, de.yml) JAR icinden plugin/lang/ klasorune cikartir
 * ve oyuncuya gore dogru mesaji dondurur.
 */
public final class LanguageManager {

    private static final String[] SUPPORTED = {"en", "tr", "de"};

    private final Plugin plugin;
    private final PlayerLangStore store;
    private final Map<String, YamlConfiguration> languages = new LinkedHashMap<>();
    private String defaultLang;

    public LanguageManager(Plugin plugin, PlayerLangStore store, String defaultLang) {
        this.plugin = plugin;
        this.store = store;
        this.defaultLang = normalize(defaultLang);
        loadLanguages();
    }

    public void reload(String defaultLang) {
        this.defaultLang = normalize(defaultLang);
        languages.clear();
        loadLanguages();
    }

    private void loadLanguages() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("lang/ klasoru olusturulamadi.");
        }

        for (String code : SUPPORTED) {
            File f = new File(langDir, code + ".yml");
            if (!f.exists()) {
                try {
                    plugin.saveResource("lang/" + code + ".yml", false);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Dil dosyasi JAR icinde yok: " + code, ex);
                    continue;
                }
            }
            languages.put(code, YamlConfiguration.loadConfiguration(f));
        }
        if (!languages.containsKey(defaultLang)) {
            defaultLang = languages.keySet().iterator().hasNext() ? languages.keySet().iterator().next() : "en";
        }
    }

    public boolean isSupported(String code) {
        return languages.containsKey(normalize(code));
    }

    public String[] getSupportedCodes() {
        return SUPPORTED;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    /** CommandSender'in dilini bulur (oyuncu ise UUID'ye gore, console ise default). */
    public String getLangFor(CommandSender sender) {
        if (sender instanceof Player p) {
            return store.getLang(p.getUniqueId(), defaultLang);
        }
        return defaultLang;
    }

    public String getLangFor(UUID uuid) {
        return store.getLang(uuid, defaultLang);
    }

    public void setLang(UUID uuid, String code) {
        store.setLang(uuid, normalize(code));
    }

    /** Prefix'siz raw mesaj. */
    public String getRaw(String langCode, String key) {
        YamlConfiguration y = languages.get(normalize(langCode));
        if (y == null) y = languages.get(defaultLang);
        if (y == null) return key;
        String msg = y.getString(key);
        if (msg == null) {
            // Fallback default lang
            YamlConfiguration fb = languages.get(defaultLang);
            if (fb != null) msg = fb.getString(key, key);
            else msg = key;
        }
        return msg;
    }

    public List<String> getRawList(String langCode, String key) {
        YamlConfiguration y = languages.get(normalize(langCode));
        if (y == null) y = languages.get(defaultLang);
        if (y == null) return List.of();
        if (!y.isSet(key)) {
            YamlConfiguration fb = languages.get(defaultLang);
            if (fb != null) return fb.getStringList(key);
            return List.of();
        }
        return y.getStringList(key);
    }

    /** Prefix dahil mesaj. */
    public String get(String langCode, String key) {
        String prefix = getRaw(langCode, "prefix");
        String msg = getRaw(langCode, key);
        return prefix + msg;
    }

    public String get(CommandSender sender, String key) {
        return get(getLangFor(sender), key);
    }

    public String getRaw(CommandSender sender, String key) {
        return getRaw(getLangFor(sender), key);
    }

    public List<String> getRawList(CommandSender sender, String key) {
        return getRawList(getLangFor(sender), key);
    }

    private static String normalize(String code) {
        return code == null ? "en" : code.trim().toLowerCase();
    }
}
