package com.emergent.gecepazari.market;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.market.display.DisplayAdapterFactory;
import com.emergent.gecepazari.market.display.MarketDisplayAdapter;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Bir oyuncuya ait fiziksel pazar.
 * Gorsel backend surume gore Display entity (1.19.4+) veya ArmorStand (1.18-1.19.3) kullanir.
 */
public final class PlayerMarket {

    private final MarketDisplayAdapter adapter;

    public PlayerMarket(GecePazariPlugin plugin,
                        ConfigManager config,
                        LanguageManager lang,
                        Player owner,
                        PlayerMarketData data) {
        this.adapter = DisplayAdapterFactory.create(plugin, config, lang, owner, data);
    }

    public Player getOwner() { return adapter.getOwner(); }

    public PlayerMarketData getData() { return adapter.getData(); }

    public MarketSlotView findByInteraction(UUID clickTargetId) {
        MarketDisplayAdapter.MarketSlotView view = adapter.findByClickTarget(clickTargetId);
        return view == null ? null : new MarketSlotView(view);
    }

    public void spawn() { adapter.spawn(); }

    public boolean revealSlot(UUID clickTargetId) { return adapter.revealSlot(clickTargetId); }

    public void refreshSlotHologram(UUID clickTargetId) { adapter.refreshSlotHologram(clickTargetId); }

    public void hideFrom(org.bukkit.entity.Player viewer) { adapter.hideFrom(viewer); }

    public void close(boolean withSmoke) { adapter.close(withSmoke); }

    public boolean isClosed() { return adapter.isClosed(); }

    public static final class MarketSlotView {
        private final MarketDisplayAdapter.MarketSlotView view;

        MarketSlotView(MarketDisplayAdapter.MarketSlotView view) {
            this.view = view;
        }

        public com.emergent.gecepazari.data.MarketItemInstance instance() { return view.instance(); }
        public com.emergent.gecepazari.data.MarketItemTemplate template() { return view.template(); }
        public UUID interactionId() { return view.clickTargetId(); }
        public boolean sealed() { return view.sealed(); }
    }
}
