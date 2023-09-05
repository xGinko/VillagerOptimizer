package me.xginko.villageroptimizer.models;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.Keys;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public record WrappedVillager(Villager villager) {

    public static WrappedVillager fromVillager(Villager villager) {
        return VillagerOptimizer.getVillagerCache().get(villager);
    }

    public boolean isOptimized() {
        return villager.getPersistentDataContainer().has(Keys.OPTIMIZED.key());
    }

    public void setOptimization(OptimizationType type) {
        if (type.equals(OptimizationType.OFF) && isOptimized()) {
            villager.getPersistentDataContainer().remove(Keys.OPTIMIZED.key());
            villager.setAware(true);
            villager.setAI(true);
        } else {
            villager.getPersistentDataContainer().set(Keys.OPTIMIZED.key(), PersistentDataType.STRING, type.name());
            villager.setAware(false);
        }
    }

    public OptimizationType getOptimizationType() {
        return isOptimized() ? OptimizationType.valueOf(villager().getPersistentDataContainer().get(Keys.OPTIMIZED.key(), PersistentDataType.STRING)) : OptimizationType.OFF;
    }

    public void setRestockCooldown(long milliseconds) {
        villager.getPersistentDataContainer().set(Keys.COOLDOWN_RESTOCK.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public boolean shouldRestock() {
        PersistentDataContainer villagerData = villager.getPersistentDataContainer();
        return villagerData.has(Keys.COOLDOWN_RESTOCK.key(), PersistentDataType.LONG) && villagerData.get(Keys.COOLDOWN_RESTOCK.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public void restock() {
        villager.getRecipes().forEach(recipe -> recipe.setUses(0));
    }

    public void setExpCooldown(long milliseconds) {
        villager.getPersistentDataContainer().set(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public boolean isOnExpCooldown() {
        PersistentDataContainer villagerData = villager.getPersistentDataContainer();
        return villagerData.has(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) && villagerData.get(Keys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public void saveWorldTime() {
        villager.getPersistentDataContainer().set(Keys.WORLDTIME.key(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    public long getSavedWorldTime() {
        PersistentDataContainer villagerData = villager.getPersistentDataContainer();
        return villagerData.has(Keys.WORLDTIME.key(), PersistentDataType.LONG) ? villagerData.get(Keys.WORLDTIME.key(), PersistentDataType.LONG) : villager.getWorld().getFullTime();
    }
}
