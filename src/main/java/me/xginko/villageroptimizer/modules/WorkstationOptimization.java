package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.models.VillagerCache;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class WorkstationOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;
    private final Config config;
    private final boolean shouldLog;

    protected WorkstationOptimization() {
        this.cache = VillagerOptimizer.getVillagerCache();
        this.config = VillagerOptimizer.getConfiguration();
        this.shouldLog = config.getBoolean("optimization.methods.by-workstation.log", false);
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
        return config.enable_workstation_optimization;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onEvent() {

    }
}
