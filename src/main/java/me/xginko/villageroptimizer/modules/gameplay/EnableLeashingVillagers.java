package me.xginko.villageroptimizer.modules.gameplay;

import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
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

public class EnableLeashingVillagers implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;
    private final VillagerCache villagerCache;
    private final boolean only_optimized, log_enabled;

    public EnableLeashingVillagers() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("gameplay.villagers-can-be-leashed.enable", """
                Enable leashing of villagers, enabling players to easily move villagers to where they want them to be.""");
        this.only_optimized = config.getBoolean("gameplay.villagers-can-be-leashed.only-optimized", false,
                "If set to true, only optimized villagers can be leashed.");
        this.log_enabled = config.getBoolean("gameplay.villagers-can-be-leashed.log", false);
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
        final ItemStack handItem = player.getInventory().getItem(event.getHand());
        if (handItem == null || !handItem.getType().equals(Material.LEAD)) return;

        Villager villager = (Villager) event.getRightClicked();
        if (villager.isLeashed()) return;
        event.setCancelled(true); // Cancel the event, so we don't interact with the villager
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

        scheduler.runAtEntity(villager, leash -> {
            // Legitimate to not use entities from the event object since they are final in PlayerLeashEntityEvent
            if (villager.setLeashHolder(player)) {
                handItem.subtract(1);
                if (log_enabled) {
                    final Location location = villager.getLocation();
                    VillagerOptimizer.getLog().info(Component.text(player.getName() + " leashed a villager at " +
                            "x=" + location.getX() + ", y=" + location.getY() + ", z=" + location.getZ() +
                            " in world " + location.getWorld().getName()).style(VillagerOptimizer.plugin_style));
                }
            }
        });
    }
}
