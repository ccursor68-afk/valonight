package com.emergent.gecepazari.data;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

/**
 * Config'den okunan, degismez bir esya sablonu.
 * Bir oyuncunun pazarinda yer alan {@link MarketItemInstance} bu sablona referans verir.
 */
public final class MarketItemTemplate {

    public enum RewardType { ITEM, COMMAND }

    private final String id;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final double basePrice;
    private final int chanceWeight;
    private final int maxStock;
    private final int minDiscount;
    private final int maxDiscount;
    private final RewardType rewardType;
    private final int amount;
    private final String command;
    private final Rarity rarity;
    private final Integer customModelData;

    public MarketItemTemplate(String id,
                              Material material,
                              String displayName,
                              List<String> lore,
                              double basePrice,
                              int chanceWeight,
                              int maxStock,
                              int minDiscount,
                              int maxDiscount,
                              RewardType rewardType,
                              int amount,
                              String command,
                              Rarity rarity,
                              Integer customModelData) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore == null ? Collections.emptyList() : List.copyOf(lore);
        this.basePrice = basePrice;
        this.chanceWeight = chanceWeight;
        this.maxStock = maxStock;
        this.minDiscount = minDiscount;
        this.maxDiscount = maxDiscount;
        this.rewardType = rewardType;
        this.amount = amount;
        this.command = command == null ? "" : command;
        this.rarity = rarity == null ? Rarity.SIRADAN : rarity;
        this.customModelData = customModelData;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public double getBasePrice() { return basePrice; }
    public int getChanceWeight() { return chanceWeight; }
    public int getMaxStock() { return maxStock; }
    public int getMinDiscount() { return minDiscount; }
    public int getMaxDiscount() { return maxDiscount; }
    public RewardType getRewardType() { return rewardType; }
    public int getAmount() { return amount; }
    public String getCommand() { return command; }
    public Rarity getRarity() { return rarity; }
    public Integer getCustomModelData() { return customModelData; }
    public boolean hasCustomModelData() { return customModelData != null; }
}
