package me.xginko.villageroptimizer.modules.gameplay;

import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class VisuallyHighlightOptimized implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;

    public VisuallyHighlightOptimized() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("gameplay.outline-optimized-villagers.enable",
                "Will make optimized villagers glow.");
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
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.outline-optimized-villagers.enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onOptimize(VillagerOptimizeEvent event) {
        Villager villager = event.getWrappedVillager().villager();
        scheduler.runAtEntity(villager, glow -> {
            if (!villager.isGlowing()) villager.setGlowing(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onUnOptimize(VillagerUnoptimizeEvent event) {
        Villager villager = event.getWrappedVillager().villager();
        scheduler.runAtEntity(villager, unGlow -> {
            if (villager.isGlowing()) villager.setGlowing(false);
        });
    }
}