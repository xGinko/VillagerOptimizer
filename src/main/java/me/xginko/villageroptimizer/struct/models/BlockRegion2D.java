package me.xginko.villageroptimizer.struct.models;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BlockRegion2D {

    private final UUID worldUID;
    private final double halfSideLength, centerX, centerZ;

    /**
     * A square region on a minecraft world map.
     *
     * @param worldUID The UUID of the world this region is in.
     * @param centerX The X-axis of the center location on the map.
     * @param centerZ The Z-axis of the center location on the map.
     * @param halfSideLength Half the length of the square's side. Acts like a radius would on circular regions.
     */
    public BlockRegion2D(UUID worldUID, double centerX, double centerZ, double halfSideLength) {
        this.worldUID = worldUID;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.halfSideLength = halfSideLength;
    }

    /**
     * Creates a square region on a minecraft world map.
     *
     * @param worldUID The UUID of the world this region is in.
     * @param centerX The X-axis of the center location on the map.
     * @param centerZ The Z-axis of the center location on the map.
     * @param halfSideLength Half the length of the square's side. Acts like a radius would on circular regions.
     */
    public static BlockRegion2D of(UUID worldUID, double centerX, double centerZ, double halfSideLength) {
        return new BlockRegion2D(worldUID, centerX, centerZ, halfSideLength);
    }

    /**
     * Creates a square region on a minecraft world map.
     *
     * @param world The world this region is in.
     * @param centerX The X-axis of the center location on the map.
     * @param centerZ The Z-axis of the center location on the map.
     * @param halfSideLength Half the length of the square's side. Acts like a radius would on circular regions.
     */
    public static BlockRegion2D of(World world, double centerX, double centerZ, double halfSideLength) {
        return BlockRegion2D.of(world.getUID(), centerX, centerZ, halfSideLength);
    }

    public UUID getWorldUID() {
        return this.worldUID;
    }

    public double getHalfSideLength() {
        return this.halfSideLength;
    }

    public double getCenterX() {
        return this.centerX;
    }

    public double getCenterZ() {
        return this.centerZ;
    }

    public boolean contains(Location location) {
        if (!location.getWorld().getUID().equals(this.worldUID)) {
            return false;
        }

        return  location.getX() >= this.centerX - this.halfSideLength
                && location.getX() <= this.centerX + this.halfSideLength
                && location.getZ() >= this.centerZ - this.halfSideLength
                && location.getZ() <= this.centerZ + this.halfSideLength;
    }

    public CompletableFuture<Collection<Entity>> getEntities() {
        World world = Bukkit.getWorld(worldUID);

        if (world == null) {
            // Only way I can imagine this happening would be if the server is using a world manager plugin and unloads
            // the world during an operation.
            // Since these plugins are rather common though, we will silently complete with an empty set instead of exceptionally.
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        CompletableFuture<Collection<Entity>> future = new CompletableFuture<>();
        Location centerLoc = new Location(world, centerX, world.getMinHeight(), centerZ);

        VillagerOptimizer.scheduling().regionSpecificScheduler(centerLoc).run(() -> future.complete(
                centerLoc.getNearbyEntities(
                        halfSideLength,
                        Math.abs(world.getMaxHeight()) + Math.abs(world.getMinHeight()), // World y can be between -64 and 320, we want everything from top to bottom
                        halfSideLength
                )));

        return future;
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj || obj.getClass() != BlockRegion2D.class)
            return false;
        BlockRegion2D blockRegion2D = (BlockRegion2D)obj;
        return blockRegion2D.worldUID.equals(this.worldUID) && blockRegion2D.centerX == this.centerX && blockRegion2D.centerZ == this.centerZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.worldUID, this.centerX, this.centerZ, this.halfSideLength);
    }

    @Override
    public String toString() {
        return "BlockRegion2D{" +
                " radius(half side length)=" + halfSideLength +
                ", centerX=" + centerX +
                ", centerZ=" + centerZ +
                ", worldUID=" + worldUID +
                "}";
    }
}
