package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MakeVillagersSpawnAdult implements VillagerOptimizerModule, Listener {

    public MakeVillagersSpawnAdult() {}

    @Override
    public String configPath() {
        return "gameplay.villagers-spawn-as-adults";
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
        return VillagerOptimizer.getConfiguration().getBoolean(configPath() + ".enable", false,
                "Spawned villagers will immediately be adults.\n" +
                "This is to save some more resources as players don't have to keep unoptimized\n" +
                "villagers loaded because they have to wait for them to turn into adults before they can\n" +
                "optimize them.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onVillagerSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType().equals(EntityType.VILLAGER)) {
            final Villager villager = (Villager) event.getEntity();
            if (!villager.isAdult()) villager.setAdult();
        }
    }
}
