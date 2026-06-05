package com.emergent.gecepazari.market;

import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerMarketData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Esya havuzundan agirlikli rastgele 5 esya secer, indirim uygular ve oyuncunun pazar verisini olusturur.
 */
public final class MarketRoller {

    private static final int DESIRED_ITEM_COUNT = 5;

    private final ConfigManager config;
    private final Random random = new Random();

    public MarketRoller(ConfigManager config) {
        this.config = config;
    }

    public PlayerMarketData roll(UUID playerId) {
        List<MarketItemTemplate> pool = config.getItemPoolList();
        if (pool.isEmpty()) {
            return new PlayerMarketData(playerId, new ArrayList<>(), System.currentTimeMillis());
        }

        int count = Math.min(DESIRED_ITEM_COUNT, pool.size());
        Set<String> picked = new HashSet<>();
        List<MarketItemInstance> selected = new ArrayList<>(count);

        int safety = 0;
        while (selected.size() < count && safety < 200) {
            safety++;
            MarketItemTemplate t = weightedPick(pool);
            if (t == null) break;
            if (picked.contains(t.getId())) continue;
            picked.add(t.getId());

            int discount = randomBetween(t.getMinDiscount(), t.getMaxDiscount());
            double finalPrice = Math.max(1.0, Math.round(t.getBasePrice() * (1.0 - discount / 100.0)));
            int stock = randomBetween(1, t.getMaxStock());

            selected.add(new MarketItemInstance(t.getId(), discount, finalPrice, stock, stock, false));
        }

        return new PlayerMarketData(playerId, selected, System.currentTimeMillis());
    }

    private MarketItemTemplate weightedPick(List<MarketItemTemplate> pool) {
        int totalWeight = 0;
        for (MarketItemTemplate t : pool) totalWeight += Math.max(0, t.getChanceWeight());
        if (totalWeight <= 0) return pool.get(random.nextInt(pool.size()));

        int r = random.nextInt(totalWeight);
        int acc = 0;
        for (MarketItemTemplate t : pool) {
            acc += Math.max(0, t.getChanceWeight());
            if (r < acc) return t;
        }
        return pool.get(pool.size() - 1);
    }

    private int randomBetween(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }
}
