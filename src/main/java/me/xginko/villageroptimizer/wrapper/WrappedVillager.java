package me.xginko.villageroptimizer.wrapper;

import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrappedVillager implements VillagerDataHandler {

    private final @NotNull Villager villager;
    private final @NotNull VillagerDataHandler[] dataHandlers;

    public WrappedVillager(@NotNull Villager villager) {
        this.villager = villager;
        this.dataHandlers = VillagerDataHandler.forVillager(villager);
    }

    /**
     * @return The villager inside the wrapper.
     */
    public @NotNull Villager villager() {
        return villager;
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
     * @return true if the villager can loose his acquired profession by having their workstation destroyed.
     */
    public boolean canLooseProfession() {
        // A villager with a level of 1 and no trading experience is liable to lose its profession.
        return villager.getVillagerLevel() <= 1 && villager.getVillagerExperience() <= 0;
    }

    public void sayNo() {
        try {
            villager.shakeHead();
        } catch (NoSuchMethodError e) {
            villager.getWorld().playSound(villager.getEyeLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
        }
    }

    public @Nullable Location getJobSite() {
        return villager.getMemory(MemoryKey.JOB_SITE);
    }

    @Override
    public boolean isMain() {
        return false;
    }

    @Override
    public boolean isOptimized() {
        for (VillagerDataHandler handler : dataHandlers) {
            if (handler.isOptimized()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canOptimize(long cooldown_millis) {
        for (VillagerDataHandler handler : dataHandlers) {
            if (!handler.canOptimize(cooldown_millis)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setOptimizationType(OptimizationType type) {
        for (VillagerDataHandler handler : dataHandlers) {
            handler.setOptimizationType(type);
        }
    }

    @Override
    public @NotNull OptimizationType getOptimizationType() {
        OptimizationType result = OptimizationType.NONE;
        for (VillagerDataHandler handler : dataHandlers) {
            OptimizationType type = handler.getOptimizationType();
            if (type != OptimizationType.NONE) {
                if (handler.isMain()) {
                    return type;
                } else {
                    result = type;
                }
            }
        }
        return result;
    }

    @Override
    public void saveOptimizeTime() {
        for (VillagerDataHandler handler : dataHandlers) {
            handler.saveOptimizeTime();
        }
    }

    @Override
    public long getOptimizeCooldownMillis(long cooldown_millis) {
        long cooldown = cooldown_millis;
        for (VillagerDataHandler handler : dataHandlers) {
            cooldown = Math.max(cooldown, handler.getOptimizeCooldownMillis(cooldown_millis));
        }
        return cooldown;
    }

    @Override
    public boolean canRestock(long cooldown_millis) {
        boolean can_restock = true;
        for (VillagerDataHandler handler : dataHandlers) {
            if (!handler.canRestock(cooldown_millis)) {
                can_restock = false;
            }
        }
        return can_restock;
    }

    @Override
    public void saveRestockTime() {
        for (VillagerDataHandler handler : dataHandlers) {
            handler.saveRestockTime();
        }
    }

    @Override
    public long getRestockCooldownMillis(long cooldown_millis) {
        long cooldown = 0L;
        for (VillagerDataHandler handler : dataHandlers) {
            cooldown = Math.max(cooldown, handler.getRestockCooldownMillis(cooldown_millis));
        }
        return cooldown;
    }

    @Override
    public boolean canLevelUp(long cooldown_millis) {
        boolean can_level_up = true;
        for (VillagerDataHandler handler : dataHandlers) {
            if (!handler.canLevelUp(cooldown_millis)) {
                can_level_up = false;
            }
        }
        return can_level_up;
    }

    @Override
    public void saveLastLevelUp() {
        for (VillagerDataHandler handler : dataHandlers) {
            handler.saveLastLevelUp();
        }
    }

    @Override
    public long getLevelCooldownMillis(long cooldown_millis) {
        long cooldown = 0L;
        for (VillagerDataHandler handler : dataHandlers) {
            cooldown = Math.max(cooldown_millis, handler.getLevelCooldownMillis(cooldown_millis));
        }
        return cooldown;
    }
}