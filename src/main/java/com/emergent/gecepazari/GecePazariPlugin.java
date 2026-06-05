package com.emergent.gecepazari;

import com.emergent.gecepazari.commands.GecePazariCommand;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.PlayerDataManager;
import com.emergent.gecepazari.discord.DiscordWebhook;
import com.emergent.gecepazari.economy.EconomyHook;
import com.emergent.gecepazari.listeners.MarketListener;
import com.emergent.gecepazari.market.MarketGUI;
import com.emergent.gecepazari.market.MarketManager;
import com.emergent.gecepazari.market.MarketRoller;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin'in giris noktasi. Tum bagimliliklari (manuel DI) burada baglar.
 */
public final class GecePazariPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataManager dataManager;
    private MarketRoller roller;
    private EconomyHook economyHook;
    private MarketManager marketManager;
    private MarketGUI marketGui;
    private DiscordWebhook webhook;

    @Override
    public void onEnable() {
        // 1) Config
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // 2) Vault zorunlu
        this.economyHook = new EconomyHook(this);
        if (!economyHook.setup()) {
            getLogger().severe("Vault ve/veya Economy provider bulunamadi. Plugin devre disi birakiliyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3) Data + RNG
        this.dataManager = new PlayerDataManager(this);
        this.roller = new MarketRoller(configManager);

        // 4) Market Manager
        this.marketManager = new MarketManager(this, configManager, dataManager, roller, economyHook);

        // 5) GUI & Webhook
        this.marketGui = new MarketGUI(this, configManager);
        this.webhook = new DiscordWebhook(this, configManager);

        // 6) Listener
        getServer().getPluginManager().registerEvents(
                new MarketListener(this, configManager, marketManager, marketGui),
                this
        );

        // 7) Command
        PluginCommand cmd = getCommand("gecepazari");
        if (cmd != null) {
            GecePazariCommand executor = new GecePazariCommand(this, configManager, marketManager, marketGui, webhook);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        } else {
            getLogger().warning("/gecepazari komutu kaydedilemedi (plugin.yml kontrol edin).");
        }

        getLogger().info("Gece Pazari basariyla yuklendi.");
    }

    @Override
    public void onDisable() {
        if (marketManager != null) {
            marketManager.closeAll(false);
        }
        getLogger().info("Gece Pazari devre disi birakildi.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getDataManager() { return dataManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public EconomyHook getEconomyHook() { return economyHook; }
}
