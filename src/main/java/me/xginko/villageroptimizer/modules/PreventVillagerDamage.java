package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class PreventVillagerDamage implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;

    protected PreventVillagerDamage() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
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
        if (villagerManager.getOrAdd((Villager) event.getEntity()).isOptimized()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPushByEntityAttack(EntityPushedByEntityAttackEvent event) {
        if (!event.getEntityType().equals(EntityType.VILLAGER)) return;
        if (villagerManager.getOrAdd((Villager) event.getEntity()).isOptimized()) {
            event.setCancelled(true);
        }
    }
}
