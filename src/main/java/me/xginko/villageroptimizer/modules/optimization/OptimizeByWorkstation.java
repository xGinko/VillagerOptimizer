package me.xginko.villageroptimizer.modules.optimization;

import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.GenericUtil;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import net.kyori.adventure.text.Component;
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
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizeByWorkstation implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;
    private final VillagerCache villagerCache;
    private final long cooldown_millis;
    private final double search_radius;
    private final int check_duration_ticks;
    private final boolean only_while_sneaking, log_enabled, notify_player;

    public OptimizeByWorkstation() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("optimization-methods.workstation-optimization.enable",
                "When enabled, villagers that have a job and have been traded with at least once will become optimized,\n" +
                "if near their workstation. If the workstation is broken, the villager will become unoptimized again.");
        this.check_duration_ticks = Math.max(config.getInt("optimization-methods.workstation-optimization.check-linger-duration-ticks", 100,
                "After a workstation has been placed, the plugin will wait for the configured amount of time in ticks\n" +
                "for a villager to claim that workstation. Not recommended to go below 100 ticks."), 1);
        this.search_radius = config.getDouble("optimization-methods.workstation-optimization.search-radius-in-blocks", 2.0,
                "The radius in blocks a villager can be away from the player when he places a workstation.\n" +
                "The closest unoptimized villager to the player will be optimized.");
        this.cooldown_millis = TimeUnit.SECONDS.toMillis(
                Math.max(1, config.getInt("optimization-methods.workstation-optimization.optimize-cooldown-seconds", 600,
                "Cooldown in seconds until a villager can be optimized again using a workstation.\n" +
                "Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.")));
        this.only_while_sneaking = config.getBoolean("optimization-methods.workstation-optimization.only-when-sneaking", true,
                "Only optimize/unoptimize by workstation when player is sneaking during place or break. Useful for villager rolling.");
        this.notify_player = config.getBoolean("optimization-methods.workstation-optimization.notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.log_enabled = config.getBoolean("optimization-methods.workstation-optimization.log", false);
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization-methods.workstation-optimization.enable", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        final Block placed = event.getBlock();
        final Villager.Profession workstationProfession = GenericUtil.getWorkstationProfession(placed.getType());
        if (workstationProfession == null) return;

        final Player player = event.getPlayer();
        if (only_while_sneaking && !player.isSneaking()) return;
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;

        final Location workstationLoc = placed.getLocation();
        final AtomicInteger taskAliveTicks = new AtomicInteger(check_duration_ticks);

        scheduler.runAtLocationTimer(workstationLoc, lingeringRepeatingCheck -> {
            if (taskAliveTicks.getAndAdd(-10) <= 0) {
                lingeringRepeatingCheck.cancel();
                return;
            }

            final Iterator<WrappedVillager> villagerIterator = workstationLoc.getNearbyEntitiesByType(Villager.class, search_radius)
                    .stream()
                    .filter(villager -> villager.isAdult() && villager.getProfession() != Villager.Profession.NITWIT)
                    .map(villagerCache::getOrAdd)
                    .iterator();

            while (villagerIterator.hasNext()) {
                final WrappedVillager wrapped = villagerIterator.next();
                if (wrapped.villager().getProfession() != workstationProfession) continue;
                if (wrapped.getJobSite() == null) continue;
                if (wrapped.getJobSite().distanceSquared(workstationLoc) > 1) continue;

                if (!wrapped.canOptimize(cooldown_millis) && !player.hasPermission(Permissions.Bypass.WORKSTATION_COOLDOWN.get())) {
                    wrapped.sayNo();
                    if (notify_player) {
                        final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                                .matchLiteral("%time%")
                                .replacement(GenericUtil.formatDuration(Duration.ofMillis(wrapped.getOptimizeCooldownMillis(cooldown_millis))))
                                .build();
                        VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown
                                .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
                    }
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
                            .replacement(wrapped.villager().getProfession().toString().toLowerCase())
                            .build();
                    final TextReplacementConfig placedWorkstation = TextReplacementConfig.builder()
                            .matchLiteral("%workstation%")
                            .replacement(placed.getType().toString().toLowerCase())
                            .build();
                    VillagerOptimizer.getLang(player.locale()).workstation_optimize_success
                            .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(vilProfession).replaceText(placedWorkstation)));
                }

                if (log_enabled) {
                    VillagerOptimizer.getLog().info(Component.text(player.getName() +
                            " optimized villager by workstation (" + placed.getType().toString().toLowerCase() + ") at " +
                            GenericUtil.formatLocation(wrapped.villager().getLocation())).color(GenericUtil.COLOR));
                }

                lingeringRepeatingCheck.cancel();
                break;
            }
        }, 1L, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        final Block broken = event.getBlock();
        final Villager.Profession workstationProfession = GenericUtil.getWorkstationProfession(broken.getType());
        if (workstationProfession == null) return;

        final Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location workstationLoc = broken.getLocation();
        WrappedVillager closestOptimized = null;
        double closestDistance = Double.MAX_VALUE;

        for (Villager villager : workstationLoc.getNearbyEntitiesByType(Villager.class, search_radius)) {
            if (!villager.getProfession().equals(workstationProfession)) continue;
            final double distance = villager.getLocation().distanceSquared(workstationLoc);
            if (distance >= closestDistance) continue;

            WrappedVillager wrapped = villagerCache.getOrAdd(villager);

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
                    .replacement(closestOptimized.villager().getProfession().toString().toLowerCase())
                    .build();
            final TextReplacementConfig brokenWorkstation = TextReplacementConfig.builder()
                    .matchLiteral("%workstation%")
                    .replacement(broken.getType().toString().toLowerCase())
                    .build();
            VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success
                    .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(vilProfession).replaceText(brokenWorkstation)));
        }

        if (log_enabled) {
            VillagerOptimizer.getLog().info(Component.text(player.getName() +
                    " unoptimized villager by workstation (" + broken.getType().toString().toLowerCase() + ") at " +
                    GenericUtil.formatLocation(closestOptimized.villager().getLocation())).color(GenericUtil.COLOR));
        }
    }
}