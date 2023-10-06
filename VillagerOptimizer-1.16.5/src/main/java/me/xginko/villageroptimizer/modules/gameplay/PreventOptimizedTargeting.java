package me.xginko.villageroptimizer.modules.gameplay;

import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;

public class PreventOptimizedTargeting implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;

    public PreventOptimizedTargeting() {
        this.villagerCache = VillagerOptimizer.getCache();
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.prevent-entities-from-targeting-optimized.enable", true,
                "Prevents hostile entities from targeting optimized villagers.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTarget(EntityTargetEvent event) {
        // Yes, instanceof checks would look way more beautiful here but checking type is much faster
        Entity target = event.getTarget();
        if (
                target != null
                && target.getType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) target).isOptimized()
        ) {
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityTargetVillager(EntityPathfindEvent event) {
        Entity target = event.getTargetEntity();
        if (
                target != null
                && target.getType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) target).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityAttackVillager(EntityDamageByEntityEvent event) {
        if (
                event.getEntityType().equals(EntityType.VILLAGER)
                && event.getDamager() instanceof Mob attacker
                && villagerCache.getOrAdd((Villager) event.getEntity()).isOptimized()
        ) {
            attacker.setTarget(null);
        }
    }
 }
