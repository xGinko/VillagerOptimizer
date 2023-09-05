package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.models.WrappedVillager;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

public class VillagerCache {

    private final Cache<UUID, WrappedVillager> villagerCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).build();

    protected VillagerCache() {}

    public Collection<WrappedVillager> getAll() {
        return this.villagerCache.asMap().values();
    }

    public @Nullable WrappedVillager get(UUID uuid) {
        return villagerCache.getIfPresent(uuid);
    }

    public WrappedVillager getOrAddIfAbsent(Villager villager) {
        WrappedVillager wrappedVillager = villagerCache.getIfPresent(villager.getUniqueId());
        if (wrappedVillager == null) wrappedVillager = new WrappedVillager(villager);
        this.villagerCache.put(villager.getUniqueId(), wrappedVillager); // refresh cache
        return wrappedVillager;
    }

    public WrappedVillager add(WrappedVillager villager) {
        villagerCache.put(villager.villager().getUniqueId(), villager);
        return villager;
    }

    public WrappedVillager add(Villager villager) {
        WrappedVillager wrapped = new WrappedVillager(villager);
        villagerCache.put(villager.getUniqueId(), wrapped);
        return wrapped;
    }

    public boolean contains(WrappedVillager villager) {
        return villagerCache.getIfPresent(villager.villager().getUniqueId()) != null;
    }

    public boolean contains(Villager villager) {
        return villagerCache.getIfPresent(villager.getUniqueId()) != null;
    }
}
