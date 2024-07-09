package me.xginko.villageroptimizer;

import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.Util;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.commands.CommandRegistration;
import space.arim.morepaperlib.scheduling.GracefulScheduling;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class VillagerOptimizer extends JavaPlugin {

    private static VillagerOptimizer instance;
    private static VillagerCache villagerCache;
    private static CommandRegistration commandRegistration;
    private static GracefulScheduling scheduling;
    private static Map<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static BukkitAudiences audiences;
    private static ComponentLogger logger;
    private static Metrics bStats;

    @Override
    public void onLoad() {
        // Disable reflection logging
        String shadedLibs = getClass().getPackage().getName() + ".libs";
        Configurator.setLevel(shadedLibs + ".reflections.Reflections", Level.OFF);
    }

    @Override
    public void onEnable() {
        instance = this;
        MorePaperLib morePaperLib = new MorePaperLib(this);
        commandRegistration = morePaperLib.commandRegistration();
        scheduling = morePaperLib.scheduling();
        audiences = BukkitAudiences.create(this);
        logger = ComponentLogger.logger(getLogger().getName());
        bStats = new Metrics(this, 19954);

        try {
            getDataFolder().mkdirs();
        } catch (Exception e) {
            logger.error("Failed to create plugin directory! Cannot enable!", e);
            getServer().getPluginManager().disablePlugin(this);
        }

        logger.info(Component.text("╭────────────────────────────────────────────────────────────╮").style(Util.PL_STYLE));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("│             _   __ _  __ __                                │").style(Util.PL_STYLE));
        logger.info(Component.text("│            | | / /(_)/ // /___ _ ___ _ ___  ____           │").style(Util.PL_STYLE));
        logger.info(Component.text("│            | |/ // // // // _ `// _ `// -_)/ __/           │").style(Util.PL_STYLE));
        logger.info(Component.text("│            |___//_//_//_/ \\_,_/ \\_, / \\__//_/              │").style(Util.PL_STYLE));
        logger.info(Component.text("│          ____        __   _    /___/_                      │").style(Util.PL_STYLE));
        logger.info(Component.text("│         / __ \\ ___  / /_ (_)__ _   (_)___ ___  ____        │").style(Util.PL_STYLE));
        logger.info(Component.text("│        / /_/ // _ \\/ __// //  ' \\ / //_ // -_)/ __/        │").style(Util.PL_STYLE));
        logger.info(Component.text("│        \\____// .__/\\__//_//_/_/_//_/ /__/\\__//_/           │").style(Util.PL_STYLE));
        logger.info(Component.text("│             /_/         by xGinko                          │").style(Util.PL_STYLE));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("│        ")
                .style(Util.PL_STYLE).append(Component.text("https://github.com/xGinko/VillagerOptimizer")
                .color(NamedTextColor.GRAY)).append(Component.text("         │").style(Util.PL_STYLE)));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        Permissions.registerAll();

        logger.info(Component.text("│              ")
                .style(Util.PL_STYLE).append(Component.text(" ➤  Loading Config...").style(Util.PL_STYLE))
                .append(Component.text("                         │").style(Util.PL_STYLE)));
        reloadConfiguration();

        logger.info(Component.text("│              ")
                .style(Util.PL_STYLE).append(Component.text(" ➤  Loading Translations...").style(Util.PL_STYLE))
                .append(Component.text("                   │").style(Util.PL_STYLE)));
        reloadLang(true);

        logger.info(Component.text("│              ")
                .style(Util.PL_STYLE).append(Component.text(" ✓  Done.").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text("                                     │").style(Util.PL_STYLE)));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("│                                                            │").style(Util.PL_STYLE));
        logger.info(Component.text("╰────────────────────────────────────────────────────────────╯").style(Util.PL_STYLE));
    }

    @Override
    public void onDisable() {
        VillagerOptimizerModule.ENABLED_MODULES.forEach(VillagerOptimizerModule::disable);
        VillagerOptimizerModule.ENABLED_MODULES.clear();
        if (scheduling != null) {
            scheduling.cancelGlobalTasks();
            scheduling = null;
        }
        if (villagerCache != null) {
            villagerCache.disable();
            villagerCache = null;
        }
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
        if (bStats != null) {
            bStats.shutdown();
            bStats = null;
        }
        config = null;
        languageCacheMap = null;
        logger = null;
        instance = null;
    }

    public static @NotNull VillagerOptimizer getInstance()  {
        return instance;
    }
    public static @NotNull Config config() {
        return config;
    }
    public static @NotNull VillagerCache getCache() {
        return villagerCache;
    }
    public static @NotNull CommandRegistration commandRegistration() {
        return commandRegistration;
    }
    public static @NotNull GracefulScheduling scheduling() {
        return scheduling;
    }
    public static @NotNull ComponentLogger logger() {
        return logger;
    }
    public static @NotNull BukkitAudiences getAudiences() {
        return audiences;
    }
    public static @NotNull LanguageCache getLang(Locale locale) {
        return getLang(locale.toString().toLowerCase());
    }
    public static @NotNull LanguageCache getLang(CommandSender commandSender) {
        return commandSender instanceof Player ? getLang(((Player) commandSender).locale()) : getLang(config.default_lang);
    }
    public static @NotNull LanguageCache getLang(String lang) {
        if (!config.auto_lang) return languageCacheMap.get(config.default_lang.toString().toLowerCase());
        return languageCacheMap.getOrDefault(lang.replace("-", "_"), languageCacheMap.get(config.default_lang.toString().toLowerCase()));
    }

    public void reloadPlugin() {
        reloadLang(false);
        reloadConfiguration();
    }

    private void reloadConfiguration() {
        try {
            config = new Config();
            if (villagerCache != null) villagerCache.disable();
            villagerCache = new VillagerCache(config.cache_keep_time_seconds);
            VillagerOptimizerCommand.reloadCommands();
            VillagerOptimizerModule.reloadModules();
            config.saveConfig();
        } catch (Exception exception) {
            logger.error("Error during config reload!", exception);
        }
    }

    private void reloadLang(boolean logFancy) {
        try {
            final SortedSet<String> availableLocales = getAvailableTranslations();
            if (!config.auto_lang) {
                final String defaultLang = config.default_lang.toString().replace("-", "_").toLowerCase();
                if (!availableLocales.contains(defaultLang))
                    throw new FileNotFoundException("Could not find any translation file for language '" + config.default_lang + "'");
                availableLocales.removeIf(localeString -> !localeString.equalsIgnoreCase(defaultLang));
            }
            languageCacheMap = new HashMap<>(availableLocales.size());
            for (String localeString : availableLocales) {
                if (logFancy) logger.info(Component.text("│                       ").style(Util.PL_STYLE)
                        .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(Component.text("                            │").style(Util.PL_STYLE)));
                else logger.info(String.format("Found language file for %s", localeString));
                languageCacheMap.put(localeString, new LanguageCache(localeString));
            }
        } catch (Throwable t) {
            if (logFancy) logger.error(Component.text("│                      ").style(Util.PL_STYLE)
                    .append(Component.text("LANG ERROR").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                    .append(Component.text("                            │").style(Util.PL_STYLE)), t);
            else logger.error("Error while loading translation files!", t);
        }
    }

    private @NotNull SortedSet<String> getAvailableTranslations() {
        try (final JarFile pluginJar = new JarFile(getFile())) {
            final File langDirectory = new File(getDataFolder() + "/lang");
            Files.createDirectories(langDirectory.toPath());
            final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            return Stream.concat(pluginJar.stream().map(ZipEntry::getName), Arrays.stream(langDirectory.listFiles()).map(File::getName))
                    .map(langPattern::matcher)
                    .filter(Matcher::find)
                    .map(matcher -> matcher.group(1))
                    .collect(Collectors.toCollection(TreeSet::new));
        } catch (Throwable t) {
            logger.error("Failed while searching for available translations!", t);
            return new TreeSet<>();
        }
    }
}
