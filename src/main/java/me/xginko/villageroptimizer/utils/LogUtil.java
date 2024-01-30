package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;
import org.slf4j.event.Level;


public class LogUtil {

    public static void moduleLog(Level logLevel, String path, String logMessage) {
        VillagerOptimizer.getLog().atLevel(logLevel).log("(" + path + ") " + logMessage);
    }

    public static void materialNotRecognized(String path, String material) {
        moduleLog(Level.WARN, path, "Material '" + material + "' not recognized. Please use correct Material enums from: " +
                "https://jd.papermc.io/paper/1.20/org/bukkit/Material.html");
    }

    public static void damageCauseNotRecognized(String path, String cause) {
        moduleLog(Level.WARN, path, "DamageCause '" + cause + "' not recognized. Please use correct DamageCause enums from: " +
                "https://jd.papermc.io/paper/1.20/org/bukkit/event/entity/EntityDamageEvent.DamageCause.html");
    }

    public static void entityTypeNotRecognized(String path, String entityType) {
        moduleLog(Level.WARN, path, "EntityType '" + entityType + "' not recognized. Please use correct Spigot EntityType enums for your Minecraft version!");
    }
}