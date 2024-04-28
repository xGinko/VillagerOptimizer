package me.xginko.villageroptimizer.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;

public class LocationUtil {

    public static @NotNull String toString(@NotNull Location location) {
        return "[" + location.getWorld().getName() + "] x=" + location.getBlockX() + ", y=" + location.getBlockY() + ", z=" + location.getBlockZ();
    }

    public static double relDistanceSquared2D(@NotNull Location from, @NotNull Location to) {
        double toX = to.getX();
        double toZ = to.getZ();
        double fromX = from.getX();
        double fromZ = from.getZ();

        // Make sure distance is relative since one block in the nether equates to 8 in the overworld/end
        if (to.getWorld().getEnvironment() != from.getWorld().getEnvironment()) {
            if (from.getWorld().getEnvironment() == World.Environment.NETHER) {
                fromX *= 8;
                fromZ *= 8;
            }
            if (to.getWorld().getEnvironment() == World.Environment.NETHER) {
                toX *= 8;
                toZ *= 8;
            }
        }

        return NumberConversions.square(toX - fromX) + NumberConversions.square(toZ - fromZ);
    }

    public static double relDistanceSquared3D(@NotNull Location from, @NotNull Location to) {
        double toY = to.getY();
        double fromY = from.getY();

        // Clamp Y levels the same way minecraft would for portal creation logic
        if (fromY < to.getWorld().getMinHeight())
            fromY = to.getWorld().getMinHeight();
        if (fromY > to.getWorld().getMaxHeight())
            fromY = to.getWorld().getMaxHeight();
        if (toY < from.getWorld().getMinHeight())
            toY = from.getWorld().getMinHeight();
        if (toY > from.getWorld().getMaxHeight())
            toY = from.getWorld().getMaxHeight();

        return relDistanceSquared2D(from, to) + NumberConversions.square(toY - fromY);
    }

    public static double relDistance2D(@NotNull Location from, @NotNull Location to) {
        return Math.sqrt(relDistanceSquared2D(from, to));
    }

    public static double relDistance3D(@NotNull Location from, @NotNull Location to) {
        return Math.sqrt(relDistanceSquared3D(from, to));
    }
}
