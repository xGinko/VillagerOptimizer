package me.xginko.villageroptimizer.enums;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public class Keys {

    public enum Origin {
        VillagerOptimizer,
        AntiVillagerLag;
    }

    public enum Own {
        OPTIMIZATION_TYPE("optimization-type"),
        LAST_OPTIMIZE("last-optimize"),
        LAST_LEVELUP("last-levelup"),
        LAST_RESTOCK("last-restock"),
        LAST_OPTIMIZE_NAME("last-optimize-name");

        private final NamespacedKey key;

        Own(String key) {
            this.key = getKey(key);
        }

        public NamespacedKey key() {
            return key;
        }

        /**
         * Returns a NamespacedKey created by VillagerOptimizer.
         *
         * @return a {@link NamespacedKey} that can be used to test for and read data stored by VillagerOptimizer
         * from a {@link PersistentDataContainer}
         */
        public static NamespacedKey getKey(String key) {
            return new NamespacedKey(VillagerOptimizer.getInstance(), key);
        }
    }

    public enum AntiVillagerLag {
        NEXT_OPTIMIZATION_SYSTIME_SECONDS("cooldown"), // Returns LONG -> System.currentTimeMillis() / 1000 + cooldown seconds
        LAST_RESTOCK_WORLDFULLTIME("time"), // Returns LONG -> villager.getWorld().getFullTime()
        NEXT_LEVELUP_SYSTIME_SECONDS("levelCooldown"), // Returns LONG -> System.currentTimeMillis() / 1000 + cooldown seconds
        OPTIMIZED_ANY("Marker"), // Returns STRING -> "AVL"
        OPTIMIZED_BLOCK("disabledByBlock"), // Returns STRING -> key().toString()
        OPTIMIZED_WORKSTATION("disabledByWorkstation"); // Returns STRING -> key().toString()

        private final NamespacedKey key;

        AntiVillagerLag(String avlKey) {
            this.key = getKey(avlKey);
        }

        public NamespacedKey key() {
            return key;
        }

        /**
         * Returns a NamespacedKey as if it was created by AntiVillagerLag.
         * This is possible because they are created using {@link NamespacedKey#NamespacedKey(Plugin, String)},
         * meaning the Namespace is always the return of {@link Plugin#getName()} && {@link String#toLowerCase()}
         * using {@link Locale#ROOT}
         *
         * @return a {@link NamespacedKey} that can be used to test for and read data stored by AntiVillagerLag
         * from a {@link PersistentDataContainer}
         */
        public static NamespacedKey getKey(String key) {
            return new NamespacedKey("AntiVillagerLag".toLowerCase(Locale.ROOT), key);
        }
    }
}
