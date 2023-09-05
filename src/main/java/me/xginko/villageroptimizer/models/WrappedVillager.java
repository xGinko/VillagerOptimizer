package me.xginko.villageroptimizer.models;

import me.xginko.villageroptimizer.enums.NamespacedKeys;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public record WrappedVillager(Villager villager) {

    public int level() {
        // Villager Level depending on their XP (source: https://minecraft.fandom.com/wiki/Trading#Mechanics)
        final int experience = villager.getVillagerExperience();
        if (experience >= 250) return 5;
        if (experience >= 150) return 4;
        if (experience >= 70) return 3;
        if (experience >= 10) return 2;
        return 1;
    }

    public boolean isOptimized() {
        return villager.getPersistentDataContainer().has(NamespacedKeys.OPTIMIZED.key());
    }

    public void setOptimization(OptimizationType type) {
        if (type.equals(OptimizationType.OFF) && isOptimized()) {
            villager.getPersistentDataContainer().remove(NamespacedKeys.OPTIMIZED.key());
        } else {
            villager.getPersistentDataContainer().set(NamespacedKeys.OPTIMIZED.key(), PersistentDataType.STRING, type.name());
        }
    }

    public OptimizationType getOptimizationType() {
        return isOptimized() ? OptimizationType.valueOf(villager().getPersistentDataContainer().get(NamespacedKeys.OPTIMIZED.key(), PersistentDataType.STRING)) : OptimizationType.OFF;
    }

    public void setRestockCooldown(long milliseconds) {
        villager.getPersistentDataContainer().set(NamespacedKeys.COOLDOWN_RESTOCK.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public boolean shouldRestock() {
        PersistentDataContainer villagerData = villager.getPersistentDataContainer();
        return villagerData.has(NamespacedKeys.COOLDOWN_RESTOCK.key(), PersistentDataType.LONG) && villagerData.get(NamespacedKeys.COOLDOWN_RESTOCK.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public void restock() {
        villager.getRecipes().forEach(recipe -> recipe.setUses(0));
    }

    public void setExpCooldown(long milliseconds) {
        villager.getPersistentDataContainer().set(NamespacedKeys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG, System.currentTimeMillis() + milliseconds);
    }

    public boolean isOnExpCooldown() {
        PersistentDataContainer villagerData = villager.getPersistentDataContainer();
        return villagerData.has(NamespacedKeys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) && villagerData.get(NamespacedKeys.COOLDOWN_EXPERIENCE.key(), PersistentDataType.LONG) <= System.currentTimeMillis();
    }

    public void saveWorldTime() {
        villager.getPersistentDataContainer().set(NamespacedKeys.GAME_TIME.key(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    public long getSavedWorldTime() {
        PersistentDataContainer villagerData = villager.getPersistentDataContainer();
        return villagerData.has(NamespacedKeys.GAME_TIME.key(), PersistentDataType.LONG) ? villagerData.get(NamespacedKeys.GAME_TIME.key(), PersistentDataType.LONG) : villager.getWorld().getFullTime();
    }
}
