package me.xginko.villageroptimizer.models;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.Keys;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class WrappedVillager {

    private final @NotNull Villager villager;
    private final @NotNull PersistentDataContainer villagerData;

    public WrappedVillager(@NotNull Villager villager) {
        this.villager = villager;
        this.villagerData = this.villager.getPersistentDataContainer();
    }

    public @NotNull Villager villager() {
        return villager;
    }

    public static @NotNull WrappedVillager fromCache(Villager villager) {
        return VillagerOptimizer.getVillagerManager().getOrAdd(villager);
    }

    public boolean isOptimized() {
        return villagerData.has(Keys.OPTIMIZED.key());
    }

    public @NotNull OptimizationType computeOptimization() {
        return VillagerOptimizer.computeOptimization(this);
    }

    public boolean setOptimization(OptimizationType type) {
        if (type.equals(OptimizationType.OFF) && isOptimized()) {
            villagerData.remove(Keys.OPTIMIZED.key());
            villager.setAware(true);
            villager.setAI(true);
        } else {
            if (isOnOptimizeCooldown()) return false;
            setOptimizeCooldown(VillagerOptimizer.getConfiguration().state_change_cooldown);
            villagerData.set(Keys.OPTIMIZED.key(), PersistentDataType.STRING, type.name());
            villager.setAware(false);
        }
        return true;
    }

    public @NotNull OptimizationType getOptimizationType() {
        return isOptimized() ? OptimizationType.valueOf(villagerData.get(Keys.OPTIMIZED.key(), PersistentDataType.STRING)) : OptimizationType.OFF;
    }

    public void setOptimizeCooldown(long milliseconds) {
        villagerData.set(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public long getOptimizeCooldown() {
        return villagerData.has(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) ? System.currentTimeMillis() - villagerData.get(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) : 0L;
    }

    public boolean isOnOptimizeCooldown() {
        return villagerData.has(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) && villagerData.get(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public void restock() {
        villager.getRecipes().forEach(recipe -> recipe.setUses(0));
    }

    public void setExpCooldown(long milliseconds) {
        villagerData.set(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public boolean isOnExpCooldown() {
        return villagerData.has(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) && villagerData.get(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public long saveWorldTime() {
        final long worldTime = villager.getWorld().getFullTime();
        villagerData.set(Keys.WORLDTIME.key(), PersistentDataType.LONG, worldTime);
        return worldTime;
    }

    public long getSavedWorldTime() {
        return villagerData.has(Keys.WORLDTIME.key(), PersistentDataType.LONG) ? villagerData.get(Keys.WORLDTIME.key(), PersistentDataType.LONG) : saveWorldTime();
    }
}
