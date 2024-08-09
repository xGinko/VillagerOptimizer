package me.xginko.villageroptimizer.wrapper;

import me.xginko.villageroptimizer.enums.Keyring;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrappedVillager extends PDCWrapper {

    private final @NotNull PDCWrapper[] pdcWrappers;

    public WrappedVillager(@NotNull Villager villager) {
        super(villager);
        this.pdcWrappers = PDCWrapper.forVillager(villager);
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
     * @return true if the villager can lose its acquired profession by having its workstation destroyed.
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
    public Keyring.Space getSpace() {
        return Keyring.Space.VillagerOptimizer;
    }

    @Override
    public boolean isOptimized() {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            if (pdcWrapper.isOptimized()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canOptimize(long cooldown_millis) {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            if (!pdcWrapper.canOptimize(cooldown_millis)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setOptimizationType(OptimizationType type) {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            pdcWrapper.setOptimizationType(type);
        }
    }

    @Override
    public @NotNull OptimizationType getOptimizationType() {
        OptimizationType result = OptimizationType.NONE;
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            OptimizationType type = pdcWrapper.getOptimizationType();
            if (type != OptimizationType.NONE) {
                if (pdcWrapper.getSpace() == Keyring.Space.VillagerOptimizer) {
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
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            pdcWrapper.saveOptimizeTime();
        }
    }

    @Override
    public long getOptimizeCooldownMillis(long cooldown_millis) {
        long cooldown = 0L;
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            cooldown = Math.max(cooldown, pdcWrapper.getOptimizeCooldownMillis(cooldown_millis));
        }
        return cooldown;
    }

    @Override
    public boolean canRestock(long cooldown_millis) {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            if (!pdcWrapper.canRestock(cooldown_millis)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void saveRestockTime() {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            pdcWrapper.saveRestockTime();
        }
    }

    @Override
    public long getRestockCooldownMillis(long cooldown_millis) {
        long cooldown = cooldown_millis;
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            cooldown = Math.max(cooldown, pdcWrapper.getRestockCooldownMillis(cooldown_millis));
        }
        return cooldown;
    }

    @Override
    public boolean canLevelUp(long cooldown_millis) {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            if (!pdcWrapper.canLevelUp(cooldown_millis)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void saveLastLevelUp() {
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            pdcWrapper.saveLastLevelUp();
        }
    }

    @Override
    public long getLevelCooldownMillis(long cooldown_millis) {
        long cooldown = cooldown_millis;
        for (PDCWrapper pdcWrapper : pdcWrappers) {
            cooldown = Math.max(cooldown, pdcWrapper.getLevelCooldownMillis(cooldown_millis));
        }
        return cooldown;
    }
}