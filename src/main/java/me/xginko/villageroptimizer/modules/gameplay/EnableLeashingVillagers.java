package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.LocationUtil;
import org.bukkit.GameMode;
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
import org.bukkit.inventory.ItemStack;

public class EnableLeashingVillagers extends VillagerOptimizerModule implements Listener {

    private final boolean only_optimized, log_enabled;

    public EnableLeashingVillagers() {
        super("gameplay.villagers-can-be-leashed");
        config.master().addComment(configPath + ".enable",
                "Enable leashing of villagers, enabling players to easily move villagers to where they want them to be.");
        this.only_optimized = config.getBoolean(configPath + ".only-optimized", false,
                "If set to true, only optimized villagers can be leashed.");
        this.log_enabled = config.getBoolean(configPath + ".log", false);
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

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onLeash(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        final Player player = event.getPlayer();
        final ItemStack handItem = player.getInventory().getItem(event.getHand());
        if (handItem == null || !handItem.getType().equals(Material.LEAD)) return;

        final Villager villager = (Villager) event.getRightClicked();
        if (villager.isLeashed()) return;
        if (only_optimized && !villagerCache.createIfAbsent(villager).isOptimized()) return;

        event.setCancelled(true); // Cancel the event, so we don't interact with the villager

        // Call event for compatibility with other plugins, constructing non deprecated if available
        PlayerLeashEntityEvent leashEvent;
        try {
            leashEvent = new PlayerLeashEntityEvent(villager, player, player, event.getHand());
        } catch (Throwable versionIncompatible) {
            leashEvent = new PlayerLeashEntityEvent(villager, player, player);
        }

        // If canceled by any plugin, do nothing
        if (!leashEvent.callEvent()) return;

        scheduler.runAtEntity(villager, leash -> {
            // Legitimate to not use entities from the event object since they are final in PlayerLeashEntityEvent
            if (!villager.setLeashHolder(player)) return;
            if (player.getGameMode().equals(GameMode.SURVIVAL))
                handItem.subtract(1); // Manually consume for survival players

            if (log_enabled) {
                info(player.getName() + " leashed a villager at " + LocationUtil.toString(villager.getLocation()));
            }
        });
    }
}
