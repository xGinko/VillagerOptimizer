package me.xginko.villageroptimizer.modules;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.models.VillagerCache;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class AntiVillagerTargetting implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;

    protected AntiVillagerTargetting() {
        this.cache = VillagerOptimizer.getVillagerCache();
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.behavior.optimized-villagers-dont-get-targeted", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Villager villager && cache.get(villager).isOptimized()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTarget(EntityPathfindEvent event) {
        if (event.getTargetEntity() instanceof Villager villager && cache.get(villager).isOptimized()) {
            event.setCancelled(true);
        }
    }
}
