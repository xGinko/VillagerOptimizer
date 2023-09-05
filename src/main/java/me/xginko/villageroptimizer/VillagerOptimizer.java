package me.xginko.villageroptimizer;

import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.VillagerCache;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VillagerOptimizer extends JavaPlugin {

    private static VillagerOptimizer instance;
    private static Logger logger;
    private static Config config;
    private static HashMap<String, LanguageCache> languageCacheMap;
    private static VillagerCache villagerCache;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        villagerCache = new VillagerCache(30);
        logger.info("Loading Translations");
        reloadLang();
        logger.info("Loading Config");
        reloadConfiguration();
        logger.info("Done.");
    }

    public static OptimizationType computeOptimization(@NotNull WrappedVillager wrapped) {
        if (config.enable_nametag_optimization) {
            Component name = wrapped.villager().customName();
            if (name != null && config.nametags.contains(PlainTextComponentSerializer.plainText().serialize(name).toLowerCase())) {
                return OptimizationType.NAMETAG;
            }
        }
        if (config.enable_block_optimization) {
            if (config.blocks_that_disable.contains(wrapped.villager().getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) {
                return OptimizationType.BLOCK;
            }
        }
        if (config.enable_workstation_optimization) {
            final Location jobSite = wrapped.villager().getMemory(MemoryKey.JOB_SITE);
            if (
                    jobSite != null
                    && config.workstations_that_disable.contains(jobSite.getBlock().getType())
                    && wrapped.villager().getLocation().distance(jobSite) <= config.workstation_max_distance
            ) {
                return OptimizationType.WORKSTATION;
            }
        }
        return wrapped.getOptimizationType();
    }

    public static OptimizationType computeOptimization(@NotNull Villager villager) {
        if (config.enable_nametag_optimization) {
            Component name = villager.customName();
            if (name != null && config.nametags.contains(PlainTextComponentSerializer.plainText().serialize(name).toLowerCase())) {
                return OptimizationType.NAMETAG;
            }
        }
        if (config.enable_block_optimization) {
            if (config.blocks_that_disable.contains(villager.getLocation().getBlock().getRelative(BlockFace.DOWN).getType())) {
                return OptimizationType.BLOCK;
            }
        }
        if (config.enable_workstation_optimization) {
            final Location jobSite = villager.getMemory(MemoryKey.JOB_SITE);
            if (
                    jobSite != null
                    && config.workstations_that_disable.contains(jobSite.getBlock().getType())
                    && villager.getLocation().distance(jobSite) <= config.workstation_max_distance
            ) {
                return OptimizationType.WORKSTATION;
            }
        }
        return villagerCache.get(villager).getOptimizationType();
    }

    public void reloadPlugin() {
        villagerCache = new VillagerCache(30);
        reloadLang();
        reloadConfiguration();
    }

    private void reloadConfiguration() {
        try {
            config = new Config();
            VillagerOptimizerModule.reloadModules();
            config.saveConfig();
        } catch (Exception e) {
            logger.severe("Failed to load config file! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void reloadLang() {
        languageCacheMap = new HashMap<>();
        try {
            File langDirectory = new File(getDataFolder() + "/lang");
            Files.createDirectories(langDirectory.toPath());
            for (String fileName : getDefaultLanguageFiles()) {
                String localeString = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
                logger.info(String.format("Found language file for %s", localeString));
                LanguageCache langCache = new LanguageCache(localeString);
                languageCacheMap.put(localeString, langCache);
            }
            Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    String localeString = langMatcher.group(1).toLowerCase();
                    if(!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        logger.info(String.format("Found language file for %s", localeString));
                        LanguageCache langCache = new LanguageCache(localeString);
                        languageCacheMap.put(localeString, langCache);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error loading language files! Language files will not reload to avoid errors, make sure to correct this before restarting the server!");
        }
    }

    private Set<String> getDefaultLanguageFiles() {
        Set<String> languageFiles = new HashSet<>();
        try (JarFile jarFile = new JarFile(this.getFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String path = entries.nextElement().getName();
                if (path.startsWith("lang/") && path.endsWith(".yml")) {
                    languageFiles.add(path);
                }
            }
        } catch (IOException e) {
            logger.severe("Error while getting default language file names! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return languageFiles;
    }

    public static LanguageCache getLang(String lang) {
        lang = lang.replace("-", "_");
        if (config.auto_lang) {
            return languageCacheMap.getOrDefault(lang, languageCacheMap.get(config.default_lang.toString().toLowerCase()));
        } else {
            return languageCacheMap.get(config.default_lang.toString().toLowerCase());
        }
    }

    public static LanguageCache getLang(Locale locale) {
        return getLang(locale.toString().toLowerCase());
    }

    public static LanguageCache getLang(CommandSender commandSender) {
        if (commandSender instanceof Player player) {
            return getLang(player.locale());
        } else {
            return getLang(config.default_lang);
        }
    }

    public static VillagerOptimizer getInstance()  {
        return instance;
    }

    public static NamespacedKey getKey(String key) {
        return new NamespacedKey(instance, key);
    }

    public static Config getConfiguration() {
        return config;
    }

    public static Logger getLog() {
        return logger;
    }
    public static VillagerCache getVillagerCache() {
        return villagerCache;
    }
}
