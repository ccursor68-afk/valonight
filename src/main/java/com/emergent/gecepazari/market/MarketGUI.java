package com.emergent.gecepazari.market;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Pazari fiziksel olarak acmadan once gosterilen tek butonlu Chest GUI.
 * Baslik ve buton metni, oyuncunun secili diline gore dil dosyalarindan alinir.
 */
public final class MarketGUI implements InventoryHolder {

    public static final int BUTTON_SLOT = 13;
    private static final int GUI_SIZE = 27;

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final LanguageManager lang;
    private Inventory inventory;

    public MarketGUI(GecePazariPlugin plugin, ConfigManager config, LanguageManager lang) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
    }

    public void open(Player player) {
        String title = lang.getRaw(player, "gui-title");
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, ColorUtil.component(title));

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(ColorUtil.component(" "));
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < GUI_SIZE; i++) inventory.setItem(i, filler);

        ItemStack button = new ItemStack(config.getGuiOpenButtonMaterial());
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(lang.getRaw(player, "gui-open-button-name")));
            List<String> lore = lang.getRawList(player, "gui-open-button-lore");
            if (lore != null && !lore.isEmpty()) {
                meta.lore(ColorUtil.components(lore));
            }
            button.setItemMeta(meta);
        }
        inventory.setItem(BUTTON_SLOT, button);

        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public static boolean isHolder(HumanEntity entity) {
        return entity.getOpenInventory().getTopInventory().getHolder() instanceof MarketGUI;
    }
}
