package me.xginko.villageroptimizer.enums;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.NamespacedKey;

public enum Keys {

    OPTIMIZED(VillagerOptimizer.getKey("optimized")),
    COOLDOWN_OPTIMIZE(VillagerOptimizer.getKey("optimize-state-change-cooldown")),
    COOLDOWN_EXPERIENCE(VillagerOptimizer.getKey("experience-cooldown")),
    WORLDTIME(VillagerOptimizer.getKey("world-time"));

    private final NamespacedKey key;

    Keys(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }

}
