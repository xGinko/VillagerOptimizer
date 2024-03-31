package me.xginko.villageroptimizer.wrapper;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

public interface VillagerDataHandler {

    static VillagerDataHandler[] forVillager(Villager villager) {
        if (VillagerOptimizer.getConfiguration().support_other_plugins) {
            return new VillagerDataHandler[]{
                    new MainVillagerDataHandlerImpl(villager),
                    new AVLVillagerDataHandlerImpl(villager)
            };
        } else {
            return new VillagerDataHandler[]{ new MainVillagerDataHandlerImpl(villager) };
        }
    }

    /**
     * @return True if the DataHandle is this plugin's implementation.
     */
    boolean isMain();

    /**
     * @return True if the villager is optimized by plugin, otherwise false.
     */
    boolean isOptimized();

    /**
     * @param cooldown_millis The configured cooldown in millis until the next optimization is allowed to occur.
     * @return True if villager can be optimized again, otherwise false.
     */
    boolean canOptimize(long cooldown_millis);

    /**
     * @param type OptimizationType the villager should be set to.
     */
    void setOptimizationType(OptimizationType type);

    /**
     * @return The current OptimizationType of the villager.
     */
    @NotNull OptimizationType getOptimizationType();

    /**
     * Saves the system time when the villager was last optimized.
     */
    void saveOptimizeTime();

    /**
     * For convenience so the remaining millis since the last stored optimize time
     * can be easily calculated.
     * This enables new configured cooldowns to instantly apply instead of them being persistent.
     *
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return The time left in millis until the villager can be optimized again.
     */
    long getOptimizeCooldownMillis(long cooldown_millis);

    /**
     * For convenience so the remaining millis since the last stored restock time
     * can be easily calculated.
     *
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return True if the villager has been loaded long enough.
     */
    boolean canRestock(long cooldown_millis);

    /**
     * Saves the time of when the entity was last restocked.
     */
    void saveRestockTime();

    /**
     * For convenience so the remaining millis since the last stored restock time
     * can be easily calculated.
     * This enables new configured cooldowns to instantly apply instead of them being persistent.
     *
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return The time left in millis until the villager can be restocked again.
     */
    long getRestockCooldownMillis(long cooldown_millis);

    /**
     * @param cooldown_millis The configured cooldown in milliseconds you want to check against.
     * @return Whether the villager can be leveled up or not with the checked milliseconds
     */
    boolean canLevelUp(long cooldown_millis);

    /**
     * Saves the time of the in-game world when the entity was last leveled up.
     */
    void saveLastLevelUp();

    /**
     * Here for convenience so the remaining millis since the last stored level-up time
     * can be easily calculated.
     *
     * @return The time of the in-game world when the entity was last leveled up.
     */
    long getLevelCooldownMillis(long cooldown_millis);
}
