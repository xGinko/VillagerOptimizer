package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MakeVillagersSpawnAdult extends VillagerOptimizerModule implements Listener {

    public MakeVillagersSpawnAdult() {
        super("gameplay.villagers-spawn-as-adults");
        config.master().addComment(configPath + ".enable",
                "Spawned villagers will immediately be adults.\n" +
                "This is to save some more resources as players don't have to keep unoptimized\n" +
                "villagers loaded because they have to wait for them to turn into adults before they can\n" +
                "optimize them.");
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
        return config.getBoolean(configPath + ".enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onVillagerSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType().equals(EntityType.VILLAGER)) {
            final Villager villager = (Villager) event.getEntity();
            if (!villager.isAdult()) villager.setAdult();
        }
    }
}
