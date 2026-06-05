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
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bir oyuncuya ait fiziksel pazar. ItemDisplay + TextDisplay + Interaction entity'leri
 * yarim ay seklinde olusturur, oyuncuyu pursuzsuzce takip eder ve sadece o oyuncuya gozukur.
 */
public final class PlayerMarket {

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
    private float spinDegrees = 0f;
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

    /** Pazari dunyada spawn'lar. */
    public void spawn() {
        Location origin = owner.getLocation();
        int count = data.getItems().size();
        if (count == 0) return;

        Location[] points = ArcMath.calculateArcPoints(
                origin, count,
                config.getRadius(),
                config.getArcDegrees(),
                config.getHeightOffset() + 1.2 // oyuncunun goz hizasina yakin
        );

        for (int i = 0; i < count; i++) {
            MarketItemInstance inst = data.getItems().get(i);
            MarketItemTemplate template = config.getTemplate(inst.getTemplateId());
            if (template == null) continue;
            spawnSlot(points[i], inst, template);
        }

        hideFromOthers();
        startFollowTask();
    }

    private void spawnSlot(Location at, MarketItemInstance inst, MarketItemTemplate template) {
        int td = config.getTeleportDuration();

        // ItemDisplay
        ItemDisplay itemDisplay = at.getWorld().spawn(at, ItemDisplay.class, ed -> {
            ed.setItemStack(buildItemStack(template));
            ed.setTeleportDuration(td);
            ed.setBillboard(Display.Billboard.FIXED);
            ed.setPersistent(false);
            ed.setInvulnerable(true);
            ed.setGravity(false);
            // Hafif buyut
            Transformation t = ed.getTransformation();
            ed.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(0.9f, 0.9f, 0.9f),
                    t.getRightRotation()
            ));
        });

        // TextDisplay (hologram) - esya uzerinde
        Location holoLoc = at.clone().add(0, 0.85, 0);
        TextDisplay textDisplay = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, td2 -> {
            td2.setTeleportDuration(td);
            td2.setBillboard(Display.Billboard.CENTER); // oyuncuya doner
            td2.setBackgroundColor(Color.fromARGB(160, 0, 0, 0));
            td2.setSeeThrough(true);
            td2.setPersistent(false);
            td2.setInvulnerable(true);
            td2.text(buildHologramText(template, inst));
        });

        // Interaction entity - tiklamayi yakalar
        Interaction interaction = at.getWorld().spawn(at, Interaction.class, ie -> {
            ie.setInteractionWidth(1.2f);
            ie.setInteractionHeight(1.2f);
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

    private Component buildHologramText(MarketItemTemplate template, MarketItemInstance inst) {
        Component name = ColorUtil.component(template.getDisplayName());
        Component priceLine;
        if (inst.isSoldOut()) {
            priceLine = ColorUtil.component("&c&l[TUKENDI]");
        } else {
            priceLine = ColorUtil.component("&e-%" + inst.getDiscountPercent()
                    + " &7| &6" + formatMoney(inst.getFinalPrice()) + "$");
        }
        Component stockLine = ColorUtil.component("&7Stok: &f" + inst.getRemainingStock());

        return name
                .append(Component.newline())
                .append(priceLine)
                .append(Component.newline())
                .append(stockLine);
    }

    private String formatMoney(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    /** Slot satin alma sonrasi hologramini gunceller. */
    public void refreshSlotHologram(UUID interactionId) {
        MarketSlot s = interactionLookup.get(interactionId);
        if (s == null) return;
        s.textDisplay.text(buildHologramText(s.template, s.instance));
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
        final double rotPerTick = config.getRotationSpeed() / 20.0;

        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (closed || !owner.isOnline()) {
                    cancel();
                    return;
                }
                spinDegrees = (float) ((spinDegrees + rotPerTick * interval) % 360);

                Location origin = owner.getLocation();
                int n = slots.size();
                Location[] pts = ArcMath.calculateArcPoints(
                        origin, n,
                        config.getRadius(),
                        config.getArcDegrees(),
                        config.getHeightOffset() + 1.2
                );

                for (int i = 0; i < n; i++) {
                    MarketSlot s = slots.get(i);
                    Location target = pts[i];

                    // Esyalar oyuncuya doner
                    float yaw = ArcMath.yawTowards(target, origin);
                    target.setYaw(yaw);

                    // ItemDisplay'i pozisyona pururzssuz teleport et
                    s.itemDisplay.teleport(target);

                    // Donme efekti icin transformation rotasyonu uygula
                    applySpin(s.itemDisplay);

                    // Hologram hafif yukarida
                    Location holoLoc = target.clone().add(0, 0.85, 0);
                    s.textDisplay.teleport(holoLoc);

                    // Interaction tiklama icin
                    s.interaction.teleport(target);
                }
            }
        };
        followTask.runTaskTimer(plugin, 0L, interval);
    }

    private void applySpin(ItemDisplay display) {
        Transformation t = display.getTransformation();
        float rad = (float) Math.toRadians(spinDegrees);
        Quaternionf left = new Quaternionf(new AxisAngle4f(rad, 0f, 1f, 0f));
        Transformation nt = new Transformation(
                t.getTranslation(),
                left,
                t.getScale(),
                t.getRightRotation()
        );
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(1, config.getUpdateIntervalTicks()));
        display.setTransformation(nt);
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
