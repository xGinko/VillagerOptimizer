package me.xginko.villageroptimizer.enums;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public class KeyHolder {
    public enum Namespaces {
        VillagerOptimizer("VillagerOptimizer"),
        AntiVillagerLag("AntiVillagerLag");

        private final String pluginName;
        Namespaces(String pluginName) {
            this.pluginName = pluginName;
        }
        public String pluginName() {
            return pluginName;
        }
    }

    /**
     * Returns a NamespacedKey as if it was created by a specific plugin.
     * This is possible because they are created using {@link NamespacedKey#NamespacedKey(Plugin, String)},
     * meaning the Namespace is always the return of {@link Plugin#getName()} && {@link String#toLowerCase()}
     * using {@link Locale#ROOT}
     *
     * @param pluginName The plugin name as configured in plugin.yml, section name
     * @param key The key name
     *
     * @return a {@link NamespacedKey} that can be used to test for and read data stored by plugins
     * from a {@link PersistentDataContainer}
     */
    public static NamespacedKey getKey(String pluginName, String key) {
        return new NamespacedKey(pluginName.toLowerCase(Locale.ROOT), key);
    }

    public enum Own {
        OPTIMIZATION_TYPE("optimization-type"),
        LAST_OPTIMIZE("last-optimize"),
        LAST_LEVELUP("last-levelup"),
        LAST_RESTOCK("last-restock"),
        LAST_OPTIMIZE_NAME("last-optimize-name");

        private final NamespacedKey key;

        Own(String key) {
            this.key = KeyHolder.getKey(Namespaces.VillagerOptimizer.pluginName(), key);
        }

        public NamespacedKey key() {
            return key;
        }
    }

    public enum AntiVillagerLag {
        NEXT_OPTIMIZATION_SYSTIME_SECONDS("cooldown"), // Returns LONG -> (System.currentTimeMillis() / 1000) + cooldown seconds
        LAST_RESTOCK_WORLDFULLTIME("time"), // Returns LONG -> villager.getWorld().getFullTime()
        NEXT_LEVELUP_SYSTIME_SECONDS("levelCooldown"), // Returns LONG -> (System.currentTimeMillis() / 1000) + cooldown seconds
        OPTIMIZED_ANY("Marker"), // Returns STRING -> "AVL"
        OPTIMIZED_BLOCK("disabledByBlock"), // Returns STRING -> key().toString()
        OPTIMIZED_WORKSTATION("disabledByWorkstation"); // Returns STRING -> key().toString()

        private final NamespacedKey key;

        AntiVillagerLag(String avlKey) {
            this.key = KeyHolder.getKey(Namespaces.AntiVillagerLag.pluginName(), avlKey);
        }

        public NamespacedKey key() {
            return key;
        }
    }
}
