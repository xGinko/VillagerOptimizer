package me.xginko.villageroptimizer.modules.gameplay;

import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EnableLeashingVillagers implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;
    private final VillagerCache villagerCache;
    private final boolean only_optimized;

    public EnableLeashingVillagers() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("gameplay.villagers-can-be-leashed.enable", """
                Enable leashing of villagers, enabling players to easily move villagers to where they want them to be.""");
        this.only_optimized = config.getBoolean("gameplay.villagers-can-be-leashed.only-optimized", false,
                "If set to true, only optimized villagers can be leashed.");
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
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.villagers-can-be-leashed.enable", false);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLeash(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        final Player player = event.getPlayer();
        if (!player.getInventory().getItem(event.getHand()).getType().equals(Material.LEAD)) return;

        event.setCancelled(true); // Cancel the event, so we don't interact with the villager
        Villager villager = (Villager) event.getRightClicked();

        if (villager.isLeashed()) {
            // If leash holder clicked leashed villager, unleash.
            try {
                if (villager.getLeashHolder().getUniqueId().equals(player.getUniqueId()))
                    villager.setLeashHolder(null);
            } catch (IllegalStateException ignored) {} // Shouldn't throw because we checked LivingEntity#isLeashed()
            // Otherwise do nothing. There should only ever be one leash holder
            return;
        }

        if (only_optimized && !villagerCache.getOrAdd(villager).isOptimized()) return;

        // Call event for compatibility with other plugins, constructing non deprecated if available
        PlayerLeashEntityEvent leashEvent;
        try {
            leashEvent = new PlayerLeashEntityEvent(villager, player, player, event.getHand());
        } catch (Throwable versionIncompatible) {
            leashEvent = new PlayerLeashEntityEvent(villager, player, player);
        }

        // If canceled by any plugin, do nothing
        if (!leashEvent.callEvent()) return;

        // Legitimate to not use entities from the event object since they are final in PlayerLeashEntityEvent
        scheduler.runAtEntity(villager, leash -> villager.setLeashHolder(player));
    }
}
