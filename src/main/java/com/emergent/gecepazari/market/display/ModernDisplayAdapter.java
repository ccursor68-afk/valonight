package com.emergent.gecepazari.market.display;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.compat.ItemMetaCompat;
import com.emergent.gecepazari.compat.ParticleCompat;
import com.emergent.gecepazari.compat.PlatformCompat;
import com.emergent.gecepazari.compat.TextDisplayCompat;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.data.Rarity;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.util.ArcMath;
import com.emergent.gecepazari.util.ColorUtil;
import com.emergent.gecepazari.util.SkullUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
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

/** 1.19.4+ Display entity tabanli pazar goruntuleme. */
public final class ModernDisplayAdapter implements MarketDisplayAdapter {

    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        MONEY_FORMAT = new DecimalFormat("#,##0.##", sym);
    }

    private static final class Slot {
        final ItemDisplay itemDisplay;
        final TextDisplay textDisplay;
        final Interaction interaction;
        final MarketItemInstance instance;
        final MarketItemTemplate template;
        boolean sealed;
        double bobPhase;

        Slot(ItemDisplay itemDisplay, TextDisplay textDisplay, Interaction interaction,
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
    private final LanguageManager lang;
    private final Player owner;
    private final PlayerMarketData data;

    private final List<Slot> slots = new ArrayList<>();
    private final Map<UUID, Slot> clickLookup = new HashMap<>();
    private BukkitRunnable followTask;
    private float anchoredYaw;
    private long tickCounter;
    private boolean closed;

    public ModernDisplayAdapter(GecePazariPlugin plugin,
                                ConfigManager config,
                                LanguageManager lang,
                                Player owner,
                                PlayerMarketData data) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.owner = owner;
        this.data = data;
    }

    @Override
    public Player getOwner() { return owner; }

    @Override
    public PlayerMarketData getData() { return data; }

    @Override
    public void spawn() {
        Location origin = owner.getLocation();
        anchoredYaw = origin.getYaw();
        int count = data.getItems().size();
        if (count == 0) return;

        Location[] points = computeArcPoints(origin);
        for (int i = 0; i < count; i++) {
            MarketItemInstance inst = data.getItems().get(i);
            MarketItemTemplate template = config.getTemplate(inst.getTemplateId());
            if (template == null) continue;
            double phase = (i * Math.PI * 2.0) / Math.max(1, count);
            spawnSlot(points[i], inst, template, phase);
        }

        hideFromOthers();
        playOpenEffect();
        startFollowTask();
    }

    private Location[] computeArcPoints(Location playerLocation) {
        Location anchor = playerLocation.clone();
        anchor.setYaw(anchoredYaw);
        anchor.setPitch(0f);
        return ArcMath.calculateArcPoints(
                anchor, data.getItems().size(),
                config.getRadius(), config.getArcDegrees(),
                config.getHeightOffset() + 1.2
        );
    }

    private void spawnSlot(Location at, MarketItemInstance inst, MarketItemTemplate template, double phase) {
        int td = config.getTeleportDuration();
        Rarity rarity = template.getRarity();
        boolean startSealed = !inst.isRevealed();
        ItemStack startStack = startSealed
                ? SkullUtil.createCustomHead(rarity.getSkullTextureBase64())
                : buildRealItemStack(template, Math.max(1, template.getAmount()));
        float startScale = startSealed ? 1.2f : 0.95f;

        ItemDisplay itemDisplay = at.getWorld().spawn(at, ItemDisplay.class, ed -> {
            ed.setItemStack(startStack);
            ed.setTeleportDuration(td);
            ed.setBillboard(Display.Billboard.CENTER);
            ed.setPersistent(false);
            ed.setInvulnerable(true);
            ed.setGravity(false);
            Transformation t = ed.getTransformation();
            ed.setTransformation(new Transformation(
                    t.getTranslation(), t.getLeftRotation(),
                    new Vector3f(startScale, startScale, startScale), t.getRightRotation()
            ));
        });

        Location holoLoc = at.clone().add(0, 1.25, 0);
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
                    tt.getTranslation(), tt.getLeftRotation(),
                    new Vector3f(1.1f, 1.1f, 1.1f), tt.getRightRotation()
            ));
            TextDisplayCompat.setText(td2, startSealed
                    ? buildSealedHologram(rarity)
                    : buildRevealedHologram(template, inst));
        });

        Interaction interaction = at.getWorld().spawn(at, Interaction.class, ie -> {
            ie.setInteractionWidth(1.4f);
            ie.setInteractionHeight(1.8f);
            ie.setResponsive(true);
            ie.setPersistent(false);
            ie.setInvulnerable(true);
        });

        Slot slot = new Slot(itemDisplay, textDisplay, interaction, inst, template, phase);
        slot.sealed = startSealed;
        slots.add(slot);
        clickLookup.put(interaction.getUniqueId(), slot);
    }

    private ItemStack buildRealItemStack(MarketItemTemplate template, int amount) {
        ItemStack stack = new ItemStack(template.getMaterial(), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            ItemMetaCompat.setDisplayName(meta, template.getDisplayName());
            if (!template.getLore().isEmpty()) {
                ItemMetaCompat.setLore(meta, template.getLore());
            }
            if (template.hasCustomModelData()) {
                meta.setCustomModelData(template.getCustomModelData());
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Component buildSealedHologram(Rarity rarity) {
        String langCode = lang.getLangFor(owner.getUniqueId());
        Component title = ColorUtil.component("&f&l" + lang.getRaw(langCode, "sealed-mystery"));
        String rarityName = lang.getRarityName(langCode, rarity);
        Component rarityLine = Component.text(rarityName).color(rarity.getColor())
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true);
        Component hintLine = ColorUtil.component(lang.getRaw(langCode, "sealed-hint"));
        return title.append(Component.newline()).append(rarityLine).append(Component.newline()).append(hintLine);
    }

    private Component buildRevealedHologram(MarketItemTemplate template, MarketItemInstance inst) {
        String langCode = lang.getLangFor(owner.getUniqueId());
        String stockLabel = lang.getRaw(langCode, "stock-label");
        String soldOutTag = lang.getRaw(langCode, "sold-out-tag");
        int amount = Math.max(1, template.getAmount());

        Component nameLine = ColorUtil.component(template.getDisplayName())
                .append(amount > 1 ? ColorUtil.component(" &7&l×" + amount) : Component.empty());
        Rarity r = template.getRarity();
        String rarityName = lang.getRarityName(langCode, r);
        Component rarityBadge = Component.text("[" + rarityName + "]")
                .color(r.getColor())
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true);

        Component body = nameLine.append(Component.newline()).append(rarityBadge);
        if (!template.getLore().isEmpty()) {
            for (String line : template.getLore()) {
                body = body.append(Component.newline()).append(ColorUtil.component(line));
            }
        }
        body = body.append(Component.newline()).append(ColorUtil.component("&8&m            "));

        if (inst.isSoldOut()) {
            body = body.append(Component.newline()).append(ColorUtil.component(soldOutTag));
        } else {
            body = body.append(Component.newline()).append(ColorUtil.component(
                    "&a&l-" + inst.getDiscountPercent() + "%  &8|  &e&l"
                            + MONEY_FORMAT.format(inst.getFinalPrice()) + "$"));
        }
        if (inst.isSoldOut()) {
            body = body.append(Component.newline()).append(ColorUtil.component(
                    "&7" + stockLabel + ": &c0&8/&7" + inst.getInitialStock()));
        } else {
            body = body.append(Component.newline()).append(ColorUtil.component(
                    "&7" + stockLabel + ": &f" + inst.getRemainingStock()
                            + "&8/&7" + inst.getInitialStock()));
        }
        return body;
    }

    private void playOpenEffect() {
        Location loc = owner.getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.4f);
        loc.getWorld().playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 0.7f, 1.2f);
    }

    private void hideFromOthers() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(owner.getUniqueId())) continue;
            hideFrom(online);
        }
    }

    @Override
    public void hideFrom(Player viewer) {
        for (Slot s : slots) {
            PlatformCompat.hideEntity(viewer, plugin, s.itemDisplay);
            PlatformCompat.hideEntity(viewer, plugin, s.textDisplay);
            PlatformCompat.hideEntity(viewer, plugin, s.interaction);
        }
    }

    private void startFollowTask() {
        int interval = Math.max(1, config.getUpdateIntervalTicks());
        double bobAmp = config.getBobAmplitude();
        double bobSpeed = config.getBobSpeed();

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
                    Slot s = slots.get(i);
                    Location target = pts[i].clone();
                    target.setYaw(0f);
                    target.setPitch(0f);
                    double amp = s.sealed ? bobAmp : bobAmp * 0.4;
                    target.add(0, Math.sin(timeSec * bobSpeed + s.bobPhase) * amp, 0);
                    s.itemDisplay.teleport(target);
                    s.textDisplay.teleport(target.clone().add(0, 1.25, 0));
                    s.interaction.teleport(target);
                }
            }
        };
        followTask.runTaskTimer(plugin, 0L, interval);
    }

    @Override
    public MarketSlotView findByClickTarget(UUID entityId) {
        Slot s = clickLookup.get(entityId);
        if (s == null) return null;
        return new MarketSlotView(s.instance, s.template, entityId, s.sealed);
    }

    @Override
    public boolean revealSlot(UUID clickTargetId) {
        Slot s = clickLookup.get(clickTargetId);
        if (s == null || !s.sealed) return false;

        s.sealed = false;
        s.instance.setRevealed(true);

        Location loc = s.itemDisplay.getLocation();
        ParticleCompat.revealBurst(loc);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        try {
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        } catch (NoSuchFieldError ignored) {
        }

        ItemStack realStack = buildRealItemStack(s.template, Math.max(1, s.template.getAmount()));
        s.itemDisplay.setItemStack(realStack);

        Transformation t = s.itemDisplay.getTransformation();
        s.itemDisplay.setInterpolationDelay(0);
        s.itemDisplay.setInterpolationDuration(8);
        s.itemDisplay.setTransformation(new Transformation(
                t.getTranslation(), t.getLeftRotation(),
                new Vector3f(0.95f, 0.95f, 0.95f), t.getRightRotation()
        ));
        TextDisplayCompat.setText(s.textDisplay, buildRevealedHologram(s.template, s.instance));
        return true;
    }

    @Override
    public void refreshSlotHologram(UUID clickTargetId) {
        Slot s = clickLookup.get(clickTargetId);
        if (s == null) return;
        if (s.sealed) {
            TextDisplayCompat.setText(s.textDisplay, buildSealedHologram(s.template.getRarity()));
        } else {
            TextDisplayCompat.setText(s.textDisplay, buildRevealedHologram(s.template, s.instance));
            if (s.instance.isSoldOut()) {
                Transformation t = s.itemDisplay.getTransformation();
                s.itemDisplay.setInterpolationDelay(0);
                s.itemDisplay.setInterpolationDuration(6);
                s.itemDisplay.setTransformation(new Transformation(
                        t.getTranslation(), t.getLeftRotation(),
                        new Vector3f(0.7f, 0.7f, 0.7f), t.getRightRotation()
                ));
            }
        }
    }

    @Override
    public void close(boolean withSmoke) {
        if (closed) return;
        closed = true;
        if (followTask != null) {
            try { followTask.cancel(); } catch (IllegalStateException ignored) {}
            followTask = null;
        }
        for (Slot s : slots) {
            if (withSmoke) {
                ParticleCompat.smokeClose(s.itemDisplay.getLocation());
            }
            s.itemDisplay.remove();
            s.textDisplay.remove();
            s.interaction.remove();
        }
        slots.clear();
        clickLookup.clear();
    }

    @Override
    public boolean isClosed() { return closed; }
}
