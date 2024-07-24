package me.xginko.villageroptimizer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.xginko.villageroptimizer.utils.Disableable;
import me.xginko.villageroptimizer.utils.Enableable;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.UUID;

public final class WrapperCache implements Enableable, Disableable, Listener {

    private final @NotNull Cache<UUID, WrappedVillager> wrapperCache;

    public WrapperCache(Duration cacheDuration) {
        this.wrapperCache = Caffeine.newBuilder().expireAfterWrite(cacheDuration).build();
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
        this.wrapperCache.invalidateAll();
        this.wrapperCache.cleanUp();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDeath(EntityDeathEvent event) {
        this.wrapperCache.invalidate(event.getEntity().getUniqueId());
    }

    @SuppressWarnings("DataFlowIssue")
    public @NotNull WrappedVillager get(@NotNull Villager villager) {
        return this.wrapperCache.get(villager.getUniqueId(), k -> new WrappedVillager(villager));
    }
}