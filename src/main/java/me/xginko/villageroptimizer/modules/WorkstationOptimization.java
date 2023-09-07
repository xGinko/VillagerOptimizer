package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.entity.EntityMoveEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.cache.VillagerManager;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class WorkstationOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final Config config;
    private final boolean shouldLog, shouldNotifyPlayer;

    protected WorkstationOptimization() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        this.config = VillagerOptimizer.getConfiguration();
        this.config.addComment("optimization.methods.by-workstation.enable", """
                When enabled, villagers near a configured radius to a workstation specific to their profession\s
                will be optimized.
                """);
        this.shouldLog = config.getBoolean("optimization.methods.by-workstation.log", false);
        this.shouldNotifyPlayer = config.getBoolean("optimization.methods.by-workstation.notify-player", true);
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
    private void onEntityMove(EntityMoveEvent event) {
        if (!event.getEntity().getType().equals(EntityType.VILLAGER)) return;


    }
}
