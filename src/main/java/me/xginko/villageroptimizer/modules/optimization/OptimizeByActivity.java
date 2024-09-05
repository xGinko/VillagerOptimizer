package me.xginko.villageroptimizer.modules.optimization;

import com.cryptomorin.xseries.XEntityType;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.struct.enums.OptimizationType;
import me.xginko.villageroptimizer.struct.models.BlockRegion2D;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizeByActivity extends VillagerOptimizerModule implements Listener {

    protected static class RegionData {

        public final BlockRegion2D region;
        public final AtomicInteger pathfindCount, entityInteractCount;
        public final AtomicBoolean regionBusy;

        public RegionData(BlockRegion2D region) {
            this.region = region;
            this.pathfindCount = new AtomicInteger();
            this.entityInteractCount = new AtomicInteger();
            this.regionBusy = new AtomicBoolean(false);
        }
    }

    private final Cache<BlockRegion2D, RegionData> regionDataCache;
    private final double checkRadius;
    private final int pathfindLimit, entityInteractLimit;
    private final boolean notifyPlayers, doLogging;

    public OptimizeByActivity() {
        super("optimization-methods.regional-activity");
        config.master().addComment(configPath + ".enable",
                "Enable optimization by naming villagers to one of the names configured below.\n" +
                "Nametag optimized villagers will be unoptimized again when they are renamed to something else.");

        this.checkRadius = config.getDouble(configPath + ".check-radius-blocks", 500.0,
                "The radius in blocks in which activity will be grouped together and measured.");
        this.regionDataCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(
                config.getInt(configPath + ".data-keep-time-millis", 10000,
                        "The time in milliseconds before a region and its data will be expired\n" +
                                "if no activity has been detected.\n" +
                                "For proper functionality, needs to be at least as long as your pause time."))).build();

        this.pathfindLimit = config.getInt(configPath + ".limits.pathfind-event", 150);
        this.entityInteractLimit = config.getInt(configPath + ".limits.interact-event", 50);

        this.notifyPlayers = config.getBoolean(configPath + ".notify-players", true,
                "Sends players a message to any player near an auto-optimized villager.");
        this.doLogging = config.getBoolean(configPath + ".log", false);
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean shouldEnable() {
        return config.getBoolean(configPath + ".enable", false);
    }

    private @NotNull RegionData getRegionData(Location location) {
        return regionDataCache.get(getRegion(location), RegionData::new);
    }

    private @NotNull BlockRegion2D getRegion(Location location) {
        // Find and return region containing this location
        for (Map.Entry<BlockRegion2D, RegionData> regionDataEntry : regionDataCache.asMap().entrySet()) {
            if (regionDataEntry.getKey().contains(location)) {
                return regionDataEntry.getKey();
            }
        }

        // Create and cache region if none exists
        BlockRegion2D region = BlockRegion2D.of(location.getWorld(), location.getX(), location.getZ(), checkRadius);
        regionDataCache.put(region, new RegionData(region));
        return region;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityPathfind(EntityPathfindEvent event) {
        if (event.getEntityType() != XEntityType.VILLAGER.get()) return;

        Location location = event.getEntity().getLocation();
        BlockRegion2D region2D = getRegion(location);
        RegionData regionData = getRegionData(location);

        if (regionData.regionBusy.get() || regionData.pathfindCount.incrementAndGet() <= pathfindLimit) {
            return;
        }

        regionData.regionBusy.set(true);

        AtomicInteger optimizeCount = new AtomicInteger();
        Set<Player> playersWithinArea = new CopyOnWriteArraySet<>();

        region2D.getEntities()
                .thenAccept(entities -> {
                    for (Entity entity : entities) {
                        scheduling.entitySpecificScheduler(entity).run(() -> {
                            if (entity.getType() == XEntityType.VILLAGER.get()) {
                                WrappedVillager wrappedVillager = wrapperCache.get((Villager) entity, WrappedVillager::new);

                                if (wrappedVillager.isOptimized()) {
                                    return;
                                }

                                wrappedVillager.setOptimizationType(OptimizationType.REGIONAL_ACTIVITY);
                                optimizeCount.incrementAndGet();
                            }

                            if (notifyPlayers && entity.getType() == XEntityType.PLAYER.get()) {
                                playersWithinArea.add((Player) entity);
                            }
                        }, null);
                    }
                })
                .thenRun(() -> {
                    if (notifyPlayers) {
                        TextReplacementConfig amount = TextReplacementConfig.builder()
                                .matchLiteral("%amount%")
                                .replacement(optimizeCount.toString())
                                .build();

                        for (Player player : playersWithinArea) {
                            VillagerOptimizer.scheduling().entitySpecificScheduler(player).run(() ->
                                            VillagerOptimizer.getLang(player.locale()).activity_optimize_success
                                                    .forEach(line -> player.sendMessage(line.replaceText(amount))),
                                    null);
                        }

                        playersWithinArea.clear();
                    }

                    if (doLogging) {
                        info(   "Optimized " + optimizeCount.get() + " villagers in a radius of " + checkRadius +
                                " blocks from center at x=" + regionData.region.getCenterX() + ", z=" + regionData.region.getCenterZ() +
                                " in world " + location.getWorld().getName() +
                                "because of too high pathfinding activity within the configured timeframe: " +
                                regionData.pathfindCount + " (limit: " + pathfindLimit + ")");
                    }

                    regionDataCache.invalidate(region2D);
                });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityInteract(EntityInteractEvent event) {
        if (event.getEntityType() != XEntityType.VILLAGER.get()) return;

        Location location = event.getEntity().getLocation();
        BlockRegion2D region2D = getRegion(location);
        RegionData regionData = getRegionData(location);

        if (regionData.regionBusy.get() || regionData.entityInteractCount.incrementAndGet() <= entityInteractLimit) {
            return;
        }

        regionData.regionBusy.set(true);

        AtomicInteger optimizeCount = new AtomicInteger();
        Set<Player> playersWithinArea = new CopyOnWriteArraySet<>();

        region2D.getEntities()
                .thenAccept(entities -> {
                    for (Entity entity : entities) {
                        scheduling.entitySpecificScheduler(entity).run(() -> {
                            if (entity.getType() == XEntityType.VILLAGER.get()) {
                                WrappedVillager wrappedVillager = wrapperCache.get((Villager) entity, WrappedVillager::new);

                                if (wrappedVillager.isOptimized()) {
                                    return;
                                }

                                wrappedVillager.setOptimizationType(OptimizationType.REGIONAL_ACTIVITY);
                                optimizeCount.incrementAndGet();
                            }

                            if (notifyPlayers && entity.getType() == XEntityType.PLAYER.get()) {
                                playersWithinArea.add((Player) entity);
                            }
                        }, null);
                    }
                })
                .thenRun(() -> {
                    if (notifyPlayers) {
                        TextReplacementConfig amount = TextReplacementConfig.builder()
                                .matchLiteral("%amount%")
                                .replacement(optimizeCount.toString())
                                .build();

                        for (Player player : playersWithinArea) {
                            VillagerOptimizer.scheduling().entitySpecificScheduler(player).run(() ->
                                            VillagerOptimizer.getLang(player.locale()).activity_optimize_success
                                                    .forEach(line -> player.sendMessage(line.replaceText(amount))),
                                    null);
                        }

                        playersWithinArea.clear();
                    }

                    if (doLogging) {
                        info(   "Optimized " + optimizeCount.get() + " villagers in a radius of " + checkRadius +
                                " blocks from center at x=" + regionData.region.getCenterX() + ", z=" + regionData.region.getCenterZ() +
                                " in world " + location.getWorld().getName() +
                                "because of too many villagers interacting with objects within the configured timeframe: " +
                                regionData.pathfindCount + " (limit: " + pathfindLimit + ")");
                    }

                    regionDataCache.invalidate(region2D);
                });
    }
}