package com.emergent.gecepazari.compat;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.TextDisplay;

import java.lang.reflect.Method;

/** TextDisplay metin atamasi — reflection ile Paper/Spigot API farklarini asar. */
public final class TextDisplayCompat {

    private static final Method TEXT_METHOD;

    static {
        Method text = null;
        try {
            text = TextDisplay.class.getMethod("text", Component.class);
        } catch (NoSuchMethodException ignored) {
        }
        TEXT_METHOD = text;
    }

    private TextDisplayCompat() {}

    public static void setText(TextDisplay display, Component component) {
        if (display == null || component == null || TEXT_METHOD == null) return;
        try {
            TEXT_METHOD.invoke(display, component);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
