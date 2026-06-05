package com.emergent.gecepazari.compat;

import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class InventoryCompat {

    private InventoryCompat() {}

    public static Inventory create(InventoryHolder holder, int size, String legacyAmpersandTitle) {
        return Bukkit.createInventory(holder, size, ColorUtil.legacy(legacyAmpersandTitle));
    }
}
