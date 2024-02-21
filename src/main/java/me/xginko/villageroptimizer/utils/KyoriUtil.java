package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class KyoriUtil {

    public static void sendMessage(CommandSender sender, Component message) {
        VillagerOptimizer.getAudiences().sender(sender).sendMessage(message);
    }

    public static void sendActionBar(CommandSender sender, Component message) {
        VillagerOptimizer.getAudiences().sender(sender).sendActionBar(message);
    }

    public static Locale getLocale(Player player) {
        return VillagerOptimizer.getAudiences().player(player).pointers().getOrDefault(Identity.LOCALE, Locale.US);
    }
}
