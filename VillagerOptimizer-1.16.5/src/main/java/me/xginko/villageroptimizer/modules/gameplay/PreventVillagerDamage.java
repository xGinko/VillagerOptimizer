package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PreventVillagerDamage implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final boolean block, player, mob, other;

    public PreventVillagerDamage() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("gameplay.prevent-damage-to-optimized.enable",
                "Configure what kind of damage you want to cancel for optimized villagers here.");
        this.block = config.getBoolean("gameplay.prevent-damage-to-optimized.damagers.block", false,
                "Prevents damage from blocks like lava, tnt, respawn anchors, etc.");
        this.player = config.getBoolean("gameplay.prevent-damage-to-optimized.damagers.player", false,
                "Prevents damage from getting hit by players.");
        this.mob = config.getBoolean("gameplay.prevent-damage-to-optimized.damagers.mob", true,
                "Prevents damage from hostile mobs.");
        this.other = config.getBoolean("gameplay.prevent-damage-to-optimized.damagers.other", true,
                "Prevents damage from all other entities.");
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.prevent-damage-to-optimized.enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (
                event.getEntityType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) event.getEntity()).isOptimized()
        ) {
            Entity damager = event.getDamager();
            if (damager.getType().equals(EntityType.PLAYER)) {
                if (player) event.setCancelled(true);
                return;
            }

            if (damager instanceof Mob) {
                if (mob) event.setCancelled(true);
                return;
            }

            if (other) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDamageByBlock(EntityDamageByBlockEvent event) {
        if (
                block
                && event.getEntityType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) event.getEntity()).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }
}