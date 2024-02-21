package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class KyoriUtil {

    public static void sendMessage(CommandSender sender, Component message) {
        VillagerOptimizer.getAudiences().sender(sender).sendMessage(message);
    }

    public static void sendActionBar(CommandSender sender, Component message) {
        VillagerOptimizer.getAudiences().sender(sender).sendActionBar(message);
    }
}
