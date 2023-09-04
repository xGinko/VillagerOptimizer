package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.models.WrappedVillager;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class VillagerCache {
    private final Cache<UUID, WrappedVillager> villagerCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();

    public VillagerCache(JavaPlugin plugin) {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, populateCache -> {
            for (World world : plugin.getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Villager villager) {
                        this.villagerCache.put(villager.getUniqueId(), new WrappedVillager(villager));
                    }
                }
            }
        });
    }

    public WrappedVillager getVillager(Villager villager) {
        WrappedVillager wrappedVillager = villagerCache.getIfPresent(villager.getUniqueId());
        if (wrappedVillager == null) wrappedVillager = new WrappedVillager(villager);
        this.villagerCache.put(villager.getUniqueId(), wrappedVillager);
        return wrappedVillager;
    }

    public Map<UUID, WrappedVillager> get() {
        return this.villagerCache.asMap();
    }
}
