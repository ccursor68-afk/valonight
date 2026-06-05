package com.emergent.gecepazari.market;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.data.Rarity;
import com.emergent.gecepazari.util.ArcMath;
import com.emergent.gecepazari.util.ColorUtil;
import com.emergent.gecepazari.util.SkullUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
 * Bir oyuncuya ait fiziksel pazar.
 * Iki asamali animasyon:
 *   1. SEALED (kapali): nadirlik bazli custom oyuncu kafasi belirir, oyuncunun etrafinda bob yapar.
 *   2. REVEALED (acik): oyuncu sag tikladiginda kafa patlama efektiyle dagilir, gercek esya ortaya cikar.
 *      Bir kez daha sag tiklayinca satin alma denemesi yapilir.
 *
 * Yarim ay yonelimi (yaw) spawn aninda kilitlenir; kafa cevirmek arc'i dondurmez.
 */
public final class PlayerMarket {

    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0.##", sym);
    }

    /** Bir slottaki tum entity'leri ve durumu tutar. */
    private static final class MarketSlot {
        final ItemDisplay itemDisplay;
        final TextDisplay textDisplay;
        final Interaction interaction;
        final MarketItemInstance instance;
        final MarketItemTemplate template;
        boolean sealed = true;
        double bobPhase;

        MarketSlot(ItemDisplay itemDisplay, TextDisplay textDisplay, Interaction interaction,
                   MarketItemInstance instance, MarketItemTemplate template, double bobPhase) {
            this.itemDisplay = itemDisplay;
            this.textDisplay = textDisplay;
            this.interaction = interaction;
            this.instance = instance;
            this.template = template;
            this.bobPhase = bobPhase;
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
    private long tickCounter = 0;
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

    /** Pazari dunyada spawn'lar. Yaw spawn aninda kilitlenir. */
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
            // Faz: slotlar farkli zamanlarda hareket etsin
            double phase = (i * Math.PI * 2.0) / Math.max(1, count);
            spawnSlot(points[i], inst, template, phase);
        }

        hideFromOthers();
        playOpenEffect();
        startFollowTask();
    }

    private void playOpenEffect() {
        Location loc = owner.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        loc.getWorld().playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 0.7f, 1.2f);
    }

    /** Anchored yaw kullanarak yarim ay noktalarini hesaplar. */
    private Location[] computeArcPoints(Location playerLocation) {
        Location anchor = playerLocation.clone();
        anchor.setYaw(anchoredYaw);
        anchor.setPitch(0f);

        int count = data.getItems().size();
        return ArcMath.calculateArcPoints(
                anchor, count,
                config.getRadius(),
                config.getArcDegrees(),
                config.getHeightOffset() + 1.2
        );
    }

    private void spawnSlot(Location at, MarketItemInstance inst, MarketItemTemplate template, double phase) {
        int td = config.getTeleportDuration();
        Rarity rarity = template.getRarity();

        // ItemDisplay: BASLANGICTA RARITY KAFASI
        ItemStack sealedStack = SkullUtil.createCustomHead(rarity.getSkullTextureBase64());

        ItemDisplay itemDisplay = at.getWorld().spawn(at, ItemDisplay.class, ed -> {
            ed.setItemStack(sealedStack);
            ed.setTeleportDuration(td);
            ed.setBillboard(Display.Billboard.CENTER);
            ed.setPersistent(false);
            ed.setInvulnerable(true);
            ed.setGravity(false);
            // Kafalar buyuk gozuksun
            Transformation t = ed.getTransformation();
            ed.setTransformation(new Transformation(
                    t.getTranslation(),
                    t.getLeftRotation(),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    t.getRightRotation()
            ));
        });

        // TextDisplay (hologram) - kafanin uzerinde
        Location holoLoc = at.clone().add(0, 1.05, 0);
        TextDisplay textDisplay = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, td2 -> {
            td2.setTeleportDuration(td);
            td2.setBillboard(Display.Billboard.CENTER);
            td2.setBackgroundColor(Color.fromARGB(190, 8, 8, 14));
            td2.setSeeThrough(false);
            td2.setPersistent(false);
            td2.setInvulnerable(true);
            td2.setShadowed(true);
            td2.setAlignment(TextDisplay.TextAlignment.CENTER);
            td2.setDefaultBackground(false);
            td2.setLineWidth(260);
            Transformation tt = td2.getTransformation();
            td2.setTransformation(new Transformation(
                    tt.getTranslation(),
                    tt.getLeftRotation(),
                    new Vector3f(1.1f, 1.1f, 1.1f),
                    tt.getRightRotation()
            ));
            // sealed metni
            td2.text(buildSealedHologram(rarity));
        });

        // Interaction entity
        Interaction interaction = at.getWorld().spawn(at, Interaction.class, ie -> {
            ie.setInteractionWidth(1.4f);
            ie.setInteractionHeight(1.8f);
            ie.setResponsive(true);
            ie.setPersistent(false);
            ie.setInvulnerable(true);
        });

        MarketSlot slot = new MarketSlot(itemDisplay, textDisplay, interaction, inst, template, phase);
        slots.add(slot);
        interactionLookup.put(interaction.getUniqueId(), slot);
    }

    /** Gercek esya ItemStack'ini olusturur (lore + custom model data uygulanmis). */
    private ItemStack buildRealItemStack(MarketItemTemplate template, int amount) {
        ItemStack stack = new ItemStack(template.getMaterial(), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.component(template.getDisplayName()));
            if (!template.getLore().isEmpty()) {
                meta.lore(ColorUtil.components(template.getLore()));
            }
            if (template.hasCustomModelData()) {
                meta.setCustomModelData(template.getCustomModelData());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * Sealed (kapali) durumdaki hologram. Sadece nadirlik + "Ac" yazar.
     */
    private Component buildSealedHologram(Rarity rarity) {
        Component title = Component.text("? ? ?").color(net.kyori.adventure.text.format.NamedTextColor.WHITE)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true);
        Component rarityLine = Component.text(rarity.getDisplayName()).color(rarity.getColor())
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true);
        Component hint = ColorUtil.component("&7&oSag tik ile ac");

        return title
                .append(Component.newline())
                .append(rarityLine)
                .append(Component.newline())
                .append(hint);
    }

    /**
     * Acik (revealed) hologram. Esya adi + lore + indirim/fiyat + stok.
     */
    private Component buildRevealedHologram(MarketItemTemplate template, MarketItemInstance inst) {
        int amount = Math.max(1, template.getAmount());

        Component nameLine = ColorUtil.component(template.getDisplayName())
                .append(amount > 1
                        ? ColorUtil.component(" &7&l×" + amount)
                        : Component.empty());

        // Rarity rozeti (kucuk)
        Rarity r = template.getRarity();
        Component rarityBadge = Component.text("[" + r.getDisplayName() + "]")
                .color(r.getColor())
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true);

        Component body = nameLine
                .append(Component.newline())
                .append(rarityBadge);

        // Lore satirlari (varsa)
        if (!template.getLore().isEmpty()) {
            for (String line : template.getLore()) {
                body = body.append(Component.newline()).append(ColorUtil.component(line));
            }
        }

        // Bos satir + fiyat
        body = body.append(Component.newline()).append(ColorUtil.component("&8&m            "));

        if (inst.isSoldOut()) {
            body = body.append(Component.newline())
                    .append(ColorUtil.component("&c&lTUKENDI"));
        } else {
            body = body.append(Component.newline())
                    .append(ColorUtil.component(
                            "&a&l-" + inst.getDiscountPercent() + "%"
                            + "  &8|  "
                            + "&e&l" + formatMoney(inst.getFinalPrice()) + "$"
                    ));
        }

        // Stok
        if (inst.isSoldOut()) {
            body = body.append(Component.newline())
                    .append(ColorUtil.component("&7Stok: &c0&8/&7" + inst.getInitialStock()));
        } else {
            body = body.append(Component.newline())
                    .append(ColorUtil.component(
                            "&7Stok: &f" + inst.getRemainingStock() + "&8/&7" + inst.getInitialStock()
                    ));
        }

        return body;
    }

    private String formatMoney(double v) {
        return MONEY_FORMAT.format(v);
    }

    /**
     * Sealed kafayi patlatip gercek esyayi ortaya cikarir.
     * @return true ise reveal gerceklesti, false ise zaten revealed.
     */
    public boolean revealSlot(UUID interactionId) {
        MarketSlot s = interactionLookup.get(interactionId);
        if (s == null || !s.sealed) return false;

        s.sealed = false;

        Location loc = s.itemDisplay.getLocation();
        // Patlama efekti
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 2);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        // Esyayi gercege cevir
        int displayAmount = Math.max(1, s.template.getAmount());
        ItemStack realStack = buildRealItemStack(s.template, displayAmount);
        s.itemDisplay.setItemStack(realStack);

        // Boyutu hafifce ufalt (kafa cok buyuk gozukuyordu)
        Transformation t = s.itemDisplay.getTransformation();
        s.itemDisplay.setInterpolationDelay(0);
        s.itemDisplay.setInterpolationDuration(8);
        s.itemDisplay.setTransformation(new Transformation(
                t.getTranslation(),
                t.getLeftRotation(),
                new Vector3f(0.95f, 0.95f, 0.95f),
                t.getRightRotation()
        ));

        // Hologram'i guncelle
        s.textDisplay.text(buildRevealedHologram(s.template, s.instance));

        return true;
    }

    /** Slot satin alma sonrasi hologramini ve item scale'ini gunceller. */
    public void refreshSlotHologram(UUID interactionId) {
        MarketSlot s = interactionLookup.get(interactionId);
        if (s == null) return;
        if (s.sealed) {
            s.textDisplay.text(buildSealedHologram(s.template.getRarity()));
        } else {
            s.textDisplay.text(buildRevealedHologram(s.template, s.instance));
        }

        if (s.instance.isSoldOut() && !s.sealed) {
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
        final double bobAmp = config.getBobAmplitude();
        final double bobSpeed = config.getBobSpeed();

        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (closed || !owner.isOnline()) {
                    cancel();
                    return;
                }

                tickCounter += interval;
                double timeSec = tickCounter / 20.0;

                Location[] pts = computeArcPoints(owner.getLocation());

                for (int i = 0; i < slots.size(); i++) {
                    MarketSlot s = slots.get(i);
                    Location target = pts[i].clone();
                    target.setYaw(0f);
                    target.setPitch(0f);

                    // Bob efekti (sealed olanlar daha cok zipliyor)
                    double amp = s.sealed ? bobAmp : bobAmp * 0.4;
                    double yOffset = Math.sin(timeSec * bobSpeed + s.bobPhase) * amp;
                    target.add(0, yOffset, 0);

                    s.itemDisplay.teleport(target);

                    Location holoLoc = target.clone().add(0, 1.05, 0);
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
        public boolean sealed() { return slot.sealed; }
    }
}
