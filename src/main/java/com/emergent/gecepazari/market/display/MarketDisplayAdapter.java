package com.emergent.gecepazari.market.display;

import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerMarketData;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Fiziksel pazar goruntuleme backend'i (Display entity veya ArmorStand). */
public interface MarketDisplayAdapter {

    Player getOwner();

    PlayerMarketData getData();

    void spawn();

    void close(boolean withSmoke);

    void hideFrom(Player viewer);

    MarketSlotView findByClickTarget(UUID entityId);

    boolean revealSlot(UUID clickTargetId);

    void refreshSlotHologram(UUID clickTargetId);

    boolean isClosed();

    final class MarketSlotView {
        private final MarketItemInstance instance;
        private final MarketItemTemplate template;
        private final UUID clickTargetId;
        private final boolean sealed;

        public MarketSlotView(MarketItemInstance instance,
                              MarketItemTemplate template,
                              UUID clickTargetId,
                              boolean sealed) {
            this.instance = instance;
            this.template = template;
            this.clickTargetId = clickTargetId;
            this.sealed = sealed;
        }

        public MarketItemInstance instance() { return instance; }
        public MarketItemTemplate template() { return template; }
        public UUID clickTargetId() { return clickTargetId; }
        public boolean sealed() { return sealed; }
    }
}
