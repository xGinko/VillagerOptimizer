package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.VillagerCache;
import me.xginko.villageroptimizer.models.WrappedVillager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class NametagOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;
    private final Config config;
    private final boolean shouldLog;

    protected NametagOptimization() {
        this.cache = VillagerOptimizer.getVillagerCache();
        this.config = VillagerOptimizer.getConfiguration();
        this.shouldLog = config.getBoolean("optimization.methods.by-nametag.log", false);
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
        return config.enable_nametag_optimization;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onNametag(PlayerNameEntityEvent event) {
        if (!event.getEntity().getType().equals(EntityType.VILLAGER)) return;
        Component name = event.getName();
        if (name == null) return;

        final String nameTag = PlainTextComponentSerializer.plainText().serialize(name);
        WrappedVillager wVillager = cache.get((Villager) event.getEntity());

        if (config.nametags.contains(nameTag.toLowerCase())) {
            if (!wVillager.isOptimized()) {
                wVillager.setOptimization(OptimizationType.NAMETAG);
                if (shouldLog) VillagerOptimizer.getLog().info(event.getPlayer().getName() + " optimized a villager using nametag: '" + nameTag + "'");
            }
        } else {
            if (wVillager.isOptimized()) {
                wVillager.setOptimization(OptimizationType.OFF);
                if (shouldLog) VillagerOptimizer.getLog().info(event.getPlayer().getName() + " disabled optimizations for a villager using nametag: '" + nameTag + "'");
            }
        }
    }
}
