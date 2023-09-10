package me.xginko.villageroptimizer;

import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VillagerOptimizer extends JavaPlugin {

    private static VillagerOptimizer instance;
    private static HashMap<String, LanguageCache> languageCacheMap;
    private static VillagerManager villagerManager;
    private static Config config;
    private static Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        logger.info("Loading Translations");
        reloadLang();
        logger.info("Loading Config");
        reloadConfiguration();
        logger.info("Registering Commands");
        VillagerOptimizerCommand.reloadCommands();
        logger.info("Done.");
    }

    public static VillagerOptimizer getInstance()  {
        return instance;
    }
    public static VillagerManager getVillagerManager() {
        return villagerManager;
    }
    public static Config getConfiguration() {
        return config;
    }
    public static NamespacedKey getKey(String key) {
        return new NamespacedKey(instance, key);
    }
    public static Logger getLog() {
        return logger;
    }

    public void reloadPlugin() {
        reloadLang();
        reloadConfiguration();
        VillagerOptimizerCommand.reloadCommands();
    }

    private void reloadConfiguration() {
        try {
            config = new Config();
            villagerManager = new VillagerManager(config.cache_keep_time_seconds);
            VillagerOptimizerModule.reloadModules();
            config.saveConfig();
        } catch (Exception e) {
            logger.severe("Error while loading config! - " + e.getLocalizedMessage());
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
            jarFile.entries().asIterator().forEachRemaining(jarEntry -> {
                final String path = jarEntry.getName();
                if (path.startsWith("lang/") && path.endsWith(".yml"))
                    languageFiles.add(path);
            });
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
}
