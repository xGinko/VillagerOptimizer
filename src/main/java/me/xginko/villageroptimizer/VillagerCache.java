package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public final class VillagerCache {

    private final @NotNull Cache<UUID, WrappedVillager> villagerCache;

    public VillagerCache(long expireAfterWriteSeconds) {
        this.villagerCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(expireAfterWriteSeconds)).build();
    }

    public @NotNull ConcurrentMap<UUID, WrappedVillager> cacheMap() {
        return this.villagerCache.asMap();
    }

    public void clear() {
        this.villagerCache.asMap().clear();
    }

    public @NotNull WrappedVillager createIfAbsent(@NotNull Villager villager) {
        WrappedVillager wrappedVillager = this.villagerCache.getIfPresent(villager.getUniqueId());
        return wrappedVillager == null ? this.add(new WrappedVillager(villager)) : this.add(wrappedVillager);
    }

    public @NotNull WrappedVillager add(@NotNull WrappedVillager villager) {
        this.villagerCache.put(villager.villager().getUniqueId(), villager);
        return villager;
    }

    public @NotNull WrappedVillager add(@NotNull Villager villager) {
        return this.add(new WrappedVillager(villager));
    }

    public boolean contains(@NotNull UUID uuid) {
        return this.villagerCache.getIfPresent(uuid) != null;
    }

    public boolean contains(@NotNull WrappedVillager villager) {
        return this.contains(villager.villager().getUniqueId());
    }

    public boolean contains(@NotNull Villager villager) {
        return this.contains(villager.getUniqueId());
    }
}