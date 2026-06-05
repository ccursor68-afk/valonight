package com.emergent.gecepazari.data;

/**
 * Bir oyuncunun pazarinda yer alan, sablondan turetilmis ve indirimi/stogu uygulanmis somut esya.
 */
public final class MarketItemInstance {

    private final String templateId;
    private final int discountPercent;
    private final double finalPrice;
    private final int initialStock;
    private int remainingStock;

    public MarketItemInstance(String templateId,
                              int discountPercent,
                              double finalPrice,
                              int initialStock,
                              int remainingStock) {
        this.templateId = templateId;
        this.discountPercent = discountPercent;
        this.finalPrice = finalPrice;
        this.initialStock = initialStock;
        this.remainingStock = remainingStock;
    }

    public String getTemplateId() { return templateId; }
    public int getDiscountPercent() { return discountPercent; }
    public double getFinalPrice() { return finalPrice; }
    public int getInitialStock() { return initialStock; }
    public int getRemainingStock() { return remainingStock; }

    public boolean isSoldOut() { return remainingStock <= 0; }

    public void decrementStock() {
        if (remainingStock > 0) remainingStock--;
    }

    /** Bu ana kadar kac adet satildi. */
    public int getSoldCount() {
        return Math.max(0, initialStock - remainingStock);
    }
}
