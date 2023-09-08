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
    private final @NotNull PersistentDataContainer dataContainer;

    public WrappedVillager(@NotNull Villager villager) {
        this.villager = villager;
        this.dataContainer = this.villager.getPersistentDataContainer();
    }

    public @NotNull Villager villager() {
        return villager;
    }

    public static @NotNull WrappedVillager fromCache(Villager villager) {
        return VillagerOptimizer.getVillagerManager().getOrAdd(villager);
    }

    public boolean isOptimized() {
        return dataContainer.has(Keys.OPTIMIZED.key());
    }

    public boolean setOptimization(OptimizationType type) {
        if (type.equals(OptimizationType.OFF) && isOptimized()) {
            dataContainer.remove(Keys.OPTIMIZED.key());
            villager.setAware(true);
            villager.setAI(true);
        } else {
            if (isOnOptimizeCooldown()) return false;
            dataContainer.set(Keys.OPTIMIZED.key(), PersistentDataType.STRING, type.name());
            villager.setAware(false);
            setOptimizeCooldown(VillagerOptimizer.getConfiguration().state_change_cooldown);
        }
        return true;
    }

    public @NotNull OptimizationType getOptimizationType() {
        return isOptimized() ? OptimizationType.valueOf(dataContainer.get(Keys.OPTIMIZED.key(), PersistentDataType.STRING)) : OptimizationType.OFF;
    }

    public void setOptimizeCooldown(long milliseconds) {
        dataContainer.set(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public long getOptimizeCooldown() {
        return dataContainer.has(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) ? System.currentTimeMillis() - dataContainer.get(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) : 0L;
    }

    public boolean isOnOptimizeCooldown() {
        return dataContainer.has(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) && dataContainer.get(Keys.COOLDOWN_OPTIMIZE.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public void restock() {
        villager.getRecipes().forEach(recipe -> recipe.setUses(0));
    }

    public void setExpCooldown(long milliseconds) {
        dataContainer.set(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public boolean isOnExpCooldown() {
        return dataContainer.has(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) && dataContainer.get(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public long saveWorldTime() {
        final long worldTime = villager.getWorld().getFullTime();
        dataContainer.set(Keys.WORLDTIME.key(), PersistentDataType.LONG, worldTime);
        return worldTime;
    }

    public long getSavedWorldTime() {
        return dataContainer.has(Keys.WORLDTIME.key(), PersistentDataType.LONG) ? dataContainer.get(Keys.WORLDTIME.key(), PersistentDataType.LONG) : saveWorldTime();
    }
}
