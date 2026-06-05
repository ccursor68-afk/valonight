package com.emergent.gecepazari.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Base64 texture'lardan custom oyuncu kafasi olusturur.
 * Paper PlayerProfile, Spigot/Bukkit GameProfile reflection destegi icerir.
 */
public final class SkullUtil {

    private static final boolean PAPER_PROFILE;
    private static final Method CREATE_PROFILE;
    private static final Method SET_PLAYER_PROFILE;

    static {
        Method createProfile = null;
        Method setPlayerProfile = null;
        boolean paper = false;
        try {
            Class<?> profileClass = Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            createProfile = Bukkit.class.getMethod("createProfile", UUID.class, String.class);
            setPlayerProfile = SkullMeta.class.getMethod("setPlayerProfile", profileClass);
            paper = true;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
        }
        CREATE_PROFILE = createProfile;
        SET_PLAYER_PROFILE = setPlayerProfile;
        PAPER_PROFILE = paper;
    }

    private SkullUtil() {}

    public static ItemStack createCustomHead(String textureBase64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (textureBase64 == null || textureBase64.isBlank()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        UUID profileId = UUID.nameUUIDFromBytes(textureBase64.getBytes());
        if (PAPER_PROFILE) {
            applyPaperProfile(meta, profileId, textureBase64);
        } else {
            applyGameProfileReflection(meta, profileId, textureBase64);
        }

        head.setItemMeta(meta);
        return head;
    }

    private static void applyPaperProfile(SkullMeta meta, UUID profileId, String textureBase64) {
        try {
            Object profile = CREATE_PROFILE.invoke(null, profileId, null);
            Class<?> propClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            Object property = propClass.getConstructor(String.class, String.class)
                    .newInstance("textures", textureBase64);
            Method setProperty = profile.getClass().getMethod("setProperty", propClass);
            setProperty.invoke(profile, property);
            SET_PLAYER_PROFILE.invoke(meta, profile);
        } catch (ReflectiveOperationException ex) {
            applyGameProfileReflection(meta, profileId, textureBase64);
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyGameProfileReflection(SkullMeta meta, UUID profileId, String textureBase64) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(profileId, "");
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", textureBase64);

            Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
            Method put = properties.getClass().getMethod("put", Object.class, Object.class);
            put.invoke(properties, "textures", property);

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (ReflectiveOperationException ex) {
            // Son care: bos kafa
        }
    }
}
