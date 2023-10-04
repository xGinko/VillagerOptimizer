package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.utils.LogUtil;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public class VillagerChunkLimit implements VillagerOptimizerModule, Listener, Runnable {

    private final Server server;
    private final VillagerCache villagerCache;
    private final List<Villager.Profession> non_optimized_removal_priority = new ArrayList<>(16);
    private final List<Villager.Profession> optimized_removal_priority = new ArrayList<>(16);
    private final long check_period;
    private final int non_optimized_max_per_chunk, optimized_max_per_chunk;
    private final boolean log_enabled;

    protected VillagerChunkLimit() {
        shouldEnable();
        this.server = VillagerOptimizer.getInstance().getServer();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("villager-chunk-limit.enable", """
                Checks chunks for too many villagers and removes excess villagers based on priority.\s
                Naturally, optimized villagers will be picked last since they don't affect performance\s
                as much as unoptimized villagers.""");
        this.check_period = config.getInt("villager-chunk-limit.check-period-in-ticks", 600, """
                Check all loaded chunks every X ticks. 1 second = 20 ticks\s
                A shorter delay in between checks is more efficient but is also more resource intense.\s
                A larger delay is less resource intense but could become inefficient.""");
        this.log_enabled = config.getBoolean("villager-chunk-limit.log-removals", false);
        this.non_optimized_max_per_chunk = config.getInt("villager-chunk-limit.unoptimized.max-per-chunk", 20,
                "The maximum amount of unoptimized villagers per chunk.");
        config.getList("villager-chunk-limit.unoptimized.removal-priority", List.of(
                        "NONE", "NITWIT", "SHEPHERD", "FISHERMAN", "BUTCHER", "CARTOGRAPHER", "LEATHERWORKER",
                        "FLETCHER", "MASON", "FARMER", "ARMORER", "TOOLSMITH", "WEAPONSMITH", "CLERIC", "LIBRARIAN"
        ), """
                Professions that are in the top of the list are going to be scheduled for removal first.\s
                Use enums from https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html"""
        ).forEach(configuredProfession -> {
            try {
                Villager.Profession profession = Villager.Profession.valueOf(configuredProfession);
                this.non_optimized_removal_priority.add(profession);
            } catch (IllegalArgumentException e) {
                LogUtil.moduleLog(Level.WARNING, "villager-chunk-limit.unoptimized",
                        "Villager profession '"+configuredProfession+"' not recognized. " +
                                "Make sure you're using the correct profession enums from https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html.");
            }
        });
        this.optimized_max_per_chunk = config.getInt("villager-chunk-limit.optimized.max-per-chunk", 60,
                "The maximum amount of optimized villagers per chunk.");
        config.getList("villager-chunk-limit.optimized.removal-priority", List.of(
                "NONE", "NITWIT", "SHEPHERD", "FISHERMAN", "BUTCHER", "CARTOGRAPHER", "LEATHERWORKER",
                "FLETCHER", "MASON", "FARMER", "ARMORER", "TOOLSMITH", "WEAPONSMITH", "CLERIC", "LIBRARIAN"
        )).forEach(configuredProfession -> {
            try {
                Villager.Profession profession = Villager.Profession.valueOf(configuredProfession);
                this.optimized_removal_priority.add(profession);
            } catch (IllegalArgumentException e) {
                LogUtil.moduleLog(Level.WARNING, "villager-chunk-limit.optimized",
                        "Villager profession '"+configuredProfession+"' not recognized. " +
                                "Make sure you're using the correct profession enums from https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html.");
            }
        });
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        server.getPluginManager().registerEvents(this, plugin);
        server.getScheduler().scheduleSyncRepeatingTask(plugin, this, check_period, check_period);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("villager-chunk-limit.enable", false);
    }

    @Override
    public void run() {
        for (World world : server.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                this.manageVillagerCount(chunk);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity spawned = event.getEntity();
        if (spawned.getType().equals(EntityType.VILLAGER)) {
            this.manageVillagerCount(spawned.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked.getType().equals(EntityType.VILLAGER)) {
            this.manageVillagerCount(clicked.getChunk());
        }
    }

    private void manageVillagerCount(@NotNull Chunk chunk) {
        // Collect all optimized and unoptimized villagers in that chunk
        List<Villager> optimized_villagers = new ArrayList<>();
        List<Villager> not_optimized_villagers = new ArrayList<>();

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                Villager villager = (Villager) entity;
                if (villagerCache.getOrAdd(villager).isOptimized()) {
                    optimized_villagers.add(villager);
                } else {
                    not_optimized_villagers.add(villager);
                }
            }
        }

        // Check if there are more unoptimized villagers in that chunk than allowed
        final int not_optimized_villagers_too_many = not_optimized_villagers.size() - non_optimized_max_per_chunk;
        if (not_optimized_villagers_too_many > 0) {
            // Sort villagers by profession priority
            not_optimized_villagers.sort(Comparator.comparingInt(villager -> {
                final Villager.Profession profession = villager.getProfession();
                return non_optimized_removal_priority.contains(profession) ? non_optimized_removal_priority.indexOf(profession) : Integer.MAX_VALUE;
            }));
            // Remove prioritized villagers that are too many
            for (int i = 0; i < not_optimized_villagers_too_many; i++) {
                Villager villager = not_optimized_villagers.get(i);
                villager.remove();
                if (log_enabled) LogUtil.moduleLog(Level.INFO, "villager-chunk-limit",
                        "Removed unoptimized villager of profession type '"+villager.getProfession().name()+"' at "+villager.getLocation()
                );
            }
        }

        // Check if there are more optimized villagers in that chunk than allowed
        final int optimized_villagers_too_many = optimized_villagers.size() - optimized_max_per_chunk;
        if (optimized_villagers_too_many > 0) {
            // Sort villagers by profession priority
            optimized_villagers.sort(Comparator.comparingInt(villager -> {
                final Villager.Profession profession = villager.getProfession();
                return optimized_removal_priority.contains(profession) ? optimized_removal_priority.indexOf(profession) : Integer.MAX_VALUE;
            }));
            // Remove prioritized villagers that are too many
            for (int i = 0; i < optimized_villagers_too_many; i++) {
                Villager villager = optimized_villagers.get(i);
                villager.remove();
                if (log_enabled) LogUtil.moduleLog(Level.INFO, "villager-chunk-limit",
                        "Removed optimized villager of profession type '"+villager.getProfession().name()+"' at "+villager.getLocation()
                );
            }
        }
    }
}