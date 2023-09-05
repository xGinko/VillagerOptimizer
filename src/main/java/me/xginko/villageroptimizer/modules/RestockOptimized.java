package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.models.WrappedVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class RestockOptimized implements VillagerOptimizerModule, Listener {

    private final long restock_delay;
    private final boolean shouldLog;

    public RestockOptimized() {
        Config config = VillagerOptimizer.getConfiguration();
        this.restock_delay = config.getInt("")
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        WrappedVillager wrappedVillager = new WrappedVillager((Villager) event.getRightClicked());
        if (!wrappedVillager.isOptimized()) return;

        if (wrappedVillager.getSavedWorldTime() >)
    }
}
