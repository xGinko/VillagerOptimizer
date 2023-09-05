package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.VillagerCache;
import me.xginko.villageroptimizer.models.WrappedVillager;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class BlockOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;
    private final Config config;
    private final boolean shouldLog;

    protected BlockOptimization() {
        this.cache = VillagerOptimizer.getVillagerCache();
        this.config = VillagerOptimizer.getConfiguration();
        this.shouldLog = config.getBoolean("optimization.methods.by-specific-block.log", false);
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean shouldEnable() {
        return config.enable_block_optimization;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onEntityMove(EntityMoveEvent event) {
        if (!event.getEntityType().equals(EntityType.VILLAGER)) return;

        final Location entityLegs = event.getEntity().getLocation();
        if (
                config.blocks_that_disable.contains(entityLegs.getBlock().getType())
                || config.blocks_that_disable.contains(entityLegs.clone().subtract(0,1,0).getBlock().getType())
        ) {
            WrappedVillager wVillager = cache.get((Villager) event.getEntity());
            if (!wVillager.isOptimized()) {
                wVillager.setOptimization(OptimizationType.BLOCK);
                if (shouldLog) VillagerOptimizer.getLog().info("Villager moved onto an optimization block at "+wVillager.villager().getLocation());
            }
        } else {
            WrappedVillager wVillager = cache.get((Villager) event.getEntity());
            if (wVillager.isOptimized()) {
                wVillager.setOptimization(OptimizationType.OFF);
                if (shouldLog) VillagerOptimizer.getLog().info("Villager moved away from an optimization block at "+wVillager.villager().getLocation());
            }
        }
    }
}
