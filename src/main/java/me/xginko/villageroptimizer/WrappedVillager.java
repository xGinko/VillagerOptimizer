package me.xginko.villageroptimizer;

import me.xginko.villageroptimizer.enums.Keys;
import me.xginko.villageroptimizer.enums.OptimizationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public final class WrappedVillager {

    private final @NotNull Villager villager;
    private final @NotNull PersistentDataContainer dataContainer;
    private final boolean parseOther;

    WrappedVillager(@NotNull Villager villager) {
        this.villager = villager;
        this.dataContainer = villager.getPersistentDataContainer();
        this.parseOther = VillagerOptimizer.getConfiguration().support_other_plugins;
    }

    /**
     * @return The villager inside the wrapper.
     */
    public @NotNull Villager villager() {
        return villager;
    }

    /**
     * @return The data container inside the wrapper.
     */
    public @NotNull PersistentDataContainer dataContainer() {
        return dataContainer;
    }

    /**
     * @return True if the villager is optimized by either this plugin or a supported alternative, otherwise false.
     */
    public boolean isOptimized() {
        if (!parseOther) {
            return isOptimized(Keys.Namespaces.VillagerOptimizer);
        }
        for (Keys.Namespaces pluginNamespaces : Keys.Namespaces.values()) {
            if (isOptimized(pluginNamespaces)) return true;
        }
        return false;
    }

    /**
     * @return True if the villager is optimized by the supported plugin, otherwise false.
     */
    public boolean isOptimized(Keys.Namespaces namespaces) {
        return switch (namespaces) {
            case VillagerOptimizer -> dataContainer.has(Keys.Own.OPTIMIZATION_TYPE.key(), PersistentDataType.STRING);
            case AntiVillagerLag -> dataContainer.has(Keys.AntiVillagerLag.OPTIMIZED_ANY.key(), PersistentDataType.STRING)
                    || dataContainer.has(Keys.AntiVillagerLag.OPTIMIZED_WORKSTATION.key(), PersistentDataType.STRING)
                    || dataContainer.has(Keys.AntiVillagerLag.OPTIMIZED_BLOCK.key(), PersistentDataType.STRING);
        };
    }

    /**
     * @param cooldown_millis The configured cooldown in millis until the next optimization is allowed to occur.
     * @return True if villager can be optimized again, otherwise false.
     */
    public boolean canOptimize(final long cooldown_millis) {
        if (parseOther) {
            if (
                    dataContainer.has(Keys.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.key(), PersistentDataType.LONG)
                    && System.currentTimeMillis() <= 1000 * dataContainer.get(Keys.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.key(), PersistentDataType.LONG)
            ) {
                return false;
            }
        }
        return System.currentTimeMillis() > getLastOptimize() + cooldown_millis;
    }

    /**
     * @param type OptimizationType the villager should be set to.
     */
    public void setOptimization(OptimizationType type) {
        VillagerOptimizer.getFoliaLib().getImpl().runAtEntityTimer(villager, setOptimization -> {
            // Keep repeating task until villager is no longer trading with a player
            if (villager.isTrading()) return;

            if (type.equals(OptimizationType.NONE) && isOptimized()) {
                if (!parseOther || isOptimized(Keys.Namespaces.VillagerOptimizer))
                    dataContainer.remove(Keys.Own.OPTIMIZATION_TYPE.key());
                villager.setAware(true);
                villager.setAI(true); // Done for stability so villager is guaranteed to wake up
            } else {
                dataContainer.set(Keys.Own.OPTIMIZATION_TYPE.key(), PersistentDataType.STRING, type.name());
                villager.setAware(false);
            }

            // End repeating task once logic is finished
            setOptimization.cancel();
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    /**
     * @return The current OptimizationType of the villager.
     */
    public @NotNull OptimizationType getOptimizationType() {
        if (!parseOther) {
            return getOptimizationType(Keys.Namespaces.VillagerOptimizer);
        }
        OptimizationType optimizationType = getOptimizationType(Keys.Namespaces.VillagerOptimizer);
        if (optimizationType != OptimizationType.NONE) {
            return optimizationType;
        }
        return getOptimizationType(Keys.Namespaces.AntiVillagerLag);
    }

    public @NotNull OptimizationType getOptimizationType(Keys.Namespaces namespaces) {
        return switch (namespaces) {
            case VillagerOptimizer -> {
                if (isOptimized(Keys.Namespaces.VillagerOptimizer)) {
                    yield OptimizationType.valueOf(dataContainer.get(Keys.Own.OPTIMIZATION_TYPE.key(), PersistentDataType.STRING));
                }
                yield OptimizationType.NONE;
            }
            case AntiVillagerLag -> {
                if (dataContainer.has(Keys.AntiVillagerLag.OPTIMIZED_BLOCK.key(), PersistentDataType.STRING)) {
                    yield OptimizationType.BLOCK;
                }
                if (dataContainer.has(Keys.AntiVillagerLag.OPTIMIZED_WORKSTATION.key(), PersistentDataType.STRING)) {
                    yield OptimizationType.WORKSTATION;
                }
                if (dataContainer.has(Keys.AntiVillagerLag.OPTIMIZED_ANY.key(), PersistentDataType.STRING)) {
                    yield OptimizationType.COMMAND; // Best we can do
                }
                yield OptimizationType.NONE;
            }
        };
    }

    /**
     * Saves the system time in millis when the villager was last optimized.
     */
    public void saveOptimizeTime() {
        dataContainer.set(Keys.Own.LAST_OPTIMIZE.key(), PersistentDataType.LONG, System.currentTimeMillis());
    }

    /**
     * @return The system time in millis when the villager was last optimized, 0L if the villager was never optimized.
     */
    public long getLastOptimize() {
        if (dataContainer.has(Keys.Own.LAST_OPTIMIZE.key(), PersistentDataType.LONG)) {
            return dataContainer.get(Keys.Own.LAST_OPTIMIZE.key(), PersistentDataType.LONG);
        }
        return 0L;
    }

    /**
     * Here for convenience so the remaining millis since the last stored optimize time
     * can be easily calculated.
     * This enables new configured cooldowns to instantly apply instead of them being persistent.
     *
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return The time left in millis until the villager can be optimized again.
     */
    public long getOptimizeCooldownMillis(final long cooldown_millis) {
        long remainingMillis = 0L;

        if (parseOther) {
            if (dataContainer.has(Keys.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.key(), PersistentDataType.LONG)) {
                remainingMillis = System.currentTimeMillis() - dataContainer.get(Keys.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.key(), PersistentDataType.LONG);
            }
        }

        if (remainingMillis > 0) return remainingMillis;

        if (dataContainer.has(Keys.Own.LAST_OPTIMIZE.key(), PersistentDataType.LONG)) {
            return System.currentTimeMillis() - (dataContainer.get(Keys.Own.LAST_OPTIMIZE.key(), PersistentDataType.LONG) + cooldown_millis);
        }

        return cooldown_millis;
    }

    /**
     * Here for convenience so the remaining millis since the last stored restock time
     * can be easily calculated.
     *
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return True if the villager has been loaded long enough.
     */
    public boolean canRestock(final long cooldown_millis) {
        return getLastRestock() + cooldown_millis <= villager.getWorld().getFullTime();
    }

    /**
     * Restock all trading recipes.
     */
    public void restock() {
        for (MerchantRecipe recipe : villager.getRecipes()) {
            recipe.setUses(0);
        }
    }

    /**
     * Saves the time of the in-game world when the entity was last restocked.
     */
    public void saveRestockTime() {
        dataContainer.set(Keys.Own.LAST_RESTOCK.key(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    /**
     * @return The time of the in-game world when the entity was last restocked.
     */
    public long getLastRestock() {
        long lastRestock = 0L;
        if (dataContainer.has(Keys.Own.LAST_RESTOCK.key(), PersistentDataType.LONG)) {
            lastRestock = dataContainer.get(Keys.Own.LAST_RESTOCK.key(), PersistentDataType.LONG);
        }
        if (parseOther) {
            if (dataContainer.has(Keys.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.key(), PersistentDataType.LONG)) {
                long lastAVLRestock = dataContainer.get(Keys.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.key(), PersistentDataType.LONG);
                if (lastRestock < lastAVLRestock) {
                    lastRestock = lastAVLRestock;
                }
            }
        }
        return lastRestock;
    }

    public long getRestockCooldownMillis(final long cooldown_millis) {
        return dataContainer.has(Keys.Own.LAST_RESTOCK.key(), PersistentDataType.LONG) ? (villager.getWorld().getFullTime() - (dataContainer.get(Keys.Own.LAST_RESTOCK.key(), PersistentDataType.LONG) + cooldown_millis)) : cooldown_millis;
    }

    /**
     * @return The level between 1-5 calculated from the villagers experience.
     */
    public int calculateLevel() {
        // https://minecraft.fandom.com/wiki/Trading#Mechanics
        int vilEXP = villager.getVillagerExperience();
        if (vilEXP >= 250) return 5;
        if (vilEXP >= 150) return 4;
        if (vilEXP >= 70) return 3;
        if (vilEXP >= 10) return 2;
        return 1;
    }

    /**
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return Whether the villager can be leveled up or not with the checked milliseconds
     */
    public boolean canLevelUp(final long cooldown_millis) {
        if (villager.getWorld().getFullTime() < getLastLevelUpTime() + cooldown_millis) {
            return false;
        }

        if (parseOther) {
            return !dataContainer.has(Keys.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.key(), PersistentDataType.LONG)
                    || System.currentTimeMillis() > dataContainer.get(Keys.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.key(), PersistentDataType.LONG) * 1000;
        }

        return true;
    }

    /**
     * Saves the time of the in-game world when the entity was last leveled up.
     */
    public void saveLastLevelUp() {
        dataContainer.set(Keys.Own.LAST_LEVELUP.key(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    /**
     * Here for convenience so the remaining millis since the last stored level-up time
     * can be easily calculated.
     *
     * @return The time of the in-game world when the entity was last leveled up.
     */
    public long getLastLevelUpTime() {
        return dataContainer.has(Keys.Own.LAST_LEVELUP.key(), PersistentDataType.LONG) ? dataContainer.get(Keys.Own.LAST_LEVELUP.key(), PersistentDataType.LONG) : 0L;
    }

    public long getLevelCooldownMillis(final long cooldown_millis) {
        return dataContainer.has(Keys.Own.LAST_LEVELUP.key(), PersistentDataType.LONG) ? (villager.getWorld().getFullTime() - (dataContainer.get(Keys.Own.LAST_LEVELUP.key(), PersistentDataType.LONG) + cooldown_millis)) : cooldown_millis;
    }

    public void memorizeName(final Component customName) {
        dataContainer.set(Keys.Own.LAST_OPTIMIZE_NAME.key(), PersistentDataType.STRING, MiniMessage.miniMessage().serialize(customName));
    }

    public @Nullable Component getMemorizedName() {
        return dataContainer.has(Keys.Own.LAST_OPTIMIZE_NAME.key(), PersistentDataType.STRING) ? MiniMessage.miniMessage().deserialize(dataContainer.get(Keys.Own.LAST_OPTIMIZE_NAME.key(), PersistentDataType.STRING)) : null;
    }

    public void forgetName() {
        dataContainer.remove(Keys.Own.LAST_OPTIMIZE_NAME.key());
    }
}