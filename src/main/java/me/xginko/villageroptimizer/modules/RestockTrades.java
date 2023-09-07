package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.models.VillagerCache;
import me.xginko.villageroptimizer.models.WrappedVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class RestockTrades implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;
    private final long restock_delay;
    private final boolean shouldLog;

    protected RestockTrades() {
        this.cache = VillagerOptimizer.getVillagerCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization.trade-restocking.enable", """
                This is for automatic restocking of trades for optimized villagers. Optimized Villagers\s
                Don't have enough AI to do trade restocks themselves, so this needs to always be enabled.
                """);
        this.restock_delay = config.getInt("optimization.trade-restocking.delay-in-ticks", 1200);
        this.shouldLog = config.getBoolean("optimization.trade-restocking.log", false);
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.trade-restocking.enable", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        WrappedVillager wVillager = cache.getOrAdd((Villager) event.getRightClicked());
        if (!wVillager.isOptimized()) return;


    }
}
