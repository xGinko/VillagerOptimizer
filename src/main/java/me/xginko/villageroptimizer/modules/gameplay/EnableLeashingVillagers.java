package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.models.ExpiringSet;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.time.Duration;
import java.util.UUID;

public class EnableLeashingVillagers implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final ExpiringSet<UUID> villagersThatShouldntOpenTradeView;
    private final boolean only_optimized;

    public EnableLeashingVillagers() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        this.villagersThatShouldntOpenTradeView = new ExpiringSet<>(Duration.ofSeconds(1));
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

        Villager villager = (Villager) event.getRightClicked();

        if (villager.isLeashed()) {
            // If leash holder clicked leashed villager, unleash.
            try {
                if (villager.getLeashHolder().getUniqueId().equals(player.getUniqueId())) {
                    villager.setLeashHolder(null);
                    villagersThatShouldntOpenTradeView.add(villager.getUniqueId());
                }
            } catch (IllegalStateException ignored) {
                // This shouldn't throw because we check LivingEntity#isLeashed(), but if for some reason it does, we catch it.
            }

            // Otherwise do nothing. There should only ever be one leash holder.
            return;
        }

        if (only_optimized && !villagerCache.getOrAdd(villager).isOptimized()) return;

        // Call event for compatibility with other plugins, constructing non deprecated if available.
        PlayerLeashEntityEvent leashEvent;
        try {
            leashEvent = new PlayerLeashEntityEvent(villager, player, player, event.getHand());
        } catch (Throwable versionIncompatible) {
            leashEvent = new PlayerLeashEntityEvent(villager, player, player);
        }

        // If canceled by any plugin, do nothing.
        if (!leashEvent.callEvent()) return;

        VillagerOptimizer.getFoliaLib().getImpl().runAtEntity(villager, leash -> {
            // Legitimate to not use entities from the event object since they are final in PlayerLeashEntityEvent
            // and can therefore never be changed by a plugin.
            if (villager.setLeashHolder(player)) {
                villagersThatShouldntOpenTradeView.add(villager.getUniqueId());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onInventoryOpen(InventoryOpenEvent event) {
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager villager
                && villagersThatShouldntOpenTradeView.contains(villager.getUniqueId())
        ) {
            event.setCancelled(true);
        }
    }
}
