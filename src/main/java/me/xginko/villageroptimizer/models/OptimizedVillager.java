package me.xginko.villageroptimizer.models;

import org.bukkit.entity.Villager;

public class OptimizedVillager {

    private final Villager villager;


    public OptimizedVillager(Villager villager) {
        this.villager = villager;
    }

    public Villager villager() {
        return villager;
    }
}
