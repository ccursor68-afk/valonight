package com.emergent.gecepazari.compat;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/** Surumler arasi guvenli parcacik spawn. */
public final class ParticleCompat {

    private ParticleCompat() {}

    public static void revealBurst(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        if (!trySpawn(world, loc, "EXPLOSION", 1)) {
            trySpawn(world, loc, "EXPLOSION_LARGE", 1);
        }
        trySpawn(world, loc, "EXPLOSION_EMITTER", 1);
        trySpawn(world, loc, "FLASH", 2);
        world.spawnParticle(Particle.END_ROD, loc, 30, 0.3, 0.3, 0.3, 0.05);
    }

    public static void smokeClose(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        if (!trySpawn(world, loc, "SMOKE", 24, 0.3, 0.4, 0.3, 0.02)) {
            trySpawn(world, loc, "SMOKE_NORMAL", 24, 0.3, 0.4, 0.3, 0.02);
        }
        trySpawn(world, loc, "SMOKE_LARGE", 8, 0.2, 0.2, 0.2, 0.0);
    }

    private static boolean trySpawn(World world, Location loc, String particleName, int count) {
        return trySpawn(world, loc, particleName, count, 0.2, 0.2, 0.2, 0.0);
    }

    private static boolean trySpawn(World world, Location loc, String particleName,
                                     int count, double ox, double oy, double oz, double extra) {
        try {
            Particle p = Particle.valueOf(particleName);
            world.spawnParticle(p, loc, count, ox, oy, oz, extra);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
