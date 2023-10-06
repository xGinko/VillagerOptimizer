package me.xginko.villageroptimizer.modules.gameplay;

import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.LogUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Arrays;
import java.util.HashSet;

public class PreventOptimizedDamage implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final HashSet<EntityDamageEvent.DamageCause> damage_causes_to_cancel = new HashSet<>();
    private final boolean push;

    public PreventOptimizedDamage() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("gameplay.prevent-damage-to-optimized.enable",
                "Configure what kind of damage you want to cancel for optimized villagers here.");
        this.push = config.getBoolean("gameplay.prevent-damage-to-optimized.prevent-push-from-attack", true,
                "Prevents optimized villagers from getting pushed by an attacking entity");
        config.getList("gameplay.prevent-damage-to-optimized.damage-causes-to-cancel",
                Arrays.stream(EntityDamageEvent.DamageCause.values()).map(Enum::name).sorted().toList(), """
                These are all current entries in the game. Remove what you do not need blocked.\s
                If you want a description or need to add a previously removed type, refer to:\s
                https://jd.papermc.io/paper/1.20/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html"""
        ).forEach(configuredDamageCause -> {
            try {
                EntityDamageEvent.DamageCause damageCause = EntityDamageEvent.DamageCause.valueOf(configuredDamageCause);
                this.damage_causes_to_cancel.add(damageCause);
            } catch (IllegalArgumentException e) {
                LogUtil.damageCauseNotRecognized("prevent-damage-to-optimized", configuredDamageCause);
            }
        });
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
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.prevent-damage-to-optimized.enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDamageByEntity(EntityDamageEvent event) {
        if (
                event.getEntityType().equals(EntityType.VILLAGER)
                && damage_causes_to_cancel.contains(event.getCause())
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