package me.xginko.villageroptimizer.events;

import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerUnoptimizeEvent extends Event implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull WrappedVillager wrappedVillager;
    private final @NotNull OptimizationType unOptimizeType;
    private final @Nullable Player whoUnoptimized;
    private boolean isCancelled = false;

    public VillagerUnoptimizeEvent(@NotNull WrappedVillager wrappedVillager, @Nullable Player whoUnoptimized, @NotNull OptimizationType unOptimizeType, boolean isAsync) {
        super(isAsync);
        this.wrappedVillager = wrappedVillager;
        this.whoUnoptimized = whoUnoptimized;
        this.unOptimizeType = unOptimizeType;
    }

    public VillagerUnoptimizeEvent(@NotNull WrappedVillager wrappedVillager, @Nullable Player whoUnoptimized, @NotNull OptimizationType unOptimizeType) {
        this.wrappedVillager = wrappedVillager;
        this.whoUnoptimized = whoUnoptimized;
        this.unOptimizeType = unOptimizeType;
    }

    public @NotNull WrappedVillager getWrappedVillager() {
        return wrappedVillager;
    }

    public @Nullable Player getWhoUnoptimized() {
        return whoUnoptimized;
    }

    public @NotNull OptimizationType getWhichTypeUnoptimized() {
        return unOptimizeType;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
