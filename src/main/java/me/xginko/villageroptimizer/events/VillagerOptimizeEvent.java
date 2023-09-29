package me.xginko.villageroptimizer.events;

import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class VillagerOptimizeEvent extends Event implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull WrappedVillager wrappedVillager;
    private @NotNull OptimizationType type;
    private boolean isCancelled = false;

    public VillagerOptimizeEvent(@NotNull WrappedVillager wrappedVillager, @NotNull OptimizationType type, boolean isAsync) throws IllegalArgumentException {
        super(isAsync);
        this.wrappedVillager = wrappedVillager;
        this.type = type;

        if (type.equals(OptimizationType.NONE)) {
            throw new IllegalArgumentException("Type can't be NONE.");
        }
    }

    public VillagerOptimizeEvent(@NotNull WrappedVillager wrappedVillager, @NotNull OptimizationType type) throws IllegalArgumentException {
        this.wrappedVillager = wrappedVillager;
        this.type = type;

        if (type.equals(OptimizationType.NONE)) {
            throw new IllegalArgumentException("Type can't be NONE.");
        }
    }

    public @NotNull WrappedVillager getWrappedVillager() {
        return wrappedVillager;
    }

    public @NotNull OptimizationType getOptimizationType() {
        return type;
    }

    public void setOptimizationType(@NotNull OptimizationType type) {
        this.type = type;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
