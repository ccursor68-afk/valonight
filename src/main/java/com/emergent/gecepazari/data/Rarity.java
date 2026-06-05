package com.emergent.gecepazari.data;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Esya nadirlik dereceleri. Her dereceye karsilik gelen oyuncu kafasi base64 texture'u burada tanimli.
 */
public enum Rarity {
    SIRADAN(
            "Siradan",
            NamedTextColor.GRAY,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjQyNWZmODA2NmU2OTlkNTE1MDU2Zjc4MjcxNTQ2YmFhODhkNzgxZjFhODk0NjUzYTBmNDAxOWU4YjE0YmNiNCJ9fX0="
    ),
    NADIR(
            "Nadir",
            NamedTextColor.AQUA,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmIwOWRmOGYzODc1Yjk1YzlkMDU3NzI4MTUyZjlmNDRlNTQwZmQ3YWY4MjVkZjQ0YjNkYjQxNTVjY2VlYzQyOSJ9fX0="
    ),
    DESTANSI(
            "Destansi",
            NamedTextColor.LIGHT_PURPLE,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDA4OTQ3ODE4MjBmODFiNzA2YTA4MTU3N2Q3MzkxYTZjMzBmMmM0NzRiMDg3YWNhMTRkNTRiNmY2NTlmMmVhNyJ9fX0="
    ),
    EFSANEVI(
            "Efsanevi",
            NamedTextColor.GOLD,
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ1YzM4ZmI1NjMyMGZhMTM2MzdhY2ZlODIwNzdiNmQ3ODBiODg5Zjc3NDJkNjliY2M2ZTVhZTAzNWVhZmQyNyJ9fX0="
    );

    private final String displayName;
    private final TextColor color;
    private final String skullTextureBase64;

    Rarity(String displayName, TextColor color, String skullTextureBase64) {
        this.displayName = displayName;
        this.color = color;
        this.skullTextureBase64 = skullTextureBase64;
    }

    public String getDisplayName() { return displayName; }
    public TextColor getColor() { return color; }
    public String getSkullTextureBase64() { return skullTextureBase64; }

    public static Rarity fromString(String s) {
        if (s == null) return SIRADAN;
        try {
            return Rarity.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SIRADAN;
        }
    }
}
