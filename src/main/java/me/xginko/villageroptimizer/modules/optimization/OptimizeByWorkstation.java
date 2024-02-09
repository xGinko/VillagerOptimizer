package me.xginko.villageroptimizer.modules.optimization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcoded.folialib.impl.ServerImplementation;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.permissions.Bypass;
import me.xginko.villageroptimizer.enums.permissions.Optimize;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.CommonUtil;
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
import org.bukkit.util.NumberConversions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class OptimizeByWorkstation implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;
    private final VillagerCache villagerCache;
    private final Cache<Location, WrappedTask> pending_optimizations;
    private final long cooldown_millis, delay_millis, resettable_delay_millis;
    private final double search_radius, search_radius_squared;
    private final boolean only_while_sneaking, log_enabled, notify_player;

    public OptimizeByWorkstation() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("optimization-methods.workstation-optimization.enable", """
                When enabled, villagers that have a job and have been traded with at least once will become optimized,\s
                if near their workstation. If the workstation is broken, the villager will become unoptimized again.""");
        this.delay_millis = Math.max(config.getInt("optimization-methods.workstation-optimization.delay.default-delay-in-ticks", 10, """
                The delay in ticks the plugin should wait before trying to optimize the closest villager on workstation place.\s
                Gives the villager time to claim the placed workstation. Minimum delay is 1 Tick (Not recommended)"""), 1) * 50L;
        this.resettable_delay_millis = Math.max(config.getInt("optimization-methods.workstation-optimization.delay.resettable-delay-in-ticks", 60, """
                The delay in ticks the plugin should wait before trying to optimize a villager that can loose its profession\s
                by having their workstation destroyed.\s
                Intended to fix issues while trade rolling."""), 1) * 50L;
        this.pending_optimizations = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(Math.max(resettable_delay_millis, delay_millis) + 500L))
                .build();
        this.search_radius = config.getDouble("optimization-methods.workstation-optimization.search-radius-in-blocks", 2.0, """
                The radius in blocks a villager can be away from the player when he places a workstation.\s
                The closest unoptimized villager to the player will be optimized.""");
        this.search_radius_squared = NumberConversions.square(search_radius);
        this.cooldown_millis = TimeUnit.SECONDS.toMillis(
                config.getInt("optimization-methods.workstation-optimization.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again using a workstation.\s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior."""));
        this.only_while_sneaking = config.getBoolean("optimization-methods.workstation-optimization.only-when-sneaking", true,
                "Only optimize/unoptimize by workstation when player is sneaking during place or break");
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
        final Villager.Profession workstationProfession = CommonUtil.getWorkstationProfession(placed.getType());
        if (workstationProfession.equals(Villager.Profession.NONE)) return;
        final Player player = event.getPlayer();
        if (!player.hasPermission(Optimize.WORKSTATION.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location workstationLoc = placed.getLocation().toCenterLocation();
        WrappedVillager toOptimize = null;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            if (!villager.getProfession().equals(workstationProfession)) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);

            final Location jobSite = wVillager.getJobSite();
            if (jobSite == null) continue;
            if (jobSite.distanceSquared(workstationLoc) > search_radius_squared) continue;

            if (wVillager.canOptimize(cooldown_millis)) {
                toOptimize = wVillager;
                break;
            }
        }

        if (toOptimize == null) return;
        WrappedVillager finalToOptimize = toOptimize;

        pending_optimizations.put(placed.getLocation(), scheduler.runAtLocationLater(workstationLoc, () -> {
            if (!finalToOptimize.canOptimize(cooldown_millis) && !player.hasPermission(Bypass.WORKSTATION_COOLDOWN.get())) {
                CommonUtil.shakeHead(finalToOptimize.villager());
                if (notify_player) {
                    final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                            .matchLiteral("%time%")
                            .replacement(CommonUtil.formatDuration(Duration.ofMillis(finalToOptimize.getOptimizeCooldownMillis(cooldown_millis))))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                            .replaceText(timeLeft)
                    ));
                }
                return;
            }

            VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(finalToOptimize, OptimizationType.WORKSTATION, player, event.isAsynchronous());
            if (!optimizeEvent.callEvent()) return;

            finalToOptimize.setOptimizationType(optimizeEvent.getOptimizationType());
            finalToOptimize.saveOptimizeTime();

            if (notify_player) {
                final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                        .matchLiteral("%vil_profession%")
                        .replacement(finalToOptimize.villager().getProfession().toString().toLowerCase())
                        .build();
                final TextReplacementConfig placedWorkstation = TextReplacementConfig.builder()
                        .matchLiteral("%workstation%")
                        .replacement(placed.getType().toString().toLowerCase())
                        .build();
                VillagerOptimizer.getLang(player.locale()).workstation_optimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(vilProfession)
                        .replaceText(placedWorkstation)
                ));
            }

            if (log_enabled) {
                VillagerOptimizer.getLog().info(Component.text(player.getName() +
                        " optimized villager by workstation (" + placed.getType().toString().toLowerCase() + ") at " +
                        CommonUtil.formatLocation(finalToOptimize.villager().getLocation())).color(VillagerOptimizer.plugin_style.color()));
            }
        }, toOptimize.canLooseProfession() ? resettable_delay_millis : delay_millis, TimeUnit.MILLISECONDS));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        final Block broken = event.getBlock();
        // Cancel any pending optimization for this block
        WrappedTask pendingOpt = pending_optimizations.getIfPresent(broken.getLocation());
        if (pendingOpt != null) pendingOpt.cancel();

        final Villager.Profession workstationProfession = CommonUtil.getWorkstationProfession(broken.getType());
        if (workstationProfession.equals(Villager.Profession.NONE)) return;
        final Player player = event.getPlayer();
        if (!player.hasPermission(Optimize.WORKSTATION.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location workstationLoc = broken.getLocation();
        WrappedVillager closestOptimizedVillager = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            if (!villager.getProfession().equals(workstationProfession)) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distanceSquared(workstationLoc);

            if (distance < closestDistance && wVillager.isOptimized()) {
                closestOptimizedVillager = wVillager;
                closestDistance = distance;
            }
        }

        if (closestOptimizedVillager == null) return;

        VillagerUnoptimizeEvent unOptimizeEvent = new VillagerUnoptimizeEvent(closestOptimizedVillager, player, OptimizationType.WORKSTATION, event.isAsynchronous());
        if (!unOptimizeEvent.callEvent()) return;

        closestOptimizedVillager.setOptimizationType(OptimizationType.NONE);

        if (notify_player) {
            final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                    .matchLiteral("%vil_profession%")
                    .replacement(closestOptimizedVillager.villager().getProfession().toString().toLowerCase())
                    .build();
            final TextReplacementConfig brokenWorkstation = TextReplacementConfig.builder()
                    .matchLiteral("%workstation%")
                    .replacement(broken.getType().toString().toLowerCase())
                    .build();
            VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success.forEach(line -> player.sendMessage(line
                    .replaceText(vilProfession)
                    .replaceText(brokenWorkstation)
            ));
        }

        if (log_enabled) {
            VillagerOptimizer.getLog().info(Component.text(player.getName() +
                    " unoptimized villager by workstation (" + broken.getType().toString().toLowerCase() + ") at " +
                    CommonUtil.formatLocation(closestOptimizedVillager.villager().getLocation())).color(VillagerOptimizer.plugin_style.color()));
        }
    }
}