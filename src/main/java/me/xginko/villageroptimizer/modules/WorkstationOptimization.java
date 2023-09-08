package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class WorkstationOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final Config config;
    private final boolean shouldLog, shouldNotifyPlayer;
    private final double search_radius;

    protected WorkstationOptimization() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        this.config = VillagerOptimizer.getConfiguration();
        this.config.addComment("optimization.methods.by-workstation.enable", """
                When enabled, villagers near a configured radius to a workstation specific to your config\s
                will be optimized.
                """);
        this.search_radius = config.getDouble("optimization.methods.by-workstation.search-radius-in-blocks", 4.0, """
                The radius in blocks a villager can be away from the player when he places a workstation.\s
                The closest unoptimized villager to the player will be optimized.
                """);
        this.shouldLog = config.getBoolean("optimization.methods.by-workstation.log", false);
        this.shouldNotifyPlayer = config.getBoolean("optimization.methods.by-workstation.notify-player", true);
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
        return config.enable_workstation_optimization;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        if (!config.workstations_that_disable.contains(placed.getType())) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                Villager villager = (Villager) entity;
                Villager.Profession profession = villager.getProfession();
                if (!profession.equals(Villager.Profession.NONE) && !profession.equals(Villager.Profession.NITWIT)) {
                    WrappedVillager wVillager = villagerManager.getOrAdd(villager);
                    if (!wVillager.isOptimized() && entity.getLocation().distance(workstationLoc) < closestDistance) {
                        closest = wVillager;
                    }
                }
            }
        }

        if (closest == null) return;

        if (closest.setOptimization(OptimizationType.WORKSTATION)) {
            if (shouldNotifyPlayer) {
                Player player = event.getPlayer();
                for (Component line : VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success) player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(closest.villager().getProfession().toString().toLowerCase()).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%workstation%").replacement(placed.getType().toString().toLowerCase()).build())
                );
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info(event.getPlayer().getName() + " optimized a villager using workstation: '" + placed.getType().toString().toLowerCase() + "'");
        } else {
            if (shouldNotifyPlayer) {
                Player player = event.getPlayer();
                for (Component line : VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown) player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(CommonUtils.formatTime(closest.getOptimizeCooldown())).build()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block placed = event.getBlock();
        if (!config.workstations_that_disable.contains(placed.getType())) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                Villager villager = (Villager) entity;
                Villager.Profession profession = villager.getProfession();
                if (!profession.equals(Villager.Profession.NONE) && !profession.equals(Villager.Profession.NITWIT)) {
                    WrappedVillager wVillager = villagerManager.getOrAdd(villager);
                    if (wVillager.isOptimized() && entity.getLocation().distance(workstationLoc) < closestDistance) {
                        closest = wVillager;
                    }
                }
            }
        }

        if (closest != null && closest.getOptimizationType().equals(OptimizationType.WORKSTATION)) {
            if (shouldNotifyPlayer) {
                Player player = event.getPlayer();
                for (Component line : VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success) player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(closest.villager().getProfession().toString().toLowerCase()).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%workstation%").replacement(placed.getType().toString().toLowerCase()).build())
                );
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info(event.getPlayer().getName() + " unoptimized a villager by breaking workstation: '" + placed.getType().toString().toLowerCase() + "'");
        }
    }
}
