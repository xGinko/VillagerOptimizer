package me.xginko.villageroptimizer.modules.optimization;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.LocationUtil;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizeByWorkstation extends VillagerOptimizerModule implements Listener {

    private final long cooldown_millis;
    private final double search_radius;
    private final int check_duration_ticks;
    private final boolean only_while_sneaking, log_enabled, notify_player;

    public OptimizeByWorkstation() {
        super("optimization-methods.workstation-optimization");
        config.master().addComment(configPath + ".enable",
                "When enabled, villagers that have a job and have been traded with at least once will become optimized,\n" +
                "if near their workstation. If the workstation is broken, the villager will become unoptimized again.");
        this.check_duration_ticks = Math.max(config.getInt(configPath + ".check-linger-duration-ticks", 100,
                "After a workstation has been placed, the plugin will wait for the configured amount of time in ticks\n" +
                "for a villager to claim that workstation. Not recommended to go below 100 ticks."), 1);
        this.search_radius = config.getDouble(configPath + ".search-radius-in-blocks", 2.0,
                "The radius in blocks a villager can be away from the player when he places a workstation.\n" +
                "The closest unoptimized villager to the player will be optimized.");
        this.cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, config.getInt(configPath + ".optimize-cooldown-seconds", 600,
                "Cooldown in seconds until a villager can be optimized again using a workstation.\n" +
                "Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.")));
        this.only_while_sneaking = config.getBoolean(configPath + ".only-when-sneaking", true,
                "Only optimize/unoptimize by workstation when player is sneaking during place or break. Useful for villager rolling.");
        this.notify_player = config.getBoolean(configPath + ".notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.log_enabled = config.getBoolean(configPath + ".log", false);
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
        return config.getBoolean(configPath + ".enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        final Block placed = event.getBlock();
        final Villager.Profession workstationProfession = Util.getWorkstationProfession(placed.getType());
        if (workstationProfession == null) return;

        final Player player = event.getPlayer();
        if (only_while_sneaking && !player.isSneaking()) return;
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;

        final Location workstationLoc = placed.getLocation();
        final AtomicBoolean taskComplete = new AtomicBoolean();
        final AtomicInteger taskAliveTicks = new AtomicInteger();

        scheduling.regionSpecificScheduler(workstationLoc).runAtFixedRate(repeatingTask -> {
            if (taskComplete.get() || taskAliveTicks.getAndAdd(10) > check_duration_ticks) {
                repeatingTask.cancel();
                return;
            }

            for (Villager villager : workstationLoc.getNearbyEntitiesByType(Villager.class, search_radius)) {
                if (villager.getProfession() != workstationProfession) continue;
                WrappedVillager wrapped = wrapperCache.get(villager);
                if (wrapped.getJobSite() == null) continue;
                if (wrapped.getJobSite().getWorld().getUID() != workstationLoc.getWorld().getUID()) continue;
                if (LocationUtil.relDistance3DSquared(wrapped.getJobSite(), workstationLoc) > 1) continue;

                if (!wrapped.canOptimize(cooldown_millis) && !player.hasPermission(Permissions.Bypass.WORKSTATION_COOLDOWN.get())) {
                    wrapped.sayNo();
                    if (notify_player) {
                        final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                                .matchLiteral("%time%")
                                .replacement(Util.formatDuration(Duration.ofMillis(wrapped.getOptimizeCooldownMillis(cooldown_millis))))
                                .build();
                        VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown
                                .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
                    }
                    taskComplete.set(true);
                    return;
                }

                VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(
                        wrapped,
                        OptimizationType.WORKSTATION,
                        player,
                        event.isAsynchronous()
                );

                if (!optimizeEvent.callEvent()) return;

                wrapped.setOptimizationType(optimizeEvent.getOptimizationType());
                wrapped.saveOptimizeTime();

                if (notify_player) {
                    final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                            .matchLiteral("%vil_profession%")
                            .replacement(Util.formatEnum(wrapped.villager().getProfession()))
                            .build();
                    final TextReplacementConfig placedWorkstation = TextReplacementConfig.builder()
                            .matchLiteral("%blocktype%")
                            .replacement(Util.formatEnum(placed.getType()))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).workstation_optimize_success
                            .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(vilProfession).replaceText(placedWorkstation)));
                }

                if (log_enabled) {
                    info(player.getName() + " optimized villager using workstation " + Util.formatEnum(placed.getType()) + " at " +
                         LocationUtil.toString(wrapped.villager().getLocation()));
                }

                taskComplete.set(true);
                return;
            }
        }, 1L, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        final Block broken = event.getBlock();
        final Villager.Profession workstationProfession = Util.getWorkstationProfession(broken.getType());
        if (workstationProfession == null) return;

        final Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location workstationLoc = broken.getLocation();
        WrappedVillager closestOptimized = null;
        double closestDistance = Double.MAX_VALUE;

        for (Villager villager : workstationLoc.getNearbyEntitiesByType(Villager.class, search_radius)) {
            if (!villager.getProfession().equals(workstationProfession)) continue;
            final double distance = LocationUtil.relDistance3DSquared(villager.getLocation(), workstationLoc);
            if (distance >= closestDistance) continue;

            WrappedVillager wrapped = wrapperCache.get(villager);

            if (wrapped.isOptimized()) {
                closestOptimized = wrapped;
                closestDistance = distance;
            }
        }

        if (closestOptimized == null) return;

        VillagerUnoptimizeEvent unOptimizeEvent = new VillagerUnoptimizeEvent(
                closestOptimized,
                player,
                OptimizationType.WORKSTATION,
                event.isAsynchronous()
        );

        if (!unOptimizeEvent.callEvent()) return;

        closestOptimized.setOptimizationType(OptimizationType.NONE);

        if (notify_player) {
            final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                    .matchLiteral("%vil_profession%")
                    .replacement(Util.formatEnum(closestOptimized.villager().getProfession()))
                    .build();
            final TextReplacementConfig brokenWorkstation = TextReplacementConfig.builder()
                    .matchLiteral("%blocktype%")
                    .replacement(Util.formatEnum(broken.getType()))
                    .build();
            VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success
                    .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(vilProfession).replaceText(brokenWorkstation)));
        }

        if (log_enabled) {
            info(player.getName() + " unoptimized villager using workstation " + Util.formatEnum(broken.getType()) + " at " +
                 LocationUtil.toString(closestOptimized.villager().getLocation()));
        }
    }
}