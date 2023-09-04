package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class VillagerUtils {

    public static boolean isDisabled(Villager villager) {
        return hasDisabledByBlock(villager) || hasDisabledByWorkstation(villager) || hasMarker(villager);
    }

    public static void restockTrades(Villager villager) {
        for (MerchantRecipe recipe : villager.getRecipes()) {
            recipe.setUses(0);
        }
    }

    public static boolean shouldDisable(Villager villager) {
        // Check nametag
        Component nameTag = villager.customName();
        if (nameTag != null) {
            if (VillagerOptimizer.getConfiguration().names_that_disable.contains(PlainTextComponentSerializer.plainText().serialize(nameTag))) {
                return true;
            }
        }

        // Check block below
        if (VillagerOptimizer.getConfiguration().blocks_that_disable.contains(villager.getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) {
            return true;
        }

        // Check Workstation
        return getDisabledByWorkstation(villager).orElse(false);
    }

    /*
    *
    *  Helper Methods for storing and reading data inside villagers using PersistentDataContainer
    *
    * */

    // Disabled by Block
    public static void setDisabledByBlock(Villager villager, boolean state) {
        villager.getPersistentDataContainer().set(NamespacedKeys.BLOCK_DISABLED.get(), PersistentDataType.BOOLEAN, state);
    }
    public static boolean isDisabledByBlock(Villager villager) {
        if (villager.getPersistentDataContainer().has(NamespacedKeys.BLOCK_DISABLED.get(), PersistentDataType.BOOLEAN)) {
            return villager.getPersistentDataContainer().get(NamespacedKeys.BLOCK_DISABLED.get(), PersistentDataType.BOOLEAN);
        } else {
            setDisabledByBlock(villager, false);
            return false;
        }
    }

    // Disabled by Workstation
    public static void setDisabledByWorkstation(Villager villager, Boolean state) {
        villager.getPersistentDataContainer().set(NamespacedKeys.WORKSTATION_DISABLED.get(), PersistentDataType.BOOLEAN, state);
    }
    public static boolean hasDisabledByWorkstation(Villager villager) {
        return villager.getPersistentDataContainer().has(NamespacedKeys.WORKSTATION_DISABLED.get(), PersistentDataType.BOOLEAN);
    }
    public static Optional<Boolean> getDisabledByWorkstation(Villager villager) {
        return Optional.ofNullable(villager.getPersistentDataContainer().get(NamespacedKeys.WORKSTATION_DISABLED.get(), PersistentDataType.BOOLEAN));
    }

    // Cooldown
    public static void setCooldown(Villager villager, long cooldown_millis) {
        villager.getPersistentDataContainer().set(NamespacedKeys.COOLDOWN.get(), PersistentDataType.LONG, System.currentTimeMillis() + cooldown_millis);
    }
    public static boolean hasCooldown(Villager villager) {
        return villager.getPersistentDataContainer().has(NamespacedKeys.COOLDOWN.get(), PersistentDataType.LONG);
    }
    public static Optional<Long> getCooldown(Villager villager) {
        return Optional.ofNullable(villager.getPersistentDataContainer().get(NamespacedKeys.COOLDOWN.get(), PersistentDataType.LONG));
    }

    // Time
    public static void setTime(Villager villager) {
        villager.getPersistentDataContainer().set(NamespacedKeys.TIME.get(), PersistentDataType.LONG, villager.getWorld().getFullTime());
    }
    public static boolean hasTime(Villager villager) {
        return villager.getPersistentDataContainer().has(NamespacedKeys.TIME.get(), PersistentDataType.LONG);
    }
    public static Optional<Long> getTime(Villager villager) {
        return Optional.ofNullable(villager.getPersistentDataContainer().get(NamespacedKeys.TIME.get(), PersistentDataType.LONG));
    }

    // Level Cooldown
    public static void setLevelCooldown(Villager villager, long cooldown_millis) {
        villager.getPersistentDataContainer().set(NamespacedKeys.LEVEL_COOLDOWN.get(), PersistentDataType.LONG, System.currentTimeMillis() + cooldown_millis);
    }
    public static boolean hasLevelCooldown(Villager villager, JavaPlugin plugin) {
        return villager.getPersistentDataContainer().has(NamespacedKeys.LEVEL_COOLDOWN.get(), PersistentDataType.LONG);
    }
    public static Optional<Long> getLevelCooldown(Villager villager) {
        return Optional.ofNullable(villager.getPersistentDataContainer().get(NamespacedKeys.LEVEL_COOLDOWN.get(), PersistentDataType.LONG));
    }

    // Marker
    public static void setMarker(Villager villager) {
        villager.getPersistentDataContainer().set(NamespacedKeys.NAMETAG_DISABLED.get(), PersistentDataType.BYTE, (byte)1);
    }
    public static boolean hasMarker(Villager villager) {
        return villager.getPersistentDataContainer().has(NamespacedKeys.NAMETAG_DISABLED.get(), PersistentDataType.BYTE);
    }
    public static void removeMarker(Villager villager) {
        villager.getPersistentDataContainer().remove(NamespacedKeys.NAMETAG_DISABLED.get());
    }
}
