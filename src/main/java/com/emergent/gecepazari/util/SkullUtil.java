package com.emergent.gecepazari.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Base64 texture'lardan custom oyuncu kafasi (PLAYER_HEAD) olusturur.
 * Paper'in {@link PlayerProfile} API'sini kullanir.
 */
public final class SkullUtil {

    private SkullUtil() {
    }

    public static ItemStack createCustomHead(String textureBase64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (textureBase64 == null || textureBase64.isBlank()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        // UUID base64'ten deterministic olusturuluyor ki ayni texture ayni profil olsun
        UUID profileId = UUID.nameUUIDFromBytes(textureBase64.getBytes());
        PlayerProfile profile = Bukkit.createProfile(profileId, null);
        profile.setProperty(new ProfileProperty("textures", textureBase64));
        meta.setPlayerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }
}
