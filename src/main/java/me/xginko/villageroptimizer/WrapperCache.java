package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.utils.Disableable;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;

public final class WrapperCache implements Disableable {

    private final @NotNull Cache<UUID, WrappedVillager> wrapperCache;

    public WrapperCache(Duration cacheDuration) {
        this.wrapperCache = Caffeine.newBuilder().expireAfterWrite(cacheDuration).build();
    }

    @Override
    public void disable() {
        this.wrapperCache.invalidateAll();
        this.wrapperCache.cleanUp();
    }

    @SuppressWarnings("DataFlowIssue")
    public @NotNull WrappedVillager get(@NotNull Villager villager) {
        return this.wrapperCache.get(villager.getUniqueId(), k -> new WrappedVillager(villager));
    }
}