package me.xginko.villageroptimizer.enums;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.NamespacedKey;

public enum Keys {

    OPTIMIZATION_TYPE(VillagerOptimizer.getKey("optimization-type")),
    LAST_OPTIMIZE(VillagerOptimizer.getKey("last-optimize")),
    LAST_LEVELUP(VillagerOptimizer.getKey("last-levelup")),
    LAST_RESTOCK(VillagerOptimizer.getKey("last-restock")),
    LAST_OPTIMIZE_NAME(VillagerOptimizer.getKey("last-optimize-name"));

    private final NamespacedKey key;

    Keys(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }

}
