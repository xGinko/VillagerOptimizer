package me.xginko.villageroptimizer.modules.optimization;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.CommonUtil;
import me.xginko.villageroptimizer.utils.LogUtil;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.List;

public class OptimizeByBlock implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final HashSet<Material> blocks_that_disable = new HashSet<>(4);
    private final long cooldown;
    private final double search_radius;
    private final boolean only_while_sneaking, notify_player, log_enabled;

    public OptimizeByBlock() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization-methods.block-optimization.enable", """
                When enabled, the closest villager standing near a configured block being placed will be optimized.\s
                If a configured block is broken nearby, the closest villager will become unoptimized again.""");
        config.getList("optimization-methods.block-optimization.materials", List.of(
                "LAPIS_BLOCK", "GLOWSTONE", "IRON_BLOCK"
        ), "Values here need to be valid bukkit Material enums for your server version."
        ).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtil.materialNotRecognized("block-optimization", configuredMaterial);
            }
        });
        this.cooldown = config.getInt("optimization-methods.block-optimization.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again by using specific blocks. \s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.""") * 1000L;
        this.search_radius = config.getDouble("optimization-methods.block-optimization.search-radius-in-blocks", 2.0, """
                The radius in blocks a villager can be away from the player when he places an optimize block.\s
                The closest unoptimized villager to the player will be optimized.""") / 2;
        this.only_while_sneaking = config.getBoolean("optimization-methods.block-optimization.only-when-sneaking", true,
                "Only optimize/unoptimize by workstation when player is sneaking during place or break.");
        this.notify_player = config.getBoolean("optimization-methods.block-optimization.notify-player", true,
                "Sends players a message when they successfully optimized or unoptimized a villager.");
        this.log_enabled = config.getBoolean("optimization-methods.block-optimization.log", false);
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("optimization-methods.block-optimization.enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        if (!blocks_that_disable.contains(placed.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

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

            if (distance < closestDistance && wVillager.canOptimize(cooldown)) {
                closestOptimizableVillager = wVillager;
                closestDistance = distance;
            }
        }

        if (closestOptimizableVillager == null) return;

        if (closestOptimizableVillager.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.BLOCK_COOLDOWN.get())) {
            VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(closestOptimizableVillager, OptimizationType.BLOCK, player, event.isAsynchronous());
            if (!optimizeEvent.callEvent()) return;

            closestOptimizableVillager.setOptimization(optimizeEvent.getOptimizationType());
            closestOptimizableVillager.saveOptimizeTime();

            if (notify_player) {
                final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                        .matchLiteral("%vil_profession%")
                        .replacement(closestOptimizableVillager.villager().getProfession().toString().toLowerCase())
                        .build();
                final TextReplacementConfig placedMaterial = TextReplacementConfig.builder()
                        .matchLiteral("%blocktype%")
                        .replacement(placed.getType().toString().toLowerCase())
                        .build();
                VillagerOptimizer.getLang(player.locale()).block_optimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(vilProfession)
                        .replaceText(placedMaterial)
                ));
            }
            if (log_enabled)
                VillagerOptimizer.getLog().info("Villager was optimized by block at "+closestOptimizableVillager.villager().getLocation());
        } else {
            if (notify_player) {
                final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                        .matchLiteral("%time%")
                        .replacement(CommonUtil.formatTime(closestOptimizableVillager.getOptimizeCooldownMillis(cooldown)))
                        .build();
                VillagerOptimizer.getLang(player.locale()).block_on_optimize_cooldown.forEach(line -> player.sendMessage(line.replaceText(timeLeft)));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (!blocks_that_disable.contains(broken.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location blockLoc = broken.getLocation();
        WrappedVillager closestOptimizedVillager = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : blockLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distance(blockLoc);

            if (distance < closestDistance && wVillager.isOptimized()) {
                closestOptimizedVillager = wVillager;
                closestDistance = distance;
            }
        }

        if (closestOptimizedVillager == null) return;

        VillagerUnoptimizeEvent unOptimizeEvent = new VillagerUnoptimizeEvent(closestOptimizedVillager, player, OptimizationType.BLOCK, event.isAsynchronous());
        if (!unOptimizeEvent.callEvent()) return;

        closestOptimizedVillager.setOptimization(OptimizationType.NONE);

        if (notify_player) {
            final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                    .matchLiteral("%vil_profession%")
                    .replacement(closestOptimizedVillager.villager().getProfession().toString().toLowerCase())
                    .build();
            final TextReplacementConfig brokenMaterial = TextReplacementConfig.builder()
                    .matchLiteral("%blocktype%")
                    .replacement(broken.getType().toString().toLowerCase())
                    .build();
            VillagerOptimizer.getLang(player.locale()).block_unoptimize_success.forEach(line -> player.sendMessage(line
                    .replaceText(vilProfession)
                    .replaceText(brokenMaterial)
            ));
        }
        if (log_enabled)
            VillagerOptimizer.getLog().info("Villager unoptimized because nearby optimization block broken at: "+closestOptimizedVillager.villager().getLocation());
    }
}