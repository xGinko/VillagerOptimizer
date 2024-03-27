package me.xginko.villageroptimizer;

import com.tcoded.folialib.FoliaLib;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.GenericUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class VillagerOptimizer extends JavaPlugin {

    private static VillagerOptimizer instance;
    private static VillagerCache villagerCache;
    private static FoliaLib foliaLib;
    private static Map<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static BukkitAudiences audiences;
    private static ComponentLogger logger;
    private static Metrics bStats;

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);
        audiences = BukkitAudiences.create(this);
        logger = ComponentLogger.logger(getLogger().getName());
        bStats = new Metrics(this, 19954);
        try {
            getDataFolder().mkdirs();
        } catch (Exception e) {
            logger.error("Failed to create plugin directory! Cannot enable!", e);
            getServer().getPluginManager().disablePlugin(this);
        }

        logger.info(Component.text("╭────────────────────────────────────────────────────────────╮").style(GenericUtil.STYLE));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("│             _   __ _  __ __                                │").style(GenericUtil.STYLE));
        logger.info(Component.text("│            | | / /(_)/ // /___ _ ___ _ ___  ____           │").style(GenericUtil.STYLE));
        logger.info(Component.text("│            | |/ // // // // _ `// _ `// -_)/ __/           │").style(GenericUtil.STYLE));
        logger.info(Component.text("│            |___//_//_//_/ \\_,_/ \\_, / \\__//_/              │").style(GenericUtil.STYLE));
        logger.info(Component.text("│          ____        __   _    /___/_                      │").style(GenericUtil.STYLE));
        logger.info(Component.text("│         / __ \\ ___  / /_ (_)__ _   (_)___ ___  ____        │").style(GenericUtil.STYLE));
        logger.info(Component.text("│        / /_/ // _ \\/ __// //  ' \\ / //_ // -_)/ __/        │").style(GenericUtil.STYLE));
        logger.info(Component.text("│        \\____// .__/\\__//_//_/_/_//_/ /__/\\__//_/           │").style(GenericUtil.STYLE));
        logger.info(Component.text("│             /_/         by xGinko                          │").style(GenericUtil.STYLE));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("│        ")
                .style(GenericUtil.STYLE).append(Component.text("https://github.com/xGinko/VillagerOptimizer")
                .color(NamedTextColor.GRAY)).append(Component.text("         │").style(GenericUtil.STYLE)));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));

        logger.info(Component.text("│              ")
                .style(GenericUtil.STYLE).append(Component.text(" ➤  Loading Translations...").style(GenericUtil.STYLE))
                .append(Component.text("                   │").style(GenericUtil.STYLE)));
        reloadLang(true);

        logger.info(Component.text("│              ")
                .style(GenericUtil.STYLE).append(Component.text(" ➤  Loading Config...").style(GenericUtil.STYLE))
                .append(Component.text("                         │").style(GenericUtil.STYLE)));
        reloadConfiguration();

        logger.info(Component.text("│              ")
                .style(GenericUtil.STYLE).append(Component.text(" ✓  Done.").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text("                                     │").style(GenericUtil.STYLE)));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("│                                                            │").style(GenericUtil.STYLE));
        logger.info(Component.text("╰────────────────────────────────────────────────────────────╯").style(GenericUtil.STYLE));
    }

    @Override
    public void onDisable() {
        VillagerOptimizerModule.modules.forEach(VillagerOptimizerModule::disable);
        VillagerOptimizerModule.modules.clear();
        if (foliaLib != null) {
            foliaLib.getImpl().cancelAllTasks();
            foliaLib = null;
        }
        if (villagerCache != null) {
            villagerCache.cacheMap().clear();
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
    public static @NotNull Config getConfiguration() {
        return config;
    }
    public static @NotNull VillagerCache getCache() {
        return villagerCache;
    }
    public static @NotNull FoliaLib getFoliaLib() {
        return foliaLib;
    }
    public static @NotNull ComponentLogger getPrefixedLogger() {
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
            villagerCache = new VillagerCache(config.cache_keep_time_seconds);
            VillagerOptimizerCommand.reloadCommands();
            VillagerOptimizerModule.reloadModules();
            config.saveConfig();
        } catch (Exception exception) {
            logger.error("Error loading config!", exception);
        }
    }

    private void reloadLang(boolean startup) {
        languageCacheMap = new HashMap<>();
        try {
            File langDirectory = new File(getDataFolder() + File.separator + "lang");
            Files.createDirectories(langDirectory.toPath());
            Set<String> locales = new HashSet<>();
            locales.addAll(getDefaultLocales(getFile()));
            locales.addAll(getPresentLocales(langDirectory));
            for (String localeString : locales) {
                if (startup) logger.info(
                        Component.text("│                       ").style(GenericUtil.STYLE)
                                .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                                .append(Component.text("                            │").style(GenericUtil.STYLE)));
                else logger.info(String.format("Found language file for %s", localeString));
                languageCacheMap.put(localeString, new LanguageCache(localeString));
            }
        } catch (Exception e) {
            if (startup) logger.error(
                    Component.text("│                      ").style(GenericUtil.STYLE)
                            .append(Component.text("LANG ERROR").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                            .append(Component.text("                            │").style(GenericUtil.STYLE)), e);
            else logger.error("Error loading language files!", e);
        }
    }

    private static final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);

    private @NotNull Set<String> getDefaultLocales(File jarFile) {
        try (final JarFile pluginJarFile = new JarFile(jarFile)) {
            return pluginJarFile.stream()
                    .map(zipEntry -> {
                        Matcher matcher = langPattern.matcher(zipEntry.getName());
                        return matcher.find() ? matcher.group(1) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Throwable t) {
            logger.error("Failed getting default lang files!", t);
            return Collections.emptySet();
        }
    }

    private @NotNull Set<String> getPresentLocales(File folder) {
        try {
            return Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                    .map(file -> {
                        Matcher matcher = langPattern.matcher(file.getName());
                        return matcher.find() ? matcher.group(1) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Throwable t) {
            logger.error("Failed getting lang files from plugin folder!", t);
            return Collections.emptySet();
        }
    }
}
