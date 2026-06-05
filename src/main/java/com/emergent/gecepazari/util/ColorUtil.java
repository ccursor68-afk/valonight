package com.emergent.gecepazari.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Renk kodlarini (&) Adventure {@link Component} ve legacy metne cevirir.
 * Spigot/Bukkit uyumlulugu icin oyuncuya mesaj gonderiminde {@link #send} kullanin.
 */
public final class ColorUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {}

    /** & kodlu metni § ile cevir (tum surumler). */
    public static String legacy(String ampersandText) {
        if (ampersandText == null) return "";
        return ChatColor.translateAlternateColorCodes('&', ampersandText);
    }

    /** Paper/Spigot/Bukkit uyumlu mesaj gonderimi (& renk kodlari). */
    public static void send(CommandSender sender, String ampersandMessage) {
        if (sender == null || ampersandMessage == null) return;
        sender.sendMessage(legacy(ampersandMessage));
    }

    public static Component component(String legacy) {
        if (legacy == null) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(legacy)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    public static List<Component> components(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines == null) return out;
        for (String line : lines) {
            out.add(component(line));
        }
        return out;
    }
}
