package me.xginko.villageroptimizer.modules.gameplay;

import com.cryptomorin.xseries.XEntityType;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.entity.Entity;
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
                && target.getType() == XEntityType.VILLAGER.get()
                && wrapperCache.get((Villager) target, WrappedVillager::new).isOptimized()
        ) {
            event.setTarget(null);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityTargetVillager(com.destroystokyo.paper.event.entity.EntityPathfindEvent event) {
        final Entity target = event.getTargetEntity();
        if (
                target != null
                && target.getType() == XEntityType.VILLAGER.get()
                && wrapperCache.get((Villager) target, WrappedVillager::new).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityAttackVillager(EntityDamageByEntityEvent event) {
        if (
                event.getEntityType() == XEntityType.VILLAGER.get()
                && event.getDamager() instanceof Mob
                && wrapperCache.get((Villager) event.getEntity(), WrappedVillager::new).isOptimized()
        ) {
            ((Mob) event.getDamager()).setTarget(null);
        }
    }
 }
