package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;

import java.util.concurrent.TimeUnit;

public class FixOptimisationAfterCure extends VillagerOptimizerModule implements Listener {

    public FixOptimisationAfterCure() {
        super("post-cure-optimization-fix");
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
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTransform(EntityTransformEvent event) {
        if (
                event.getTransformReason().equals(EntityTransformEvent.TransformReason.CURED)
                && event.getTransformedEntity().getType().equals(EntityType.VILLAGER)
        ) {
            Villager villager = (Villager) event.getTransformedEntity();
            scheduler.runAtEntityLater(villager, () -> {
                WrappedVillager wVillager = villagerCache.createIfAbsent(villager);
                wVillager.setOptimizationType(wVillager.getOptimizationType());
            }, 2, TimeUnit.SECONDS);
        }
    }
}