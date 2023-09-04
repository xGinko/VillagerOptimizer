package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.NamespacedKey;

public enum NamespacedKeys {

    COOLDOWN(VillagerOptimizer.getKey("cooldown")),
    TIME(VillagerOptimizer.getKey("time")),
    LEVEL_COOLDOWN(VillagerOptimizer.getKey("level-cooldown")),
    NAMETAG_DISABLED(VillagerOptimizer.getKey("nametag-disabled")),
    BLOCK_DISABLED(VillagerOptimizer.getKey("block-disabled")),
    WORKSTATION_DISABLED(VillagerOptimizer.getKey("workstation-disabled"));

    private final NamespacedKey key;

    NamespacedKeys(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey get() {
        return key;
    }
}
