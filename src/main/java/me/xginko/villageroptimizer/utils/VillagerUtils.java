package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.models.WrappedVillager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Villager;

public class VillagerUtils {

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
        return new WrappedVillager(villager).isOptimized();
    }
}
