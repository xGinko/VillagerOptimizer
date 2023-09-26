package me.xginko.villageroptimizer.modules.optimizations;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.List;

public class OptimizeByWorkstation implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final HashSet<Material> workstations_that_disable = new HashSet<>(14);
    private final boolean shouldLog, shouldNotifyPlayer;
    private final long cooldown;
    private final double search_radius;

    public OptimizeByWorkstation() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization-methods.workstation-optimization.enable", """
                        When enabled, villagers near a configured radius to a workstation specific to your config\s
                        will be optimized.""");
        config.getList("optimization-methods.workstation-optimization.workstation-materials", List.of(
                "COMPOSTER", "SMOKER", "BARREL", "LOOM", "BLAST_FURNACE", "BREWING_STAND", "CAULDRON",
                "FLETCHING_TABLE", "CARTOGRAPHY_TABLE", "LECTERN", "SMITHING_TABLE", "STONECUTTER", "GRINDSTONE"
        ), "Values here need to be valid bukkit Material enums for your server version."
        ).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.workstations_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtil.materialNotRecognized("workstation-optimization", configuredMaterial);
            }
        });
        this.search_radius = config.getDouble("optimization-methods.workstation-optimization.search-radius-in-blocks", 2.0, """
                The radius in blocks a villager can be away from the player when he places a workstation.\s
                The closest unoptimized villager to the player will be optimized.""") / 2;
        this.cooldown = config.getInt("optimization-methods.workstation-optimization.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again using a workstation.\s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.""") * 1000L;
        this.shouldNotifyPlayer = config.getBoolean("optimization-methods.workstation-optimization.notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.shouldLog = config.getBoolean("optimization-methods.workstation-optimization.log", false);
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
        final Material placedType = placed.getType();
        if (!workstations_that_disable.contains(placedType)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;
        Villager.Profession workstationProfession = getWorkstationProfession(placedType);
        if (workstationProfession.equals(Villager.Profession.NONE)) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closestOptimizableVillager = null;
        double closestDistance = Double.MAX_VALUE;


        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            if (!villager.getProfession().equals(workstationProfession)) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distance(workstationLoc);

            if (distance < closestDistance) {
                final OptimizationType type = wVillager.getOptimizationType();
                if (type.equals(OptimizationType.NONE) || type.equals(OptimizationType.COMMAND)) {
                    closestOptimizableVillager = wVillager;
                    closestDistance = distance;
                }
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
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%vil_profession%").replacement(villagerType).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%workstation%").replacement(workstation).build())
                ));
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info(player.getName() + " optimized a villager using workstation: '" + placed.getType().toString().toLowerCase() + "'");
        } else {
            closestOptimizableVillager.villager().shakeHead();
            if (shouldNotifyPlayer) {
                final String timeLeft = CommonUtil.formatTime(closestOptimizableVillager.getOptimizeCooldownMillis(cooldown));
                VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build())
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block placed = event.getBlock();
        final Material brokenType = placed.getType();
        if (!workstations_that_disable.contains(brokenType)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.WORKSTATION.get())) return;
        Villager.Profession workstationProfession = getWorkstationProfession(brokenType);
        if (workstationProfession.equals(Villager.Profession.NONE)) return;

        final Location workstationLoc = placed.getLocation();
        WrappedVillager closestOptimizedVillager = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : workstationLoc.getNearbyEntities(search_radius, search_radius, search_radius)) {
            if (!entity.getType().equals(EntityType.VILLAGER)) continue;
            Villager villager = (Villager) entity;
            if (!villager.getProfession().equals(workstationProfession)) continue;

            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            final double distance = entity.getLocation().distance(workstationLoc);

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
            final String workstation = placed.getType().toString().toLowerCase();
            VillagerOptimizer.getLang(player.locale()).workstation_unoptimize_success.forEach(line -> player.sendMessage(line
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%vil_profession%").replacement(villagerType).build())
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%workstation%").replacement(workstation).build())
            ));
        }
        if (shouldLog)
            VillagerOptimizer.getLog().info(player.getName() + " unoptimized a villager by breaking workstation: '" + placed.getType().toString().toLowerCase() + "'");
    }

    private Villager.Profession getWorkstationProfession(Material workstation) {
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
