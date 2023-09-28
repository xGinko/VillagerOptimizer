package me.xginko.villageroptimizer.modules.extras;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PreventVillagerDamage implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final boolean block, player, mob, other, push;

    public PreventVillagerDamage() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("gameplay.prevent-damage.enable",
                "Configure what kind of damage you want to cancel for optimized villagers here.");
        this.block = config.getBoolean("gameplay.prevent-damage.block", false,
                "Prevents damage from blocks like lava, tnt, respawn anchors, etc.");
        this.player = config.getBoolean("gameplay.prevent-damage.player", false,
                "Prevents damage from getting hit by players.");
        this.mob = config.getBoolean("gameplay.prevent-damage.mob", true,
                "Prevents damage from hostile mobs.");
        this.other = config.getBoolean("gameplay.prevent-damage.other", true,
                "Prevents damage from all other entities.");
        this.push = config.getBoolean("gameplay.prevent-damage.prevent-push-from-attack", true,
                "Prevents optimized villagers from getting pushed by an attacking entity");
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
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.prevent-damage.enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDamageReceive(EntityDamageByEntityEvent event) {
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
    private void onDamageReceive(EntityDamageByBlockEvent event) {
        if (
                block
                && event.getEntityType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) event.getEntity()).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPushByEntityAttack(EntityPushedByEntityAttackEvent event) {
        if (
                push
                && event.getEntityType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) event.getEntity()).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }
}