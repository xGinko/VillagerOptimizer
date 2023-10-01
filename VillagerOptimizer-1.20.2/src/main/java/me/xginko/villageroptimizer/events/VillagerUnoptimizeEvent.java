package me.xginko.villageroptimizer.events;

import me.xginko.villageroptimizer.WrappedVillager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class VillagerUnoptimizeEvent extends Event implements Cancellable {

    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull WrappedVillager wrappedVillager;
    private boolean isCancelled = false;

    public VillagerUnoptimizeEvent(@NotNull WrappedVillager wrappedVillager, boolean isAsync) {
        super(isAsync);
        this.wrappedVillager = wrappedVillager;
    }

    public VillagerUnoptimizeEvent(@NotNull WrappedVillager wrappedVillager) {
        this.wrappedVillager = wrappedVillager;
    }

    public @NotNull WrappedVillager getWrappedVillager() {
        return wrappedVillager;
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
