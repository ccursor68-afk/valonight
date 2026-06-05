package com.emergent.gecepazari;

import com.emergent.gecepazari.commands.GecePazariCommand;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.PlayerDataManager;
import com.emergent.gecepazari.discord.DiscordWebhook;
import com.emergent.gecepazari.economy.EconomyHook;
import com.emergent.gecepazari.integration.PlaceholderHook;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.lang.PlayerLangStore;
import com.emergent.gecepazari.listeners.MarketListener;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.market.MarketRoller;
import com.emergent.gecepazari.schedule.ScheduleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ENightMarket plugin'inin giris noktasi. Tum bagimliliklari (manuel DI) burada baglar.
 * Yapimci: ArtfulMiner
 */
public final class GecePazariPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager dataManager;
    private MarketRoller roller;
    private EconomyHook economyHook;
    private MarketManager marketManager;
    private MarketGUI marketGui;
    private DiscordWebhook webhook;
    private LanguageManager languageManager;
    private PlayerLangStore playerLangStore;
    private ScheduleManager scheduleManager;
    private PlaceholderHook placeholderHook;

    @Override
    public void onEnable() {
        // 1) Config
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // 2) Dil sistemi (config.default-language)
        this.playerLangStore = new PlayerLangStore(this);
        this.languageManager = new LanguageManager(this, playerLangStore, configManager.getDefaultLanguage());

        // 3) Vault zorunlu
        this.economyHook = new EconomyHook(this);
        if (!economyHook.setup()) {
            getLogger().severe("Vault ve/veya Economy provider bulunamadi. ENightMarket devre disi birakiliyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 4) Data + RNG
        this.dataManager = new PlayerDataManager(this);
        this.roller = new MarketRoller(configManager);

        // 5) Market Manager (lang manager dahil)
        this.marketManager = new MarketManager(this, configManager, languageManager,
                dataManager, roller, economyHook);

        // 6) GUI & Webhook
        this.marketGui = new MarketGUI(this, configManager, languageManager);
        this.webhook = new DiscordWebhook(this, configManager);

        // 7) Schedule
        this.scheduleManager = new ScheduleManager(this, configManager, marketManager, webhook, languageManager);
        this.scheduleManager.start();

        // 8) Listener
        getServer().getPluginManager().registerEvents(
                new MarketListener(this, languageManager, marketManager, marketGui),
                this
        );

        // 9) Command
        PluginCommand cmd = getCommand("gecepazari");
        if (cmd != null) {
            GecePazariCommand executor = new GecePazariCommand(this, configManager, languageManager,
                    marketManager, marketGui, webhook, scheduleManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().warning("/gecepazari komutu kaydedilemedi (plugin.yml kontrol edin).");
        }

        // 10) PlaceholderAPI (opsiyonel)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderHook = new PlaceholderHook(this, marketManager, scheduleManager, languageManager);
            if (placeholderHook.register()) {
                getLogger().info("PlaceholderAPI entegrasyonu etkin (%enightmarket_...%).");
            }
        }

        getLogger().info("ENightMarket basariyla yuklendi. (by ArtfulMiner)");
    }

    @Override
    public void onDisable() {
        if (scheduleManager != null) scheduleManager.stop();
        if (marketManager != null) marketManager.closeAll(false);
        if (placeholderHook != null) {
            try { placeholderHook.unregister(); } catch (Exception ignored) {}
        }
        getLogger().info("ENightMarket devre disi birakildi.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getDataManager() { return dataManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public EconomyHook getEconomyHook() { return economyHook; }
    public LanguageManager getLanguageManager() { return languageManager; }
    public ScheduleManager getScheduleManager() { return scheduleManager; }
}
