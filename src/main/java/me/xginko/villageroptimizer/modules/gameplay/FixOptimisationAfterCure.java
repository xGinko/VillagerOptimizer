package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

import java.util.concurrent.TimeUnit;

public class FixOptimisationAfterCure implements VillagerOptimizerModule, Listener {

    public FixOptimisationAfterCure() {}

    @Override
    public String configPath() {
        return "post-cure-optimization-fix";
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
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTransform(EntityTransformEvent event) {
        if (
                event.getTransformReason().equals(EntityTransformEvent.TransformReason.CURED)
                && event.getTransformedEntity().getType().equals(EntityType.VILLAGER)
        ) {
            Villager villager = (Villager) event.getTransformedEntity();
            VillagerOptimizer.getFoliaLib().getImpl().runAtEntityLater(villager, () -> {
                WrappedVillager wVillager = VillagerOptimizer.getCache().getOrAdd(villager);
                wVillager.setOptimizationType(wVillager.getOptimizationType());
            }, 2, TimeUnit.SECONDS);
        }
    }
}