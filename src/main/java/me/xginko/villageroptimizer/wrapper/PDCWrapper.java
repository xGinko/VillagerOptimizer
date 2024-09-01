package me.xginko.villageroptimizer.wrapper;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.struct.enums.Keyring;
import me.xginko.villageroptimizer.struct.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

public abstract class PDCWrapper {

    public final Villager villager;
    public final PersistentDataContainer dataContainer;

    public PDCWrapper(Villager villager) {
        this.villager = villager;
        this.dataContainer = villager.getPersistentDataContainer();
    }

    public static PDCWrapper[] forVillager(Villager villager) {
        if (VillagerOptimizer.config().support_other_plugins) {
            return new PDCWrapper[]{new PDCWrapperVO(villager), new PDCWrapperAVL(villager)};
        } else {
            return new PDCWrapper[]{new PDCWrapperVO(villager)};
        }
    }

    /**
     * @return The namespace of the handler
     */
    public abstract Keyring.Space getSpace();

    /**
     * @return True if the villager is optimized by plugin, otherwise false.
     */
    public abstract boolean isOptimized();

    /**
     * @param cooldown_millis The configured cooldown in millis until the next optimization is allowed to occur.
     * @return True if villager can be optimized again, otherwise false.
     */
    public abstract boolean canOptimize(long cooldown_millis);

    /**
     * @param type OptimizationType the villager should be set to.
     */
    public abstract void setOptimizationType(OptimizationType type);

    /**
     * @return The current OptimizationType of the villager.
     */
    @NotNull
    public abstract OptimizationType getOptimizationType();

    /**
     * Saves the system time when the villager was last optimized.
     */
    public abstract void saveOptimizeTime();

    /**
     * For convenience so the remaining millis since the last stored optimize time
     * can be easily calculated.
     * This enables new configured cooldowns to instantly apply instead of them being persistent.
     *
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return The time left in millis until the villager can be optimized again.
     */
    public abstract long getOptimizeCooldownMillis(long cooldown_millis);

    /**
     * Gets the time of the day in ticks when the entity was last restocked.
     * This value is affected by /time set
     * @return The time of the minecraft day (in ticks) when the villager was last restocked
     */
    public abstract long getLastRestockFullTime();

    /**
     * Saves the time of when the entity was last restocked.
     */
    public abstract void saveRestockTime();

    /**
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return Whether the villager can be leveled up or not with the checked milliseconds
     */
    public abstract boolean canLevelUp(long cooldown_millis);

    /**
     * Saves the time of the in-game world when the entity was last leveled up.
     */
    public abstract void saveLastLevelUp();

    /**
     * Here for convenience so the remaining millis since the last stored level-up time
     * can be easily calculated.
     *
     * @return The time of the in-game world when the entity was last leveled up.
     */
    public abstract long getLevelCooldownMillis(long cooldown_millis);
}
