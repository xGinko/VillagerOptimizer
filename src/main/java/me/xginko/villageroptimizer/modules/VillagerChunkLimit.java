package me.xginko.villageroptimizer.modules;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.CachedVillagers;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.utils.LogUtils;
import org.bukkit.Chunk;
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

    /*
    * TODO: expand villager chunk limit with settings for optimized and unoptimized.
    * */

    private final VillagerOptimizer plugin;
    private final CachedVillagers cachedVillagers;
    private ScheduledTask scheduledTask;
    private final List<Villager.Profession> removalPriority = new ArrayList<>(16);
    private final int global_max_villagers_per_chunk, max_unoptimized_per_chunk, max_optimized_per_chunk;
    private final boolean logIsEnabled;
    private final long check_period;

    protected VillagerChunkLimit() {
        shouldEnable();
        this.plugin = VillagerOptimizer.getInstance();
        this.cachedVillagers = VillagerOptimizer.getCachedVillagers();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("villager-chunk-limit.enable", """
                Checks chunks for too many villagers and removes excess villagers based on priority.\s
                Naturally, optimized villagers will be picked last since they don't affect performance\s
                as much as unoptimized villagers.""");
        this.global_max_villagers_per_chunk = config.getInt("villager-chunk-limit.global-max-villagers-per-chunk", 50, """
                The total amount of villagers, no matter if optimized or unoptimized per chunk.\s
                You want this number to be minimum as high as the sum of optimized and unoptimized\s
                per chunk if you don't want to risk deleting the wrong villagers.""");
        this.max_unoptimized_per_chunk = config.getInt("villager-chunk-limit.max-unoptimized-per-chunk", 30,
                "The maximum amount of unoptimized villagers per chunk.");
        this.max_optimized_per_chunk = config.getInt("villager-chunk-limit.max-optimized-per-chunk", 20,
                "The maximum amount of optimized villagers per chunk.");
        this.check_period = config.getInt("villager-chunk-limit.check-period-in-ticks", 600,
                "Check all loaded chunks every X ticks. 1 second = 20 ticks");
        this.logIsEnabled = config.getBoolean("villager-chunk-limit.log-removals", false);
        config.getList("villager-chunk-limit.removal-priority", List.of(
                "NONE", "NITWIT", "SHEPHERD", "FISHERMAN", "BUTCHER", "CARTOGRAPHER", "LEATHERWORKER",
                "FLETCHER", "MASON", "FARMER", "ARMORER", "TOOLSMITH", "WEAPONSMITH", "CLERIC", "LIBRARIAN"
        ),
                "Professions that are in the top of the list are going to be scheduled for removal first."

        ).forEach(configuredProfession -> {
            try {
                Villager.Profession profession = Villager.Profession.valueOf(configuredProfession);
                this.removalPriority.add(profession);
            } catch (IllegalArgumentException e) {
                LogUtils.moduleLog(Level.WARNING, "villager-chunk-limit",
                        "Villager profession '"+configuredProfession+"' not recognized. Make sure you're using the correct profession enums.");
            }
        });
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.scheduledTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, periodic_chunk_check -> {
            plugin.getServer().getWorlds().forEach(world -> {
                for (Chunk chunk : world.getLoadedChunks())
                    plugin.getServer().getRegionScheduler().run(plugin, world, chunk.getX(), chunk.getZ(), check_chunk -> checkVillagersInChunk(chunk));
            });
        }, check_period, check_period);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("villager-chunk-limit.enable", false);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        if (scheduledTask != null) scheduledTask.cancel();
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
        // Create a list with all villagers in that chunk
        List<Villager> villagers_in_chunk = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType().equals(EntityType.VILLAGER)) {
                villagers_in_chunk.add((Villager) entity);
            }
        }

        // Check if there are more villagers in that chunk than allowed
        int amount_over_the_limit = villagers_in_chunk.size() - global_max_villagers_per_chunk;
        if (amount_over_the_limit <= 0) return;

        // Sort villager list by profession priority
        villagers_in_chunk.sort(Comparator.comparingInt(this::getProfessionPriority));

        // Remove prioritized villagers that are too many
        for (int i = 0; i < amount_over_the_limit; i++) {
            Villager villager = villagers_in_chunk.get(i);
            villager.remove();
            if (logIsEnabled) LogUtils.moduleLog(Level.INFO, "villager-chunk-limit",
                    "Removed villager of profession type '"+villager.getProfession()+"' at "+villager.getLocation());
        }
    }

    private int getProfessionPriority(Villager villager) {
        final Villager.Profession profession = villager.getProfession();
        return removalPriority.contains(profession) && !cachedVillagers.getOrAdd(villager).isOptimized() ? removalPriority.indexOf(profession) : Integer.MAX_VALUE;
    }
}
