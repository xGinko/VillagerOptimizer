package me.xginko.villageroptimizer.modules;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public class VillagerChunkLimit implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private ScheduledTask periodic_chunk_check;
    private final List<Villager.Profession> removal_priority = new ArrayList<>(16);
    private final long check_period;
    private final int max_unoptimized_per_chunk, max_optimized_per_chunk;
    private final boolean log_enabled;

    protected VillagerChunkLimit() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("villager-chunk-limit.enable", """
                Checks chunks for too many villagers and removes excess villagers based on priority.\s
                Naturally, optimized villagers will be picked last since they don't affect performance\s
                as much as unoptimized villagers.""");
        this.max_unoptimized_per_chunk = config.getInt("villager-chunk-limit.max-unoptimized-per-chunk", 30,
                "The maximum amount of unoptimized villagers per chunk.");
        this.max_optimized_per_chunk = config.getInt("villager-chunk-limit.max-optimized-per-chunk", 20,
                "The maximum amount of optimized villagers per chunk.");
        this.check_period = config.getInt("villager-chunk-limit.check-period-in-ticks", 600, """
                Check all loaded chunks every X ticks. 1 second = 20 ticks\s
                A shorter delay in between checks is more efficient but is also more resource intense.\s
                A larger delay is less resource intense but could become inefficient.""");
        this.log_enabled = config.getBoolean("villager-chunk-limit.log-removals", false);
        config.getList("villager-chunk-limit.removal-priority", List.of(
                "NONE", "NITWIT", "SHEPHERD", "FISHERMAN", "BUTCHER", "CARTOGRAPHER", "LEATHERWORKER",
                "FLETCHER", "MASON", "FARMER", "ARMORER", "TOOLSMITH", "WEAPONSMITH", "CLERIC", "LIBRARIAN"
        ), """
                Professions that are in the top of the list are going to be scheduled for removal first.\s
                Use enums from https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html"""
        ).forEach(configuredProfession -> {
            try {
                Villager.Profession profession = Villager.Profession.valueOf(configuredProfession);
                this.removal_priority.add(profession);
            } catch (IllegalArgumentException e) {
                LogUtil.moduleLog(Level.WARNING, "villager-chunk-limit",
                        "Villager profession '"+configuredProfession+"' not recognized. " +
                                "Make sure you're using the correct profession enums from https://jd.papermc.io/paper/1.20/org/bukkit/entity/Villager.Profession.html.");
            }
        });
    }

    @Override
    public void enable() {
        final VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        final Server server = plugin.getServer();
        server.getPluginManager().registerEvents(this, plugin);
        this.periodic_chunk_check = server.getGlobalRegionScheduler().runAtFixedRate(plugin, periodic_chunk_check -> {
            for (World world : server.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    plugin.getServer().getRegionScheduler().run(
                            plugin, world, chunk.getX(), chunk.getZ(), check_chunk -> checkVillagersInChunk(chunk)
                    );
                }
            }
        }, check_period, check_period);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("villager-chunk-limit.enable", false);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (periodic_chunk_check != null) periodic_chunk_check.cancel();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity spawned = event.getEntity();
        if (spawned.getType().equals(EntityType.VILLAGER)) {
            checkVillagersInChunk(spawned.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked.getType().equals(EntityType.VILLAGER)) {
            checkVillagersInChunk(clicked.getChunk());
        }
    }

    private void checkVillagersInChunk(Chunk chunk) {
        // Create lists with all optimized and unoptimzed villagers in that chunk
        List<Villager> unoptimized_villagers = new ArrayList<>();
        List<Villager> optimized_villagers = new ArrayList<>();

        // Collect villagers accordingly
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                Villager villager = (Villager) entity;
                if (villagerCache.getOrAdd(villager).isOptimized()) {
                    optimized_villagers.add(villager);
                } else {
                    unoptimized_villagers.add(villager);
                }
            }
        }

        // Check if there are more unoptimized villagers in that chunk than allowed
        final int unoptimized_vils_too_many = unoptimized_villagers.size() - max_unoptimized_per_chunk;
        if (unoptimized_vils_too_many > 0) {
            // Sort villagers by profession priority
            unoptimized_villagers.sort(Comparator.comparingInt(this::getProfessionPriority));
            // Remove prioritized villagers that are too many
            for (int i = 0; i < unoptimized_vils_too_many; i++) {
                Villager villager = unoptimized_villagers.get(i);
                villager.remove();
                if (log_enabled) LogUtil.moduleLog(Level.INFO, "villager-chunk-limit",
                        "Removed unoptimized villager of profession type '"+villager.getProfession().name()+"' at "+villager.getLocation()
                );
            }
        }

        // Check if there are more optimized villagers in that chunk than allowed
        final int optimized_vils_too_many = optimized_villagers.size() - max_optimized_per_chunk;
        if (optimized_vils_too_many > 0) {
            // Sort villagers by profession priority
            optimized_villagers.sort(Comparator.comparingInt(this::getProfessionPriority));
            // Remove prioritized villagers that are too many
            for (int i = 0; i < optimized_vils_too_many; i++) {
                Villager villager = optimized_villagers.get(i);
                villager.remove();
                if (log_enabled) LogUtil.moduleLog(Level.INFO, "villager-chunk-limit",
                        "Removed optimized villager of profession type '"+villager.getProfession().name()+"' at "+villager.getLocation()
                );
            }
        }
    }

    private int getProfessionPriority(Villager villager) {
        final Villager.Profession profession = villager.getProfession();
        return removal_priority.contains(profession) ? removal_priority.indexOf(profession) : Integer.MAX_VALUE;
    }
}