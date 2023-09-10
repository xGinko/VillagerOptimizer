package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import me.xginko.villageroptimizer.utils.LogUtils;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashSet;
import java.util.List;

public class BlockOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final HashSet<Material> blocks_that_disable = new HashSet<>(4);
    private final boolean shouldLog, shouldNotifyPlayer;
    private final int maxVillagers;
    private final long cooldown;

    protected BlockOptimization() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization.methods.by-specific-block.enable", """
                When enabled, villagers standing on the configured specific blocks will become optimized once a\s
                player interacts with them. If the block is broken or moved, the villager will become unoptimized\s
                again once a player interacts with the villager afterwards.
                """);
        config.getList("optimization.methods.by-specific-block.materials", List.of(
                "LAPIS_BLOCK", "GLOWSTONE", "IRON_BLOCK"
        ), "Values here need to be valid bukkit Material enums for your server version."
        ).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("optimization.methods.by-specific-block", configuredMaterial);
            }
        });
        this.cooldown = config.getInt("optimization.methods.by-specific-block.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again by using this method. \s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.
                """) * 1000L;
        this.maxVillagers = config.getInt("optimization.methods.by-specific-block.max-villagers-per-block", 3,
                "How many villagers can be optimized at once by placing a block under them.");
        this.shouldNotifyPlayer = config.getBoolean("optimization.methods.by-specific-block.notify-player", true);
        this.shouldLog = config.getBoolean("optimization.methods.by-specific-block.log", false);
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.methods.by-specific-block.enable", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        if (!blocks_that_disable.contains(placed.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;

        int counter = 0;
        for (Entity entity : placed.getRelative(BlockFace.UP).getLocation().getNearbyEntities(0.5,1,0.5)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;

            WrappedVillager wVillager = villagerManager.getOrAdd((Villager) entity);
            if (wVillager.isOptimized()) continue;
            if (counter >= maxVillagers) return;

            if (wVillager.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.BLOCK_COOLDOWN.get())) {
                wVillager.setOptimization(OptimizationType.BLOCK);
                wVillager.saveOptimizeTime();
                counter++;
                if (shouldNotifyPlayer) {
                    final String villagerType = wVillager.villager().getProfession().toString().toLowerCase();
                    final String placedType = placed.getType().toString().toLowerCase();
                    VillagerOptimizer.getLang(player.locale()).block_optimize_success.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(placedType).build())
                    ));
                }
                if (shouldLog)
                    VillagerOptimizer.getLog().info("Villager was optimized by block at "+wVillager.villager().getLocation());
            } else {
                wVillager.villager().shakeHead();
                if (shouldNotifyPlayer) {
                    final String timeLeft = CommonUtils.formatTime(wVillager.getOptimizeCooldownMillis(cooldown));
                    VillagerOptimizer.getLang(player.locale()).block_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build())));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (!blocks_that_disable.contains(broken.getType())) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;

        int counter = 0;
        for (Entity entity : broken.getRelative(BlockFace.UP).getLocation().getNearbyEntities(0.5,1,0.5)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;

            WrappedVillager wVillager = villagerManager.getOrAdd((Villager) entity);

            if (wVillager.getOptimizationType().equals(OptimizationType.BLOCK)) {
                if (counter >= maxVillagers) return;

                wVillager.setOptimization(OptimizationType.OFF);

                if (shouldNotifyPlayer) {
                    final String villagerType = wVillager.villager().getProfession().toString().toLowerCase();
                    final String brokenType = broken.getType().toString().toLowerCase();
                    VillagerOptimizer.getLang(player.locale()).block_unoptimize_success.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(brokenType).build())
                    ));
                }
                if (shouldLog)
                    VillagerOptimizer.getLog().info("Villager unoptimized because no longer standing on optimization block at "+wVillager.villager().getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEntityEvent event) {
        Entity interacted = event.getRightClicked();
        if (!interacted.getType().equals(EntityType.VILLAGER)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.BLOCK.get())) return;

        WrappedVillager wVillager = villagerManager.getOrAdd((Villager) interacted);
        final Location entityLegs = interacted.getLocation();

        if (
                blocks_that_disable.contains(entityLegs.getBlock().getType()) // check for blocks inside the entity's legs because of slabs and sink-in blocks
                || blocks_that_disable.contains(entityLegs.clone().subtract(0,1,0).getBlock().getType())
        ) {
            if (wVillager.isOptimized()) return;

            if (wVillager.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.BLOCK_COOLDOWN.get())) {
                wVillager.setOptimization(OptimizationType.BLOCK);
                wVillager.saveOptimizeTime();
                if (shouldNotifyPlayer) {
                    final String vilType = wVillager.villager().getProfession().toString().toLowerCase();
                    final String blockType = entityLegs.getBlock().getType().toString().toLowerCase();
                    VillagerOptimizer.getLang(player.locale()).block_optimize_success.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(vilType).build())
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(blockType).build())
                    ));
                }
                if (shouldLog)
                    VillagerOptimizer.getLog().info("Villager was optimized by block at "+wVillager.villager().getLocation());
            } else {
                wVillager.villager().shakeHead();
                if (shouldNotifyPlayer) {
                    final String timeLeft = CommonUtils.formatTime(wVillager.getOptimizeCooldownMillis(cooldown));
                    VillagerOptimizer.getLang(player.locale()).block_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build()))
                    );
                }
            }
        } else {
            if (wVillager.getOptimizationType().equals(OptimizationType.BLOCK)) {
                wVillager.setOptimization(OptimizationType.OFF);
                if (shouldNotifyPlayer) {
                    final String villagerType = wVillager.villager().getProfession().toString().toLowerCase();
                    final String blockType = entityLegs.getBlock().getType().toString().toLowerCase();
                    VillagerOptimizer.getLang(player.locale()).block_unoptimize_success.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(villagerType).build())
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(blockType).build())
                    ));
                }
                if (shouldLog)
                    VillagerOptimizer.getLog().info("Villager unoptimized because no longer standing on optimization block at "+wVillager.villager().getLocation());
            }
        }
    }
}
