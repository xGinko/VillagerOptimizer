package me.xginko.villageroptimizer.modules.gameplay;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class PreventOptimizedTargeting extends VillagerOptimizerModule implements Listener {

    public PreventOptimizedTargeting() {
        super("gameplay.prevent-entities-from-targeting-optimized");
        config.master().addComment(configPath + ".enable",
                "Prevents hostile entities from targeting optimized villagers.");
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
        return config.getBoolean(configPath + ".enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTarget(EntityTargetEvent event) {
        final Entity target = event.getTarget();
        if (
                target != null
                && target.getType().equals(EntityType.VILLAGER)
                && villagerCache.createIfAbsent((Villager) target).isOptimized()
        ) {
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityTargetVillager(EntityPathfindEvent event) {
        final Entity target = event.getTargetEntity();
        if (
                target != null
                && target.getType().equals(EntityType.VILLAGER)
                && villagerCache.createIfAbsent((Villager) target).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityAttackVillager(EntityDamageByEntityEvent event) {
        if (
                event.getEntityType().equals(EntityType.VILLAGER)
                && event.getDamager() instanceof Mob
                && villagerCache.createIfAbsent((Villager) event.getEntity()).isOptimized()
        ) {
            ((Mob) event.getDamager()).setTarget(null);
        }
    }
 }
