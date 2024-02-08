package me.xginko.villageroptimizer.modules.gameplay;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PreventOptimizedDamage implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final Set<EntityDamageEvent.DamageCause> damage_causes_to_cancel;
    private final boolean cancelKnockback;

    public PreventOptimizedDamage() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("gameplay.prevent-damage-to-optimized.enable",
                "Configure what kind of damage you want to cancel for optimized villagers here.");
        this.cancelKnockback = config.getBoolean("gameplay.prevent-damage-to-optimized.prevent-knockback-from-entity", true,
                "Prevents optimized villagers from getting knocked back by an attacking entity");
        this.damage_causes_to_cancel = config.getList("gameplay.prevent-damage-to-optimized.damage-causes-to-cancel",
                Arrays.stream(EntityDamageEvent.DamageCause.values()).map(Enum::name).sorted().toList(), """
                These are all current entries in the game. Remove what you do not need blocked.\s
                If you want a description or need to add a previously removed type, refer to:\s
                https://jd.papermc.io/paper/1.20/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html"""
        ).stream().map(configuredDamageCause -> {
            try {
                return EntityDamageEvent.DamageCause.valueOf(configuredDamageCause);
            } catch (IllegalArgumentException e) {
                VillagerOptimizer.getLog().warn("(prevent-damage-to-optimized) DamageCause '"+configuredDamageCause +
                        "' not recognized. Please use correct DamageCause enums from: " +
                        "https://jd.papermc.io/paper/1.20/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html");
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
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
    private void onKnockbackByEntity(EntityKnockbackByEntityEvent event) {
        if (
                cancelKnockback
                && event.getEntityType().equals(EntityType.VILLAGER)
                && villagerCache.getOrAdd((Villager) event.getEntity()).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }
}