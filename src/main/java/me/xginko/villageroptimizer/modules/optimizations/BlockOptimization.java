package me.xginko.villageroptimizer.modules.optimizations;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
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

public class BlockOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final HashSet<Material> blocks_that_disable = new HashSet<>(4);
    private final boolean shouldLog, shouldNotifyPlayer;
    private final long cooldown;
    private final double search_radius;

    public BlockOptimization() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization-methods.block-optimization.enable", """
                When enabled, villagers standing on the configured specific blocks will become optimized once a\s
                player interacts with them. If the block is broken or moved, the villager will become unoptimized\s
                again once a player interacts with the villager afterwards.""");
        config.getList("optimization-methods.block-optimization.materials", List.of(
                "LAPIS_BLOCK", "GLOWSTONE", "IRON_BLOCK"
        ), "Values here need to be valid bukkit Material enums for your server version."
        ).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("block-optimization", configuredMaterial);
            }
        });
        this.cooldown = config.getInt("optimization-methods.block-optimization.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again by using specific blocks. \s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.""") * 1000L;
        this.search_radius = config.getDouble("optimization-methods.block-optimization.search-radius-in-blocks", 2.0, """
                The radius in blocks a villager can be away from the player when he places an optimize block.\s
                The closest unoptimized villager to the player will be optimized.""") / 2;
        this.shouldNotifyPlayer = config.getBoolean("optimization-methods.block-optimization.notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.shouldLog = config.getBoolean("optimization-methods.block-optimization.log", false);
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization-methods.block-optimization.enable", false);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        if (!blocks_that_disable.contains(placed.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;

        final Location blockLoc = placed.getLocation();
        WrappedVillager closestOptimizableVillager = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : blockLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            final Villager.Profession profession = villager.getProfession();
            if (profession.equals(Villager.Profession.NONE) || profession.equals(Villager.Profession.NITWIT)) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distance(blockLoc);

            if (distance < closestDistance) {
                final OptimizationType type = wVillager.getOptimizationType();
                if (type.equals(OptimizationType.NONE) || type.equals(OptimizationType.COMMAND)) {
                    closestOptimizableVillager = wVillager;
                    closestDistance = distance;
                }
            }
        }

        if (closestOptimizableVillager == null) return;

        if (closestOptimizableVillager.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.BLOCK_COOLDOWN.get())) {
            closestOptimizableVillager.setOptimization(OptimizationType.BLOCK);
            closestOptimizableVillager.saveOptimizeTime();
            if (shouldNotifyPlayer) {
                final String villagerType = closestOptimizableVillager.villager().getProfession().toString().toLowerCase();
                final String placedType = placed.getType().toString().toLowerCase();
                VillagerOptimizer.getLang(player.locale()).block_optimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(placedType).build())
                ));
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info("Villager was optimized by block at "+closestOptimizableVillager.villager().getLocation());
        } else {
            closestOptimizableVillager.villager().shakeHead();
            if (shouldNotifyPlayer) {
                final String timeLeft = CommonUtils.formatTime(closestOptimizableVillager.getOptimizeCooldownMillis(cooldown));
                VillagerOptimizer.getLang(player.locale()).block_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build())));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (!blocks_that_disable.contains(broken.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;

        final Location blockLoc = broken.getLocation();
        WrappedVillager closestOptimizedVillager = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : blockLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distance(blockLoc);

            if (distance < closestDistance) {
                final OptimizationType type = wVillager.getOptimizationType();
                if (type.equals(OptimizationType.WORKSTATION) || type.equals(OptimizationType.COMMAND)) {
                    closestOptimizedVillager = wVillager;
                    closestDistance = distance;
                }
            }
        }

        if (closestOptimizedVillager == null) return;

        closestOptimizedVillager.setOptimization(OptimizationType.NONE);
        if (shouldNotifyPlayer) {
            final String villagerType = closestOptimizedVillager.villager().getProfession().toString().toLowerCase();
            final String brokenType = broken.getType().toString().toLowerCase();
            VillagerOptimizer.getLang(player.locale()).block_unoptimize_success.forEach(line -> player.sendMessage(line
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(brokenType).build())
            ));
        }
        if (shouldLog)
            VillagerOptimizer.getLog().info("Villager unoptimized because no longer standing on optimization block at "+closestOptimizedVillager.villager().getLocation());
    }
}
