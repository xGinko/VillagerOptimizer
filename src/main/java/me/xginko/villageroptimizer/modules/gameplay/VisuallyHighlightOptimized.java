package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class VisuallyHighlightOptimized extends VillagerOptimizerModule implements Listener {

    public VisuallyHighlightOptimized() {
        super("gameplay.outline-optimized-villagers");
        config.master().addComment("gameplay.outline-optimized-villagers.enable",
                "Will make optimized villagers glow.");
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
        return config.getBoolean("gameplay.outline-optimized-villagers.enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onOptimize(VillagerOptimizeEvent event) {
        Villager villager = event.getWrappedVillager().villager();
        scheduling.entitySpecificScheduler(villager).run(glow -> {
            if (!villager.isGlowing()) villager.setGlowing(true);
        }, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onUnOptimize(VillagerUnoptimizeEvent event) {
        Villager villager = event.getWrappedVillager().villager();
        scheduling.entitySpecificScheduler(villager).run(unGlow -> {
            if (villager.isGlowing()) villager.setGlowing(false);
        }, null);
    }
}