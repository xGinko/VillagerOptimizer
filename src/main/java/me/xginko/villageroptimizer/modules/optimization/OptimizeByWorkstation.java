package me.xginko.villageroptimizer.modules.optimization;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.permissions.Bypass;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.permissions.Optimize;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.CommonUtil;
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

import java.util.concurrent.TimeUnit;

public class OptimizeByWorkstation implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final long cooldown_millis;
    private final double search_radius;
    private final boolean only_while_sneaking, log_enabled, notify_player;

    public OptimizeByWorkstation() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("optimization-methods.workstation-optimization.enable", """
                When enabled, the closest villager near a matching workstation being placed will be optimized.\s
                If a nearby matching workstation is broken, the villager will become unoptimized again.""");
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlock();
        Villager.Profession workstationProfession = getWorkstationProfession(placed.getType());
        if (workstationProfession.equals(Villager.Profession.NONE)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Optimize.WORKSTATION.get())) return;
        if (only_while_sneaking && !player.isSneaking()) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closestOptimizableVillager = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            if (!villager.getProfession().equals(workstationProfession)) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distance(workstationLoc);

            if (distance < closestDistance && wVillager.canOptimize(cooldown_millis)) {
                closestOptimizableVillager = wVillager;
                closestDistance = distance;
            }
        }

        if (closestOptimizableVillager == null) return;

        if (closestOptimizableVillager.canOptimize(cooldown_millis) || player.hasPermission(Bypass.WORKSTATION_COOLDOWN.get())) {
            VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(closestOptimizableVillager, OptimizationType.WORKSTATION, player, event.isAsynchronous());
            if (!optimizeEvent.callEvent()) return;

            closestOptimizableVillager.setOptimization(optimizeEvent.getOptimizationType());
            closestOptimizableVillager.saveOptimizeTime();

            if (notify_player) {
                final TextReplacementConfig vilProfession = TextReplacementConfig.builder()
                        .matchLiteral("%vil_profession%")
                        .replacement(closestOptimizableVillager.villager().getProfession().toString().toLowerCase())
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
            CommonUtil.shakeHead(closestOptimizableVillager.villager());
            if (notify_player) {
                final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                        .matchLiteral("%time%")
                        .replacement(CommonUtil.formatTime(closestOptimizableVillager.getOptimizeCooldownMillis(cooldown_millis)))
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
        Villager.Profession workstationProfession = getWorkstationProfession(broken.getType());
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

        closestOptimizedVillager.setOptimization(OptimizationType.NONE);

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

    private Villager.Profession getWorkstationProfession(final Material workstation) {
        return switch (workstation) {
            case BARREL -> Villager.Profession.FISHERMAN;
            case CARTOGRAPHY_TABLE -> Villager.Profession.CARTOGRAPHER;
            case SMOKER -> Villager.Profession.BUTCHER;
            case SMITHING_TABLE -> Villager.Profession.TOOLSMITH;
            case GRINDSTONE -> Villager.Profession.WEAPONSMITH;
            case BLAST_FURNACE -> Villager.Profession.ARMORER;
            case CAULDRON -> Villager.Profession.LEATHERWORKER;
            case BREWING_STAND -> Villager.Profession.CLERIC;
            case COMPOSTER -> Villager.Profession.FARMER;
            case FLETCHING_TABLE -> Villager.Profession.FLETCHER;
            case LOOM -> Villager.Profession.SHEPHERD;
            case LECTERN -> Villager.Profession.LIBRARIAN;
            case STONECUTTER -> Villager.Profession.MASON;
            default -> Villager.Profession.NONE;
        };
    }
}