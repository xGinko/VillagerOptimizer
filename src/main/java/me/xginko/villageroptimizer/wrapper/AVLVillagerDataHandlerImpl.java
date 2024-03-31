package me.xginko.villageroptimizer.wrapper;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.Keyring;
import me.xginko.villageroptimizer.enums.OptimizationType;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class AVLVillagerDataHandlerImpl implements VillagerDataHandler {

    private final @NotNull Villager villager;
    private final @NotNull PersistentDataContainer dataContainer;

    AVLVillagerDataHandlerImpl(@NotNull Villager villager) {
        this.villager = villager;
        this.dataContainer = villager.getPersistentDataContainer();
    }

    @Override
    public boolean isMain() {
        return false;
    }

    @Override
    public boolean isOptimized() {
        return dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_ANY.getKey(), PersistentDataType.STRING)
                || dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_WORKSTATION.getKey(), PersistentDataType.STRING)
                || dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_BLOCK.getKey(), PersistentDataType.STRING);
    }

    @Override
    public boolean canOptimize(final long cooldown_millis) {
        return dataContainer.has(Keyring.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG)
                && System.currentTimeMillis() > TimeUnit.SECONDS.toMillis(dataContainer.get(Keyring.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG));
    }

    @Override
    public void setOptimizationType(final OptimizationType type) {
        VillagerOptimizer.getFoliaLib().getImpl().runAtEntityTimer(villager, setOptimization -> {
            // Keep repeating task until villager is no longer trading with a player
            if (villager.isTrading()) return;

            if (type == OptimizationType.NONE) {
                if (dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_ANY.getKey(), PersistentDataType.STRING))
                    dataContainer.remove(Keyring.AntiVillagerLag.OPTIMIZED_ANY.getKey());
                if (dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_WORKSTATION.getKey(), PersistentDataType.STRING))
                    dataContainer.remove(Keyring.AntiVillagerLag.OPTIMIZED_WORKSTATION.getKey());
                if (dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_BLOCK.getKey(), PersistentDataType.STRING))
                    dataContainer.remove(Keyring.AntiVillagerLag.OPTIMIZED_BLOCK.getKey());

                villager.setAware(true);
                villager.setAI(true);
            } else {
                switch (type) {
                    case BLOCK:
                        dataContainer.set(Keyring.AntiVillagerLag.OPTIMIZED_BLOCK.getKey(), PersistentDataType.STRING, Keyring.AntiVillagerLag.OPTIMIZED_BLOCK.getKey().toString());
                        break;
                    case WORKSTATION:
                        dataContainer.set(Keyring.AntiVillagerLag.OPTIMIZED_WORKSTATION.getKey(), PersistentDataType.STRING, Keyring.AntiVillagerLag.OPTIMIZED_WORKSTATION.getKey().toString());
                        break;
                    case COMMAND:
                    case NAMETAG:
                        dataContainer.set(Keyring.AntiVillagerLag.OPTIMIZED_ANY.getKey(), PersistentDataType.STRING, "AVL");
                        break;
                }

                villager.setAware(false);
            }

            // End repeating task once logic is finished
            setOptimization.cancel();
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public @NotNull OptimizationType getOptimizationType() {
        if (dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_BLOCK.getKey(), PersistentDataType.STRING)) {
            return OptimizationType.BLOCK;
        }
        if (dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_WORKSTATION.getKey(), PersistentDataType.STRING)) {
            return OptimizationType.WORKSTATION;
        }
        if (dataContainer.has(Keyring.AntiVillagerLag.OPTIMIZED_ANY.getKey(), PersistentDataType.STRING)) {
            return OptimizationType.COMMAND; // Best we can do
        }
        return OptimizationType.NONE;
    }

    @Override
    public void saveOptimizeTime() {
        dataContainer.set(Keyring.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG,
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 30);
    }

    @Override
    public long getOptimizeCooldownMillis(final long cooldown_millis) {
        if (dataContainer.has(Keyring.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG)) {
            return Math.max(cooldown_millis, System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(dataContainer.get(Keyring.AntiVillagerLag.NEXT_OPTIMIZATION_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG)));
        }
        return cooldown_millis;
    }

    @Override
    public boolean canRestock(final long cooldown_millis) {
        if (dataContainer.has(Keyring.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.getKey(), PersistentDataType.LONG)) {
            return villager.getWorld().getFullTime() > dataContainer.get(Keyring.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.getKey(), PersistentDataType.LONG);
        }
        return true;
    }

    @Override
    public void saveRestockTime() {
        dataContainer.set(Keyring.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.getKey(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }

    @Override
    public long getRestockCooldownMillis(final long cooldown_millis) {
        if (dataContainer.has(Keyring.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.getKey(), PersistentDataType.LONG))
            return (villager.getWorld().getFullTime() - dataContainer.get(Keyring.AntiVillagerLag.LAST_RESTOCK_WORLDFULLTIME.getKey(), PersistentDataType.LONG)) * 50L;
        return cooldown_millis;
    }

    @Override
    public boolean canLevelUp(final long cooldown_millis) {
        return !dataContainer.has(Keyring.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG)
                || System.currentTimeMillis() > TimeUnit.SECONDS.toMillis(dataContainer.get(Keyring.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG));
    }

    @Override
    public void saveLastLevelUp() {
        dataContainer.set(Keyring.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + 30);
    }

    @Override
    public long getLevelCooldownMillis(final long cooldown_millis) {
        if (dataContainer.has(Keyring.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG))
            return System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(dataContainer.get(Keyring.AntiVillagerLag.NEXT_LEVELUP_SYSTIME_SECONDS.getKey(), PersistentDataType.LONG));
        return cooldown_millis;
    }
}