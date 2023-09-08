package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class LevelVillagers implements VillagerOptimizerModule, Listener {

    private final VillagerOptimizer plugin;
    private final VillagerManager villagerManager;

    public LevelVillagers() {
        this.plugin = VillagerOptimizer.getInstance();
        this.villagerManager = VillagerOptimizer.getVillagerManager();

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
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onSomething() {

    }
}
