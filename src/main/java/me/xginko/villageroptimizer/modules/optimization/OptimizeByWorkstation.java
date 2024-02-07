package me.xginko.villageroptimizer.modules.optimization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcoded.folialib.impl.ServerImplementation;
import io.papermc.paper.event.entity.EntityMoveEvent;
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
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OptimizeByWorkstation implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;
    private final VillagerCache villagerCache;
    private final Cache<UUID, Location> cachedVillagerJobSites;
    private final long cooldown_millis;
    private final double search_radius;
    private final boolean only_while_sneaking, log_enabled, notify_player;

    public OptimizeByWorkstation() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        this.villagerCache = VillagerOptimizer.getCache();
        this.cachedVillagerJobSites = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(2)).build();

        // Broken and unfinished, in progress

        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("optimization-methods.workstation-optimization.enable", """
                When enabled, villagers that have a job and have been traded with at least once will become optimized,\s
                if near their workstation. If the workstation is broken, the villager will become unoptimized again.""");
        this.search_radius = config.getDouble("optimization-methods.workstation-optimization.search-radius-in-blocks", 2.0, """
                The radius in blocks a villager can be away from the player when he places a workstation.\s
                The closest unoptimized villager to the player will be optimized.""") / 2;
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

    private @Nullable Location getJobSite(Villager villager) {
        Location jobSite = cachedVillagerJobSites.getIfPresent(villager.getUniqueId());
        if (jobSite == null) {
            jobSite = villager.getMemory(MemoryKey.JOB_SITE);
            cachedVillagerJobSites.put(villager.getUniqueId(), jobSite);
        }
        return jobSite;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onVillagerMove(EntityMoveEvent event) {
        if (!event.getEntityType().equals(EntityType.VILLAGER)) return;

        final Villager villager = (Villager) event.getEntity();

        if (villager.getProfession().equals(Villager.Profession.NONE)) return;
        if (CommonUtil.canLooseProfession(villager)) return;

        final Location jobSite = getJobSite(villager);
        if (jobSite == null) return;
        // Using distanceSquared is faster. 1*1=1 -> 1 block away from the workstation
        if (!(villager.getLocation().distanceSquared(jobSite) <= 1)) return;

        WrappedVillager wrappedVillager = villagerCache.getOrAdd(villager);

        if (wrappedVillager.canOptimize(cooldown_millis)) {
            wrappedVillager.setOptimizationType(OptimizationType.WORKSTATION);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onCareerChange(VillagerCareerChangeEvent event) {
        if (!event.getReason().equals(VillagerCareerChangeEvent.ChangeReason.EMPLOYED)) return;
        if (CommonUtil.canLooseProfession(event.getEntity())) return;

        WrappedVillager wrappedVillager = villagerCache.getOrAdd(event.getEntity());

        if (!wrappedVillager.isOptimized() && wrappedVillager.canOptimize(cooldown_millis)) {
            wrappedVillager.setOptimizationType(OptimizationType.WORKSTATION);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        Villager.Profession workstationProfession = CommonUtil.getWorkstationProfession(placed.getType());
        if (workstationProfession.equals(Villager.Profession.NONE)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(Optimize.WORKSTATION.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location workstationLoc = placed.getLocation().toCenterLocation();
        WrappedVillager villagerThatClaimedWorkstation = null;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            if (!villager.getProfession().equals(workstationProfession)) continue;
            // Ignore villagers that haven't been locked into a profession yet, so we don't disturb trade rollers
            if (CommonUtil.canLooseProfession(villager)) continue;
            Location jobSite = getJobSite(villager);
            if (jobSite == null) continue;
            if (!workstationLoc.equals(jobSite.toBlockLocation())) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);

            if (wVillager.canOptimize(cooldown_millis)) {
                villagerThatClaimedWorkstation = wVillager;
                break;
            }
        }

        if (villagerThatClaimedWorkstation == null) return;

        if (villagerThatClaimedWorkstation.canOptimize(cooldown_millis) || player.hasPermission(Bypass.WORKSTATION_COOLDOWN.get())) {
            VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(villagerThatClaimedWorkstation, OptimizationType.WORKSTATION, player, event.isAsynchronous());
            if (!optimizeEvent.callEvent()) return;

            villagerThatClaimedWorkstation.setOptimizationType(optimizeEvent.getOptimizationType());
            villagerThatClaimedWorkstation.saveOptimizeTime();

            if (notify_player) {
                final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                        .matchLiteral("%vil_profession%")
                        .replacement(villagerThatClaimedWorkstation.villager().getProfession().toString().toLowerCase())
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
            if (log_enabled)
                VillagerOptimizer.getLog().info(player.getName() + " optimized a villager using workstation: '" + placed.getType().toString().toLowerCase() + "'");
        } else {
            CommonUtil.shakeHead(villagerThatClaimedWorkstation.villager());
            if (notify_player) {
                final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                        .matchLiteral("%time%")
                        .replacement(CommonUtil.formatTime(villagerThatClaimedWorkstation.getOptimizeCooldownMillis(cooldown_millis)))
                        .build();
                VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                        .replaceText(timeLeft)
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block broken = event.getBlock();
        Villager.Profession workstationProfession = CommonUtil.getWorkstationProfession(broken.getType());
        if (workstationProfession.equals(Villager.Profession.NONE)) return;
        Player player = event.getPlayer();
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
            final double distance = entity.getLocation().distance(workstationLoc);

            if (distance < closestDistance && wVillager.canOptimize(cooldown_millis)) {
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
        if (log_enabled)
            VillagerOptimizer.getLog().info(player.getName() + " unoptimized a villager by breaking workstation: '" + broken.getType().toString().toLowerCase() + "'");
    }
}