package me.xginko.villageroptimizer;

import me.xginko.villageroptimizer.enums.Keys;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class WrappedVillager {

    private final @NotNull Villager villager;
    private final @NotNull PersistentDataContainer dataContainer;

    WrappedVillager(@NotNull Villager villager) {
        this.villager = villager;
        this.dataContainer = this.villager.getPersistentDataContainer();
    }

    /**
     * @return The villager inside the wrapper.
     */
    public @NotNull Villager villager() {
        return villager;
    }

    /**
     * @return True if the villager is optimized by this or another plugin, otherwise false.
     */
    public boolean isOptimized() {
        return dataContainer.has(Keys.OPTIMIZATION_TYPE.key()) || !villager.isAware() || !villager.hasAI();
    }

    /**
     * @param cooldown_millis The configured cooldown in millis until the next optimization is allowed to occur.
     * @return True if villager can be optimized again, otherwise false.
     */
    public boolean canOptimize(final long cooldown_millis) {
        return getLastOptimize() + cooldown_millis <= System.currentTimeMillis();
    }

    /**
     * @param type OptimizationType the villager should be set to.
     */
    public void setOptimization(OptimizationType type) {
        if (type.equals(OptimizationType.NONE) && isOptimized()) {
            dataContainer.remove(Keys.OPTIMIZATION_TYPE.key());
            villager.getScheduler().run(VillagerOptimizer.getInstance(), enableAI -> {
                villager.setAware(true);
                villager.setAI(true);
            }, null);
        } else {
            dataContainer.set(Keys.OPTIMIZATION_TYPE.key(), PersistentDataType.STRING, type.name());
            villager.getScheduler().run(VillagerOptimizer.getInstance(), disableAI -> {
                villager.setAware(false);
            }, null);
        }
    }

    /**
     * @return The current OptimizationType of the villager.
     */
    public @NotNull OptimizationType getOptimizationType() {
        return isOptimized() ? OptimizationType.valueOf(dataContainer.get(Keys.OPTIMIZATION_TYPE.key(), PersistentDataType.STRING)) : OptimizationType.NONE;
    }

    /**
     * Saves the system time in millis when the villager was last optimized.
     */
    public void saveOptimizeTime() {
        dataContainer.set(Keys.LAST_OPTIMIZE.key(), PersistentDataType.LONG, System.currentTimeMillis());
    }

    /**
     * @return The system time in millis when the villager was last optimized, 0L if the villager was never optimized.
     */
    public long getLastOptimize() {
        return dataContainer.has(Keys.LAST_OPTIMIZE.key(), PersistentDataType.LONG) ? dataContainer.get(Keys.LAST_OPTIMIZE.key(), PersistentDataType.LONG) : 0L;
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
        return dataContainer.has(Keys.LAST_OPTIMIZE.key(), PersistentDataType.LONG) ? (System.currentTimeMillis() - (dataContainer.get(Keys.LAST_OPTIMIZE.key(), PersistentDataType.LONG) + cooldown_millis)) : cooldown_millis;
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
        villager.getRecipes().forEach(recipe -> recipe.setUses(0));
    }

    /**
     * Saves the time of the in-game world when the entity was last restocked.
     */
    public void saveRestockTime() {
        dataContainer.set(Keys.LAST_RESTOCK.key(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    /**
     * @return The time of the in-game world when the entity was last restocked.
     */
    public long getLastRestock() {
        return dataContainer.has(Keys.LAST_RESTOCK.key(), PersistentDataType.LONG) ? dataContainer.get(Keys.LAST_RESTOCK.key(), PersistentDataType.LONG) : 0L;
    }

    public long getRestockCooldownMillis(final long cooldown_millis) {
        return dataContainer.has(Keys.LAST_RESTOCK.key(), PersistentDataType.LONG) ? (villager.getWorld().getFullTime() - (dataContainer.get(Keys.LAST_RESTOCK.key(), PersistentDataType.LONG) + cooldown_millis)) : cooldown_millis;
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
     * @return The system time in millis when the villager was last optimized, 0L if the villager was never optimized.
     */
    public boolean canLevelUp(final long cooldown_millis) {
        return getLastLevelUpTime() + cooldown_millis <= villager.getWorld().getFullTime();
    }

    /**
     * Saves the time of the in-game world when the entity was last leveled up.
     */
    public void saveLastLevelUp() {
        dataContainer.set(Keys.LAST_LEVELUP.key(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    /**
     * Here for convenience so the remaining millis since the last stored level-up time
     * can be easily calculated.
     *
     * @return The time of the in-game world when the entity was last leveled up.
     */
    public long getLastLevelUpTime() {
        return dataContainer.has(Keys.LAST_LEVELUP.key(), PersistentDataType.LONG) ? dataContainer.get(Keys.LAST_LEVELUP.key(), PersistentDataType.LONG) : 0L;
    }

    public long getLevelCooldownMillis(final long cooldown_millis) {
        return dataContainer.has(Keys.LAST_LEVELUP.key(), PersistentDataType.LONG) ? (villager.getWorld().getFullTime() - (dataContainer.get(Keys.LAST_LEVELUP.key(), PersistentDataType.LONG) + cooldown_millis)) : cooldown_millis;
    }
}
