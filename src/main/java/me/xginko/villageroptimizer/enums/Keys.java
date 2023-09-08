package me.xginko.villageroptimizer.enums;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.NamespacedKey;

public enum Keys {

    OPTIMIZATION(VillagerOptimizer.getKey("optimization")),
    LAST_OPTIMIZE(VillagerOptimizer.getKey("last-optimize")),
    LAST_LEVELUP(VillagerOptimizer.getKey("last-levelup")),
    LAST_RESTOCK(VillagerOptimizer.getKey("last-restock"));

    private final NamespacedKey key;

    Keys(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }

}
