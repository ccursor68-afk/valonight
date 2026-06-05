package com.emergent.gecepazari.compat;

import com.emergent.gecepazari.util.ColorUtil;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Tum surumlerde calisan legacy ItemMeta yardimcilari. */
public final class ItemMetaCompat {

    private ItemMetaCompat() {}

    public static void setDisplayName(ItemMeta meta, String legacyAmpersand) {
        if (meta == null) return;
        meta.setDisplayName(ColorUtil.legacy(legacyAmpersand));
    }

    public static void setDisplayNameBlank(ItemMeta meta) {
        if (meta == null) return;
        meta.setDisplayName(" ");
    }

    public static void setLore(ItemMeta meta, List<String> legacyLines) {
        if (meta == null || legacyLines == null || legacyLines.isEmpty()) return;
        List<String> out = new ArrayList<>(legacyLines.size());
        for (String line : legacyLines) {
            out.add(ColorUtil.legacy(line));
        }
        meta.setLore(out);
    }
}
