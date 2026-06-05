package com.emergent.gecepazari.market.display;

import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.Rarity;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.util.ColorUtil;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

/** ArmorStand hologramlari icin tek satirlik legacy metin uretir. */
final class HologramText {

    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0.##", sym);
    }

    private HologramText() {}

    static String sealed(LanguageManager lang, UUID playerId, Rarity rarity) {
        String code = lang.getLangFor(playerId);
        String hint = strip(ColorUtil.legacy(lang.getRaw(code, "sealed-hint")));
        String rarityName = lang.getRarityName(code, rarity);
        return ColorUtil.legacy("&f&l? ? ? &r" + rarity.getLegacyColorCode() + "&l["
                + rarityName + "] &7" + hint);
    }

    static String revealed(LanguageManager lang, UUID playerId,
                           MarketItemTemplate template, MarketItemInstance inst) {
        String code = lang.getLangFor(playerId);
        String stockLabel = lang.getRaw(code, "stock-label");
        String soldOutTag = lang.getRaw(code, "sold-out-tag");

        StringBuilder sb = new StringBuilder();
        sb.append(template.getDisplayName());
        if (template.getAmount() > 1) {
            sb.append(" x").append(template.getAmount());
        }
        sb.append(" &8| ").append(template.getRarity().getLegacyColorCode())
                .append("[").append(lang.getRarityName(code, template.getRarity())).append("]");

        if (inst.isSoldOut()) {
            sb.append(" &8| ").append(soldOutTag);
            sb.append(" &8| &7").append(stockLabel).append(": &c0&7/").append(inst.getInitialStock());
        } else {
            sb.append(" &8| &a-").append(inst.getDiscountPercent()).append("%");
            sb.append(" &8| &e").append(MONEY_FORMAT.format(inst.getFinalPrice())).append("$");
            sb.append(" &8| &7").append(stockLabel).append(": &f")
                    .append(inst.getRemainingStock()).append("&7/").append(inst.getInitialStock());
        }
        return ColorUtil.legacy(sb.toString());
    }

    private static String strip(String colored) {
        return colored.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}
