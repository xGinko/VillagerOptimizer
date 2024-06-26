package me.xginko.villageroptimizer.modules;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.xginko.villageroptimizer.utils.LocationUtil;
import me.xginko.villageroptimizer.utils.Util;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VillagerChunkLimit extends VillagerOptimizerModule implements Runnable, Listener {

    private WrappedTask periodic_chunk_check;
    private final List<Villager.Profession> non_optimized_removal_priority, optimized_removal_priority;
    private final long check_period;
    private final int non_optimized_max_per_chunk, optimized_max_per_chunk;
    private final boolean log_enabled, skip_unloaded_chunks;

    protected VillagerChunkLimit() {
        super("villager-chunk-limit");
        config.master().addComment(configPath + ".enable",
                "Checks chunks for too many villagers and removes excess villagers based on priority.");
        this.check_period = config.getInt(configPath + ".check-period-in-ticks", 600,
                "Check all loaded chunks every X ticks. 1 second = 20 ticks\n" +
                "A shorter delay in between checks is more efficient but is also more resource intense.\n" +
                "A larger delay is less resource intense but could become inefficient.");
        this.skip_unloaded_chunks = config.getBoolean(configPath + ".skip-not-fully-loaded-chunks", true,
                "Does not check chunks that don't have their entities loaded.");
        this.log_enabled = config.getBoolean(configPath + ".log-removals", true);
        this.non_optimized_max_per_chunk = config.getInt(configPath + ".unoptimized.max-per-chunk", 20,
                "The maximum amount of unoptimized villagers per chunk.");
        final List<String> defaults = Stream.of(
                "NONE", "NITWIT", "SHEPHERD", "FISHERMAN", "BUTCHER", "CARTOGRAPHER", "LEATHERWORKER",
                "FLETCHER", "MASON", "FARMER", "ARMORER", "TOOLSMITH", "WEAPONSMITH", "CLERIC", "LIBRARIAN")
                .filter(profession -> {
                    try {
                        // Make sure no scary warnings appear when creating config defaults
                        Villager.Profession.valueOf(profession);
                        return true;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }).collect(Collectors.toList());
        this.non_optimized_removal_priority = config.getList(configPath + ".unoptimized.removal-priority", defaults,
                        "Professions that are in the top of the list are going to be scheduled for removal first.\n" +
                        "Use enums from https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html")
                .stream()
                .map(configuredProfession -> {
                    try {
                        return Villager.Profession.valueOf(configuredProfession);
                    } catch (IllegalArgumentException e) {
                        warn("(unoptimized) Villager profession '" + configuredProfession +
                             "' not recognized. Make sure you're using the correct profession enums from " +
                             "https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html.");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        this.optimized_max_per_chunk = config.getInt(configPath + ".optimized.max-per-chunk", 60,
                "The maximum amount of optimized villagers per chunk.");
        this.optimized_removal_priority = config.getList(configPath + ".optimized.removal-priority", defaults)
                .stream()
                .map(configuredProfession -> {
                    try {
                        return Villager.Profession.valueOf(configuredProfession);
                    } catch (IllegalArgumentException e) {
                        warn("(optimized) Villager profession '" + configuredProfession + "' not recognized. " +
                             "Make sure you're using the correct profession enums from " +
                             "https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html.");
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        periodic_chunk_check = scheduler.runTimer(this, check_period, check_period);
    }

    @Override
    public boolean shouldEnable() {
        return config.getBoolean(configPath + ".enable", false);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (periodic_chunk_check != null) periodic_chunk_check.cancel();
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (!skip_unloaded_chunks || Util.isChunkLoaded(chunk)) {
                    manageVillagerCount(chunk);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.VILLAGER) {
            manageVillagerCount(event.getEntity().getChunk());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            manageVillagerCount(event.getRightClicked().getChunk());
        }
    }

    private void manageVillagerCount(@NotNull Chunk chunk) {
        // Collect all optimized and unoptimized villagers in that chunk
        List<Villager> optimized_villagers = new ArrayList<>();
        List<Villager> not_optimized_villagers = new ArrayList<>();

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() != EntityType.VILLAGER) continue;

            Villager villager = (Villager) entity;

            if (villagerCache.createIfAbsent(villager).isOptimized()) {
                optimized_villagers.add(villager);
            } else {
                not_optimized_villagers.add(villager);
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
                scheduler.runAtEntity(villager, kill -> {
                    villager.remove();
                    if (log_enabled) info("Removed unoptimized villager with profession '" +
                            Util.formatEnum(villager.getProfession()) + "' at " + LocationUtil.toString(villager.getLocation()));
                });
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
                scheduler.runAtEntity(villager, kill -> {
                    villager.remove();
                    if (log_enabled) info("Removed optimized villager with profession '" +
                            Util.formatEnum(villager.getProfession()) + "' at " + LocationUtil.toString(villager.getLocation()));
                });
            }
        }
    }
}