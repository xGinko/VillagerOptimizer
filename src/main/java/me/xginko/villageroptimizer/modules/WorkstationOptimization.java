package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import me.xginko.villageroptimizer.utils.LogUtils;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.HashSet;
import java.util.List;

public class WorkstationOptimization implements VillagerOptimizerModule, Listener {
    
    private final VillagerManager villagerManager;
    private final HashSet<Material> workstations_that_disable = new HashSet<>(14);
    private final boolean shouldLog, shouldNotifyPlayer;
    private final long cooldown;
    private final double search_radius;

    protected WorkstationOptimization() {
        shouldEnable();
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization.methods.by-workstation.enable", """
                        When enabled, villagers near a configured radius to a workstation specific to your config\s
                        will be optimized.""");
        config.getList("optimization.methods.by-workstation.workstation-materials", List.of(
                "COMPOSTER", "SMOKER", "BARREL", "LOOM", "BLAST_FURNACE", "BREWING_STAND", "CAULDRON",
                "FLETCHING_TABLE", "CARTOGRAPHY_TABLE", "LECTERN", "SMITHING_TABLE", "STONECUTTER", "GRINDSTONE"
        ), "Values here need to be valid bukkit Material enums for your server version."
        ).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.workstations_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("optimization.methods.by-workstation", configuredMaterial);
            }
        });
        this.search_radius = config.getDouble("optimization.methods.by-workstation.search-radius-in-blocks", 4.0, """
                The radius in blocks a villager can be away from the player when he places a workstation.\s
                The closest unoptimized villager to the player will be optimized.""");
        this.cooldown = config.getInt("optimization.methods.by-workstation.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again using this method.\s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.""") * 1000L;
        this.shouldNotifyPlayer = config.getBoolean("optimization.methods.by-workstation.notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.shouldLog = config.getBoolean("optimization.methods.by-workstation.log", false);
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.methods.by-workstation.enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        if (!workstations_that_disable.contains(placed.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closestOptimizableVillager = null;
        double closestDistance = search_radius;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            final Villager.Profession profession = villager.getProfession();
            if (profession.equals(Villager.Profession.NONE) || profession.equals(Villager.Profession.NITWIT)) continue;

            WrappedVillager wVillager = villagerManager.getOrAdd(villager);
            final double distance = entity.getLocation().distance(workstationLoc);

            if (!wVillager.isOptimized() && distance < closestDistance) {
                closestOptimizableVillager = wVillager;
                closestDistance = distance;
            }
        }

        if (closestOptimizableVillager == null) return;

        if (closestOptimizableVillager.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.WORKSTATION_COOLDOWN.get())) {
            closestOptimizableVillager.setOptimization(OptimizationType.WORKSTATION);
            closestOptimizableVillager.saveOptimizeTime();
            if (shouldNotifyPlayer) {
                final String villagerType = closestOptimizableVillager.villager().getProfession().toString().toLowerCase();
                final String workstation = placed.getType().toString().toLowerCase();
                VillagerOptimizer.getLang(player.locale()).workstation_optimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%workstation%").replacement(workstation).build())
                ));
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info(event.getPlayer().getName() + " optimized a villager using workstation: '" + placed.getType().toString().toLowerCase() + "'");
        } else {
            closestOptimizableVillager.villager().shakeHead();
            if (shouldNotifyPlayer) {
                final String timeLeft = CommonUtils.formatTime(closestOptimizableVillager.getOptimizeCooldownMillis(cooldown));
                VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build())
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block placed = event.getBlock();
        if (!workstations_that_disable.contains(placed.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closestOptimizedVillager = null;
        double closestDistance = search_radius;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;

            WrappedVillager wVillager = villagerManager.getOrAdd(villager);
            final double distance = entity.getLocation().distance(workstationLoc);

            if (wVillager.isOptimized() && distance < closestDistance) {
                closestOptimizedVillager = wVillager;
                closestDistance = distance;
            }
        }

        if (closestOptimizedVillager == null) return;

        if (closestOptimizedVillager.getOptimizationType().equals(OptimizationType.WORKSTATION)) {
            if (shouldNotifyPlayer) {
                final String villagerType = closestOptimizedVillager.villager().getProfession().toString().toLowerCase();
                final String workstation = placed.getType().toString().toLowerCase();
                VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%workstation%").replacement(workstation).build())
                ));
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info(event.getPlayer().getName() + " unoptimized a villager by breaking workstation: '" + placed.getType().toString().toLowerCase() + "'");
        }
    }
}
