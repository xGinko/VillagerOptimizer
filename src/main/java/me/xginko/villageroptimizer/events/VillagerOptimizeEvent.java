package me.xginko.villageroptimizer.events;

import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import me.xginko.villageroptimizer.struct.enums.OptimizationType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerOptimizeEvent extends Event implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull WrappedVillager wrappedVillager;
    private @NotNull OptimizationType optimizationType;
    private final @Nullable Player whoOptimised;
    private boolean isCancelled = false;

    public VillagerOptimizeEvent(
            @NotNull WrappedVillager wrappedVillager,
            @NotNull OptimizationType optimizationType,
            @Nullable Player whoOptimised,
            boolean isAsync
    ) throws IllegalArgumentException {
        super(isAsync);
        this.wrappedVillager = wrappedVillager;
        this.whoOptimised = whoOptimised;
        if (optimizationType.equals(OptimizationType.NONE)) {
            throw new IllegalArgumentException("OptimizationType can't be NONE.");
        } else {
            this.optimizationType = optimizationType;
        }
    }

    public VillagerOptimizeEvent(
            @NotNull WrappedVillager wrappedVillager,
            @NotNull OptimizationType optimizationType,
            @Nullable Player whoOptimised
    ) throws IllegalArgumentException {
        this.wrappedVillager = wrappedVillager;
        this.whoOptimised = whoOptimised;
        if (optimizationType.equals(OptimizationType.NONE)) {
            throw new IllegalArgumentException("OptimizationType can't be NONE.");
        } else {
            this.optimizationType = optimizationType;
        }
    }

    public @NotNull WrappedVillager getWrappedVillager() {
        return wrappedVillager;
    }

    public @NotNull OptimizationType getOptimizationType() {
        return optimizationType;
    }

    public void setOptimizationType(@NotNull OptimizationType optimizationType) throws IllegalArgumentException {
        if (optimizationType.equals(OptimizationType.NONE)) {
            throw new IllegalArgumentException("OptimizationType can't be NONE.");
        } else {
            this.optimizationType = optimizationType;
        }
    }

    public @Nullable Player getWhoOptimised() {
        return whoOptimised;
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
