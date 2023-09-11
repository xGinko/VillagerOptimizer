package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class NoBabyVillagers implements VillagerOptimizerModule, Listener {

    protected NoBabyVillagers() {}

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
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.villagers-spawn-as-adults", false,
                "Automatically turns baby villagers into adults when spawning.");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onVillagerSpawn(CreatureSpawnEvent event) {
        if (!event.getEntityType().equals(EntityType.VILLAGER)) return;
        Villager villager = (Villager) event.getEntity();
        if (!villager.isAdult()) villager.setAdult();
    }
}
