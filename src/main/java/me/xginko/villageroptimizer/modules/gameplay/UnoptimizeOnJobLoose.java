package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerCareerChangeEvent;

public class UnoptimizeOnJobLoose implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;

    public UnoptimizeOnJobLoose() {
        this.villagerCache = VillagerOptimizer.getCache();
    }

    @Override
    public String configPath() {
        return "gameplay.unoptimize-on-job-loose";
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
        return VillagerOptimizer.getConfiguration().getBoolean(configPath() + ".enable", true,
                "Villagers that get their jobs reset will become unoptimized again.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onJobReset(VillagerCareerChangeEvent event) {
        if (!event.getReason().equals(VillagerCareerChangeEvent.ChangeReason.LOSING_JOB)) return;
        final WrappedVillager wrappedVillager = villagerCache.getOrAdd(event.getEntity());
        if (wrappedVillager.isOptimized()) {
            wrappedVillager.setOptimizationType(OptimizationType.NONE);
        }
    }
}
