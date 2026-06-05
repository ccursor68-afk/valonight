package com.emergent.gecepazari.market.display;

import com.emergent.gecepazari.GecePazariPlugin;
import com.emergent.gecepazari.compat.ItemMetaCompat;
import com.emergent.gecepazari.compat.ParticleCompat;
import com.emergent.gecepazari.compat.PlatformCompat;
import com.emergent.gecepazari.config.ConfigManager;
import com.emergent.gecepazari.data.MarketItemInstance;
import com.emergent.gecepazari.data.MarketItemTemplate;
import com.emergent.gecepazari.data.PlayerMarketData;
import com.emergent.gecepazari.lang.LanguageManager;
import com.emergent.gecepazari.util.ArcMath;
import com.emergent.gecepazari.util.SkullUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 1.18 - 1.19.3 icin ArmorStand tabanli pazar goruntuleme. */
public final class LegacyArmorStandAdapter implements MarketDisplayAdapter {

    private static final class Slot {
        final ArmorStand itemStand;
        final ArmorStand holoStand;
        final MarketItemInstance instance;
        final MarketItemTemplate template;
        boolean sealed;
        double bobPhase;

        Slot(ArmorStand itemStand, ArmorStand holoStand,
             MarketItemInstance instance, MarketItemTemplate template, double bobPhase) {
            this.itemStand = itemStand;
            this.holoStand = holoStand;
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

    public LegacyArmorStandAdapter(GecePazariPlugin plugin,
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
        boolean startSealed = !inst.isRevealed();
        ItemStack stack = startSealed
                ? SkullUtil.createCustomHead(template.getRarity().getSkullTextureBase64())
                : buildRealItemStack(template, Math.max(1, template.getAmount()));

        ArmorStand itemStand = spawnStand(at, false);
        itemStand.getEquipment().setHelmet(stack);

        Location holoLoc = at.clone().add(0, 0.85, 0);
        ArmorStand holoStand = spawnStand(holoLoc, true);
        holoStand.setCustomName(startSealed
                ? HologramText.sealed(lang, owner.getUniqueId(), template.getRarity())
                : HologramText.revealed(lang, owner.getUniqueId(), template, inst));

        Slot slot = new Slot(itemStand, holoStand, inst, template, phase);
        slot.sealed = startSealed;
        slots.add(slot);
        clickLookup.put(itemStand.getUniqueId(), slot);
    }

    private ArmorStand spawnStand(Location loc, boolean hologram) {
        return loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setSmall(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setPersistent(false);
            as.setCustomNameVisible(hologram);
            try {
                ArmorStand.class.getMethod("setMarker", boolean.class).invoke(as, true);
            } catch (ReflectiveOperationException ignored) {
                // 1.18.x: marker yok, kucuk stand yeterli
            }
            try {
                ArmorStand.class.getMethod("setCollidable", boolean.class).invoke(as, false);
            } catch (ReflectiveOperationException ignored) {
            }
        });
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
            PlatformCompat.hideEntity(viewer, plugin, s.itemStand);
            PlatformCompat.hideEntity(viewer, plugin, s.holoStand);
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
                    double amp = s.sealed ? bobAmp : bobAmp * 0.4;
                    target.add(0, Math.sin(timeSec * bobSpeed + s.bobPhase) * amp, 0);
                    s.itemStand.teleport(target);
                    s.holoStand.teleport(target.clone().add(0, 0.85, 0));
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

        Location loc = s.itemStand.getLocation();
        ParticleCompat.revealBurst(loc);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.3f);
        try {
            loc.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        } catch (NoSuchFieldError ignored) {
        }

        s.itemStand.getEquipment().setHelmet(
                buildRealItemStack(s.template, Math.max(1, s.template.getAmount())));
        s.holoStand.setCustomName(HologramText.revealed(lang, owner.getUniqueId(), s.template, s.instance));
        return true;
    }

    @Override
    public void refreshSlotHologram(UUID clickTargetId) {
        Slot s = clickLookup.get(clickTargetId);
        if (s == null) return;
        if (s.sealed) {
            s.holoStand.setCustomName(HologramText.sealed(lang, owner.getUniqueId(), s.template.getRarity()));
        } else {
            s.holoStand.setCustomName(HologramText.revealed(lang, owner.getUniqueId(), s.template, s.instance));
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
                ParticleCompat.smokeClose(s.itemStand.getLocation());
            }
            s.itemStand.remove();
            s.holoStand.remove();
        }
        slots.clear();
        clickLookup.clear();
    }

    @Override
    public boolean isClosed() { return closed; }
}
