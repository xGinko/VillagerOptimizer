package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
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

public class BlockOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final Config config;
    private final boolean shouldLog, shouldNotifyPlayer;

    protected BlockOptimization() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        this.config = VillagerOptimizer.getConfiguration();
        this.config.addComment("optimization.methods.by-specific-block.enable", """
                When enabled, villagers standing on the configured specific blocks will become optimized once a\s
                player interacts with them. If the block is broken or moved, the villager will become unoptimized\s
                again once a player interacts with the villager afterwards.
                """);
        this.shouldLog = config.getBoolean("optimization.methods.by-specific-block.log", false);
        this.shouldNotifyPlayer = config.getBoolean("optimization.methods.by-specific-block.notify-player", true);
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
        return config.enable_block_optimization;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        if (!config.blocks_that_disable.contains(placed.getType())) return;

        placed.getRelative(BlockFace.UP).getLocation().getNearbyEntities(0.5,1,0.5).forEach(entity -> {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                WrappedVillager wVillager = villagerManager.getOrAdd((Villager) entity);
                if (!wVillager.isOptimized()) {
                    if (wVillager.setOptimization(OptimizationType.BLOCK)) {
                        if (shouldNotifyPlayer) {
                            Player player = event.getPlayer();
                            VillagerOptimizer.getLang(player.locale()).block_optimize_success.forEach(line -> player.sendMessage(line
                                    .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(wVillager.villager().getProfession().toString().toLowerCase()).build())
                                    .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(placed.getType().toString().toLowerCase()).build())
                            ));
                        }
                        if (shouldLog)
                            VillagerOptimizer.getLog().info("Villager was optimized by block at "+wVillager.villager().getLocation());
                    } else {
                        if (shouldNotifyPlayer) {
                            Player player = event.getPlayer();
                            VillagerOptimizer.getLang(player.locale()).block_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                                    .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(CommonUtils.formatTime(wVillager.getOptimizeCooldown())).build())));
                        }
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (!config.blocks_that_disable.contains(broken.getType())) return;

        broken.getRelative(BlockFace.UP).getLocation().getNearbyEntities(0.5,1,0.5).forEach(entity -> {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                WrappedVillager wVillager = villagerManager.getOrAdd((Villager) entity);
                if (wVillager.getOptimizationType().equals(OptimizationType.BLOCK)) {
                    wVillager.setOptimization(OptimizationType.OFF);
                    if (shouldNotifyPlayer) {
                        Player player = event.getPlayer();
                        VillagerOptimizer.getLang(player.locale()).block_unoptimize_success.forEach(line -> player.sendMessage(line
                                .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(wVillager.villager().getProfession().toString().toLowerCase()).build())
                                .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(broken.getType().toString().toLowerCase()).build())
                        ));
                    }
                    if (shouldLog)
                        VillagerOptimizer.getLog().info("Villager unoptimized because no longer standing on optimization block at "+wVillager.villager().getLocation());
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onPlayerInteract(PlayerInteractEntityEvent event) {
        Entity interacted = event.getRightClicked();
        if (!interacted.getType().equals(EntityType.VILLAGER)) return;

        WrappedVillager wVillager = villagerManager.getOrAdd((Villager) interacted);
        final Location entityLegs = interacted.getLocation();

        if (
                config.blocks_that_disable.contains(entityLegs.getBlock().getType()) // check for blocks inside the entity's legs because of slabs and sink-in blocks
                || config.blocks_that_disable.contains(entityLegs.clone().subtract(0,1,0).getBlock().getType())
        ) {
            if (!wVillager.isOptimized()) {
                if (wVillager.setOptimization(OptimizationType.BLOCK)) {
                    if (shouldNotifyPlayer) {
                        Player player = event.getPlayer();
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
                    if (shouldNotifyPlayer) {
                        Player player = event.getPlayer();
                        final long optimizeCoolDown = wVillager.getOptimizeCooldown();
                        VillagerOptimizer.getLang(player.locale()).block_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                                .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(CommonUtils.formatTime(optimizeCoolDown)).build()))
                        );
                    }
                }
            }
        } else {
            if (wVillager.getOptimizationType().equals(OptimizationType.BLOCK)) {
                wVillager.setOptimization(OptimizationType.OFF);
                if (shouldNotifyPlayer) {
                    Player player = event.getPlayer();
                    final String vilType = wVillager.villager().getProfession().toString().toLowerCase();
                    final String blockType = entityLegs.getBlock().getType().toString().toLowerCase();
                    VillagerOptimizer.getLang(player.locale()).block_unoptimize_success.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%villagertype%").replacement(vilType).build())
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%blocktype%").replacement(blockType).build())
                    ));
                }
                if (shouldLog)
                    VillagerOptimizer.getLog().info("Villager unoptimized because no longer standing on optimization block at "+wVillager.villager().getLocation());
            }
        }
    }
}
