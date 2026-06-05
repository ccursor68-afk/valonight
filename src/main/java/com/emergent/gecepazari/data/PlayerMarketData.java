package com.emergent.gecepazari.data;

import java.util.List;
import java.util.UUID;

/**
 * Bir oyuncuya ait, pazara dair tum kayit. YAML'a serilestirilir.
 */
public final class PlayerMarketData {

    private final UUID playerId;
    private final List<MarketItemInstance> items;
    private final long createdAt;

    public PlayerMarketData(UUID playerId, List<MarketItemInstance> items, long createdAt) {
        this.playerId = playerId;
        this.items = items;
        this.createdAt = createdAt;
    }

    public UUID getPlayerId() { return playerId; }
    public List<MarketItemInstance> getItems() { return items; }
    public long getCreatedAt() { return createdAt; }
}
