package me.xginko.villageroptimizer.enums;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.NamespacedKey;

public enum NamespacedKeys {

    OPTIMIZED(VillagerOptimizer.getKey("optimized")),
    COOLDOWN_RESTOCK(VillagerOptimizer.getKey("restock-cooldown")),
    COOLDOWN_EXPERIENCE(VillagerOptimizer.getKey("experience-cooldown")),
    GAME_TIME(VillagerOptimizer.getKey("game-time"));

    private final NamespacedKey key;

    NamespacedKeys(NamespacedKey key) {
        this.key = key;
    }

    public NamespacedKey key() {
        return key;
    }
}
