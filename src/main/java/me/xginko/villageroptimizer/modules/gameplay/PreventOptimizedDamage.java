package me.xginko.villageroptimizer.modules.gameplay;

import com.cryptomorin.xseries.XEntityType;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PreventOptimizedDamage extends VillagerOptimizerModule implements Listener {

    private final Set<EntityDamageEvent.DamageCause> damage_causes_to_cancel;
    private final boolean cancel_knockback;

    public PreventOptimizedDamage() {
        super("gameplay.prevent-damage-to-optimized");
        config.master().addComment(configPath + ".enable",
                "Configure what kind of damage you want to cancel for optimized villagers here.");
        this.cancel_knockback = config.getBoolean(configPath + ".prevent-knockback-from-entity", true,
                "Prevents optimized villagers from getting knocked back by an attacking entity");
        this.damage_causes_to_cancel = config.getList(configPath + ".damage-causes-to-cancel",
                Arrays.stream(EntityDamageEvent.DamageCause.values()).map(Enum::name).sorted().collect(Collectors.toList()),
                "These are all current entries in the game. Remove what you do not need blocked.\n" +
                "If you want a description or need to add a previously removed type, refer to:\n" +
                "https://jd.papermc.io/paper/1.20/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html")
                .stream()
                .map(configuredDamageCause -> {
                    try {
                        return EntityDamageEvent.DamageCause.valueOf(configuredDamageCause);
                    } catch (IllegalArgumentException e) {
                        warn("DamageCause '" + configuredDamageCause + "' not recognized. Please use correct DamageCause enums from: " +
                             "https://jd.papermc.io/paper/1.20/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EntityDamageEvent.DamageCause.class)));
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
    private void onDamageByEntity(EntityDamageEvent event) {
        if (
                event.getEntityType() == XEntityType.VILLAGER.get()
                && damage_causes_to_cancel.contains(event.getCause())
                && wrapperCache.get((Villager) event.getEntity(), WrappedVillager::new).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onKnockbackByEntity(com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent event) {
        if (
                cancel_knockback
                && event.getEntityType() == XEntityType.VILLAGER.get()
                && wrapperCache.get((Villager) event.getEntity(), WrappedVillager::new).isOptimized()
        ) {
            event.setCancelled(true);
        }
    }
}