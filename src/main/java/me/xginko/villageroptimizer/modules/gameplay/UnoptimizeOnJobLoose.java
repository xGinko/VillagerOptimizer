package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;

public class UnoptimizeOnJobLoose extends VillagerOptimizerModule implements Listener {

    public UnoptimizeOnJobLoose() {
        super("gameplay.unoptimize-on-job-loose");
        config.master().addComment(configPath + ".enable",
                "Villagers that get their jobs reset will become unoptimized again.");
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
        return config.getBoolean(configPath + ".enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onJobReset(VillagerCareerChangeEvent event) {
        if (event.getReason() != VillagerCareerChangeEvent.ChangeReason.LOSING_JOB) return;
        final WrappedVillager wrappedVillager = villagerCache.createIfAbsent(event.getEntity());
        if (wrappedVillager.isOptimized()) {
            wrappedVillager.setOptimizationType(OptimizationType.NONE);
        }
    }
}
