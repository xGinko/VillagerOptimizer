package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.WrappedVillager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

public class VillagerManager {

    private final Cache<UUID, WrappedVillager> villagerCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();
    private final Config config;

    protected VillagerManager(JavaPlugin plugin) {
        this.config = VillagerOptimizer.getConfiguration();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, reCache -> {
            for (World world : plugin.getServer().getWorlds()) {
                for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                    this.villagerCache.put(villager.getUniqueId(), new WrappedVillager(villager));
                }
            }
        });
    }

    public Collection<WrappedVillager> getCachedVillagers() {
        return this.villagerCache.asMap().values();
    }

    public WrappedVillager wrap(Villager villager) {
        WrappedVillager wrappedVillager = villagerCache.getIfPresent(villager.getUniqueId());
        if (wrappedVillager == null) wrappedVillager = new WrappedVillager(villager);
        this.villagerCache.put(villager.getUniqueId(), wrappedVillager);
        return wrappedVillager;
    }

    public OptimizationType computeOptimization(Villager villager) {
        Component nameTag = villager.customName();
        if (
                nameTag != null
                && config.names_that_disable.contains(PlainTextComponentSerializer.plainText().serialize(nameTag).toLowerCase())
        ) {
            // Optimized by nametag

        }

        if (config.blocks_that_disable.contains(villager.getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) {
            // Optimized by Block

        }

        final Location jobSite = villager.getMemory(MemoryKey.JOB_SITE);
        if (
                jobSite != null
                && config.workstations_that_disable.contains(jobSite.getBlock().getType())
        ) {
            // Optimized by Workstation

        }
    }
}
