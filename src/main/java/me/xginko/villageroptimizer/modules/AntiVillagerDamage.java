package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.models.VillagerCache;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class AntiVillagerDamage implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;

    protected AntiVillagerDamage() {
        this.cache = VillagerOptimizer.getVillagerCache();
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.behavior.optimized-villagers-dont-take-damage", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDamageReceive(EntityDamageEvent event) {
        if (!event.getEntityType().equals(EntityType.VILLAGER)) return;
        if (cache.get((Villager) event.getEntity()).isOptimized()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPushByEntityAttack(EntityPushedByEntityAttackEvent event) {
        if (!event.getEntityType().equals(EntityType.VILLAGER)) return;
        if (cache.get((Villager) event.getEntity()).isOptimized()) {
            event.setCancelled(true);
        }
    }
}
