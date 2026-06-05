package com.emergent.gecepazari.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Yarim ay (arc) seklinde duzenli noktalar hesaplar.
 * Oyuncunun bakis yonune gore esyalarin pozisyonlarini bulur.
 */
public final class ArcMath {

    private ArcMath() {
    }

    /**
     * Oyuncunun onunde, bakis yonu boyunca yarim ay olusturur.
     *
     * @param origin         oyuncunun lokasyonu
     * @param count          esya sayisi
     * @param radius         oyuncudan uzaklik
     * @param arcDegrees     yarim ayin acisi (derece)
     * @param heightOffset   y ekseninde offset
     */
    public static Location[] calculateArcPoints(Location origin,
                                                int count,
                                                double radius,
                                                double arcDegrees,
                                                double heightOffset) {
        Location[] points = new Location[count];
        if (count == 0) return points;

        float yaw = origin.getYaw();
        double yawRad = Math.toRadians(yaw);

        // forward vector (XZ duzleminde)
        Vector forward = new Vector(-Math.sin(yawRad), 0.0, Math.cos(yawRad)).normalize();
        // right vector (forward'a dik, XZ)
        Vector right = new Vector(Math.cos(yawRad), 0.0, Math.sin(yawRad)).normalize();

        double half = arcDegrees / 2.0;
        // Esyalari -half ile +half arasinda esit dagit
        double step = count == 1 ? 0 : arcDegrees / (count - 1);

        for (int i = 0; i < count; i++) {
            double angleDeg = -half + step * i;
            double angleRad = Math.toRadians(angleDeg);

            double cos = Math.cos(angleRad);
            double sin = Math.sin(angleRad);

            Vector offset = forward.clone().multiply(cos).add(right.clone().multiply(sin)).multiply(radius);

            Location point = origin.clone().add(offset.getX(), heightOffset, offset.getZ());
            points[i] = point;
        }

        return points;
    }

    /**
     * Bir lokasyonun, hedef lokasyona bakacak sekilde yaw acisini hesaplar.
     */
    public static float yawTowards(Location from, Location target) {
        double dx = target.getX() - from.getX();
        double dz = target.getZ() - from.getZ();
        double yawRad = Math.atan2(-dx, dz);
        return (float) Math.toDegrees(yawRad);
    }
}
