package com.emergent.gecepazari.market;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.util.ArcMath;
import com.emergent.gecepazari.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Bir oyuncuya ait fiziksel pazar. ItemDisplay + TextDisplay + Interaction entity'leri
 * yarim ay seklinde olusturur, oyuncuyu pururzssuzce takip eder ve sadece o oyuncuya gozukur.
 *
 * <p>Onemli not: Yarim ay'in yonelimi (yaw) spawn aninda KILITLENIR. Boylece oyuncu kafasini
 * cevirdiginde esyalar kaymaz; sadece oyuncunun konumu degisirse (yurudukce) takip eder.</p>
 */
public final class PlayerMarket {

    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0.##", sym);
    }

    /** Bir slottaki tum entity'leri tutar. */
    private static final class MarketSlot {
        final ItemDisplay itemDisplay;
        final TextDisplay textDisplay;
        final Interaction interaction;
        final MarketItemInstance instance;
        final MarketItemTemplate template;

        MarketSlot(ItemDisplay itemDisplay, TextDisplay textDisplay, Interaction interaction,
                   MarketItemInstance instance, MarketItemTemplate template) {
            this.itemDisplay = itemDisplay;
            this.textDisplay = textDisplay;
            this.interaction = interaction;
            this.instance = instance;
            this.template = template;
        }
    }

    private final GecePazariPlugin plugin;
    private final ConfigManager config;
    private final Player owner;
    private final PlayerMarketData data;

    private final List<MarketSlot> slots = new ArrayList<>();
    private final Map<UUID, MarketSlot> interactionLookup = new HashMap<>();
    private BukkitRunnable followTask;
    private float anchoredYaw;
    private boolean closed = false;

    public PlayerMarket(GecePazariPlugin plugin,
                        ConfigManager config,
                        Player owner,
                        PlayerMarketData data) {
        this.plugin = plugin;
        this.config = config;
        this.owner = owner;
        this.data = data;
    }

    public Player getOwner() { return owner; }

    public PlayerMarketData getData() { return data; }

    public MarketSlotView findByInteraction(UUID interactionId) {
        MarketSlot s = interactionLookup.get(interactionId);
        return s == null ? null : new MarketSlotView(s);
    }

    /** Pazari dunyada spawn'lar. Yaw, spawn aninda kilitlenir. */
    public void spawn() {
        Location origin = owner.getLocation();
        this.anchoredYaw = origin.getYaw();

        int count = data.getItems().size();
        if (count == 0) return;

        Location[] points = computeArcPoints(origin);

        for (int i = 0; i < count; i++) {
            MarketItemInstance inst = data.getItems().get(i);
            MarketItemTemplate template = config.getTemplate(inst.getTemplateId());
            if (template == null) continue;
            spawnSlot(points[i], inst, template);
        }

        hideFromOthers();
        startFollowTask();
    }

    /** Anchored yaw kullanarak yarim ay noktalarini hesaplar. */
    private Location[] computeArcPoints(Location playerLocation) {
        // Yaw'i kilitli yaw ile zorla. Bu sayede kafa cevirme arc'i dondurmez.
        Location anchor = playerLocation.clone();
        anchor.setYaw(anchoredYaw);
        anchor.setPitch(0f); // pitch de etkilemesin

        int count = data.getItems().size();
        return ArcMath.calculateArcPoints(
                anchor, count,
                config.getRadius(),
                config.getArcDegrees(),
                config.getHeightOffset() + 1.2
        );
    }

    private void spawnSlot(Location at, MarketItemInstance inst, MarketItemTemplate template) {
        int td = config.getTeleportDuration();

        // ItemDisplay - Billboard.CENTER ile her zaman kameraya doner
        ItemDisplay itemDisplay = at.getWorld().spawn(at, ItemDisplay.class, ed -> {
            ed.setItemStack(buildItemStack(template));
            ed.setTeleportDuration(td);
            ed.setBillboard(Display.Billboard.CENTER);
            ed.setPersistent(false);
            ed.setInvulnerable(true);
            ed.setGravity(false);
            // Boyut: tukenmis ise hafifce kucult
            float scale = inst.isSoldOut() ? 0.7f : 0.95f;
            Transformation t = ed.getTransformation();
            ed.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(scale, scale, scale),
                    t.getRightRotation()
            ));
        });

        // TextDisplay (hologram) - esya uzerinde
        Location holoLoc = at.clone().add(0, 0.95, 0);
        TextDisplay textDisplay = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, td2 -> {
            td2.setTeleportDuration(td);
            td2.setBillboard(Display.Billboard.CENTER);
            td2.setBackgroundColor(Color.fromARGB(180, 12, 12, 18));
            td2.setSeeThrough(false);
            td2.setPersistent(false);
            td2.setInvulnerable(true);
            td2.setShadowed(true);
            td2.setAlignment(TextDisplay.TextAlignment.CENTER);
            td2.setDefaultBackground(false);
            td2.setLineWidth(220);
            // Hafif buyut
            Transformation tt = td2.getTransformation();
            td2.setTransformation(new Transformation(
                    tt.getTranslation(),
                    tt.getLeftRotation(),
                    new Vector3f(1.05f, 1.05f, 1.05f),
                    tt.getRightRotation()
            ));
            td2.text(buildHologramText(template, inst));
        });

        // Interaction entity - tiklamayi yakalar
        Interaction interaction = at.getWorld().spawn(at, Interaction.class, ie -> {
            ie.setInteractionWidth(1.3f);
            ie.setInteractionHeight(1.6f);
            ie.setResponsive(true);
            ie.setPersistent(false);
            ie.setInvulnerable(true);
        });

        MarketSlot slot = new MarketSlot(itemDisplay, textDisplay, interaction, inst, template);
        slots.add(slot);
        interactionLookup.put(interaction.getUniqueId(), slot);
    }

    private ItemStack buildItemStack(MarketItemTemplate template) {
        ItemStack stack = new ItemStack(template.getMaterial(), 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(template.getDisplayName()));
            if (!template.getLore().isEmpty()) {
                meta.lore(ColorUtil.components(template.getLore()));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Hologram tasarimi (3 satir, temiz hiyerarsi):
     *   1: &b&lEsya Adi &7×N           (ad + adet)
     *   2: &a&l-39%  &8|  &6&l1.525$   (indirim + nihai fiyat)
     *   3: &7Stok: &f2&7/&f3           (kalan / baslangic)
     */
    private Component buildHologramText(MarketItemTemplate template, MarketItemInstance inst) {
        String coloredName = template.getDisplayName();
        int amount = Math.max(1, template.getAmount());

        // 1. satir - ad + adet
        Component nameLine = ColorUtil.component(coloredName)
                .append(amount > 1
                        ? ColorUtil.component(" &7&l×" + amount)
                        : Component.empty());

        // 2. satir - fiyat / indirim ya da TUKENDI
        Component priceLine;
        if (inst.isSoldOut()) {
            priceLine = ColorUtil.component("&c&lTUKENDI");
        } else {
            priceLine = ColorUtil.component(
                    "&a&l-" + inst.getDiscountPercent() + "%"
                    + "  &8|  "
                    + "&e&l" + formatMoney(inst.getFinalPrice()) + "$"
            );
        }

        // 3. satir - stok x/y
        Component stockLine;
        if (inst.isSoldOut()) {
            stockLine = ColorUtil.component("&7Stok: &c0&8/&7" + inst.getInitialStock());
        } else {
            stockLine = ColorUtil.component(
                    "&7Stok: &f" + inst.getRemainingStock() + "&8/&7" + inst.getInitialStock()
            );
        }

        return nameLine
                .append(Component.newline())
                .append(priceLine)
                .append(Component.newline())
                .append(stockLine);
    }

    private String formatMoney(double v) {
        return MONEY_FORMAT.format(v);
    }

    /** Slot satin alma sonrasi hologramini ve item scale'ini gunceller. */
    public void refreshSlotHologram(UUID interactionId) {
        MarketSlot s = interactionLookup.get(interactionId);
        if (s == null) return;
        s.textDisplay.text(buildHologramText(s.template, s.instance));

        // Tukendiyse item display'i ufalt
        if (s.instance.isSoldOut()) {
            Transformation t = s.itemDisplay.getTransformation();
            s.itemDisplay.setInterpolationDelay(0);
            s.itemDisplay.setInterpolationDuration(6);
            s.itemDisplay.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(0.7f, 0.7f, 0.7f),
                    t.getRightRotation()
            ));
        }
    }

    /** Bu pazara ait tum entity'leri yeni katilan oyuncudan da gizler. */
    public void hideFrom(Player viewer) {
        for (MarketSlot s : slots) {
            viewer.hideEntity(plugin, s.itemDisplay);
            viewer.hideEntity(plugin, s.textDisplay);
            viewer.hideEntity(plugin, s.interaction);
        }
    }

    private void hideFromOthers() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(owner.getUniqueId())) continue;
            hideFrom(online);
        }
    }

    private void startFollowTask() {
        final int interval = Math.max(1, config.getUpdateIntervalTicks());

        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (closed || !owner.isOnline()) {
                    cancel();
                    return;
                }

                Location[] pts = computeArcPoints(owner.getLocation());

                for (int i = 0; i < slots.size(); i++) {
                    MarketSlot s = slots.get(i);
                    Location target = pts[i];
                    // Yaw: Billboard.CENTER sayesinde gorsel rotasyon otomatik;
                    // entity'nin kendi yaw'i onemli degil, sifirla.
                    target.setYaw(0f);
                    target.setPitch(0f);

                    s.itemDisplay.teleport(target);

                    Location holoLoc = target.clone().add(0, 0.95, 0);
                    s.textDisplay.teleport(holoLoc);

                    s.interaction.teleport(target);
                }
            }
        };
        followTask.runTaskTimer(plugin, 0L, interval);
    }

    /**
     * Pazari kapatir. {@code withSmoke=true} ise her entity'nin lokasyonunda duman efekti gosterilir.
     */
    public void close(boolean withSmoke) {
        if (closed) return;
        closed = true;

        if (followTask != null) {
            try { followTask.cancel(); } catch (IllegalStateException ignored) {}
            followTask = null;
        }

        for (MarketSlot s : slots) {
            if (withSmoke) {
                Location loc = s.itemDisplay.getLocation();
                loc.getWorld().spawnParticle(Particle.SMOKE, loc, 24, 0.3, 0.4, 0.3, 0.02);
                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 8, 0.2, 0.2, 0.2, 0.0);
            }
            s.itemDisplay.remove();
            s.textDisplay.remove();
            s.interaction.remove();
        }
        slots.clear();
        interactionLookup.clear();
    }

    public boolean isClosed() { return closed; }

    /** Disari acilan slot bilgisi. Listener'lar bu view uzerinden eslesir. */
    public static final class MarketSlotView {
        private final MarketSlot slot;

        private MarketSlotView(MarketSlot slot) { this.slot = slot; }

        public MarketItemInstance instance() { return slot.instance; }
        public MarketItemTemplate template() { return slot.template; }
        public UUID interactionId() { return slot.interaction.getUniqueId(); }
    }
}
