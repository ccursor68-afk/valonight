package com.emergent.gecepazari.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Renk kodlarini (&) Adventure {@link Component} nesnelerine cevirir.
 * Paper 1.21+ Adventure API'sini kullanir.
 */
public final class ColorUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {
    }

    public static Component component(String legacy) {
        if (legacy == null) {
            return Component.empty();
        }
        return SERIALIZER.deserialize(legacy).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    }

    public static List<Component> components(List<String> lines) {
        List<Component> out = new ArrayList<>();
        if (lines == null) return out;
        for (String line : lines) {
            out.add(component(line));
        }
        return out;
    }

    public static String legacyToPlain(String legacy) {
        // Sadece kayit/sablon icin: renk kodlarini kaldirmak isteyenler icin.
        return legacy == null ? "" : legacy.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
