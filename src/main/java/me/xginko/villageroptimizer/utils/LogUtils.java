package me.xginko.villageroptimizer.utils;

import me.xginko.villageroptimizer.VillagerOptimizer;

import java.util.logging.Level;

public class LogUtils {

    public static void moduleLog(Level logLevel, String path, String logMessage) {
        VillagerOptimizer.getLog().log(logLevel, "(" + path + ") " + logMessage);
    }

    public static void materialNotRecognized(String path, String material) {
        moduleLog(Level.WARNING, path, "Material '" + material + "' not recognized. Please use correct Spigot Material enums for your Minecraft version!");
    }

    public static void entityTypeNotRecognized(String path, String entityType) {
        moduleLog(Level.WARNING, path, "EntityType '" + entityType + "' not recognized. Please use correct Spigot EntityType enums for your Minecraft version!");
    }

    public static void enchantmentNotRecognized(String path, String enchantment) {
        moduleLog(Level.WARNING, path, "Enchantment '" + enchantment + "' not recognized. Please use correct Spigot Enchantment enums for your Minecraft version!");
    }

    public static void integerNotRecognized(String path, String element) {
        moduleLog(Level.WARNING, path, "The configured amount for "+element+" is not an integer.");
    }
}