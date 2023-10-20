package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public final class VillagerCache {

    private final @NotNull Cache<UUID, WrappedVillager> villagerCache;

    VillagerCache(long expireAfterWriteSeconds) {
        this.villagerCache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(expireAfterWriteSeconds)).build();
    }

    public @NotNull ConcurrentMap<UUID, WrappedVillager> cacheMap() {
        return this.villagerCache.asMap();
    }

    public @Nullable WrappedVillager get(@NotNull UUID uuid) {
        WrappedVillager wrappedVillager = this.villagerCache.getIfPresent(uuid);
        return wrappedVillager == null && Bukkit.getEntity(uuid) instanceof Villager villager ? add(villager) : wrappedVillager;
    }

    public @NotNull WrappedVillager getOrAdd(@NotNull Villager villager) {
        WrappedVillager wrappedVillager = this.villagerCache.getIfPresent(villager.getUniqueId());
        return wrappedVillager == null ? add(new WrappedVillager(villager)) : add(wrappedVillager);
    }

    public @NotNull WrappedVillager add(@NotNull WrappedVillager villager) {
        this.villagerCache.put(villager.villager().getUniqueId(), villager);
        return villager;
    }

    public @NotNull WrappedVillager add(@NotNull Villager villager) {
        return add(new WrappedVillager(villager));
    }

    public boolean contains(@NotNull UUID uuid) {
        return this.villagerCache.getIfPresent(uuid) != null;
    }

    public boolean contains(@NotNull WrappedVillager villager) {
        return this.villagerCache.getIfPresent(villager.villager().getUniqueId()) != null;
    }

    public boolean contains(@NotNull Villager villager) {
        return this.villagerCache.getIfPresent(villager.getUniqueId()) != null;
    }
}