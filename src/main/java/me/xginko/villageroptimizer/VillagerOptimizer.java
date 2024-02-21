package me.xginko.villageroptimizer;

import com.tcoded.folialib.FoliaLib;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class VillagerOptimizer extends JavaPlugin {
    public static final TextColor COLOR = TextColor.color(102,255,230);
    public static final Style STYLE = Style.style(COLOR, TextDecoration.BOLD);

    private static VillagerOptimizer instance;
    private static VillagerCache villagerCache;
    private static FoliaLib foliaLib;
    private static Map<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static BukkitAudiences audiences;
    private static ComponentLogger logger;
    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);
        audiences = BukkitAudiences.create(this);
        logger = ComponentLogger.logger(this.getName());
        metrics = new Metrics(this, 19954);

        logger.info(Component.text("╭────────────────────────────────────────────────────────────╮").style(STYLE));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│             _   __ _  __ __                                │").style(STYLE));
        logger.info(Component.text("│            | | / /(_)/ // /___ _ ___ _ ___  ____           │").style(STYLE));
        logger.info(Component.text("│            | |/ // // // // _ `// _ `// -_)/ __/           │").style(STYLE));
        logger.info(Component.text("│            |___//_//_//_/ \\_,_/ \\_, / \\__//_/              │").style(STYLE));
        logger.info(Component.text("│          ____        __   _    /___/_                      │").style(STYLE));
        logger.info(Component.text("│         / __ \\ ___  / /_ (_)__ _   (_)___ ___  ____        │").style(STYLE));
        logger.info(Component.text("│        / /_/ // _ \\/ __// //  ' \\ / //_ // -_)/ __/        │").style(STYLE));
        logger.info(Component.text("│        \\____// .__/\\__//_//_/_/_//_/ /__/\\__//_/           │").style(STYLE));
        logger.info(Component.text("│             /_/         by xGinko                          │").style(STYLE));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│        ")
                .style(STYLE).append(Component.text("https://github.com/xGinko/VillagerOptimizer")
                .color(NamedTextColor.GRAY)).append(Component.text("         │").style(STYLE)));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│              ")
                .style(STYLE).append(Component.text(" ➤  Loading Translations...").style(STYLE))
                .append(Component.text("                   │").style(STYLE)));
        reloadLang(true);
        logger.info(Component.text("│              ")
                .style(STYLE).append(Component.text(" ➤  Loading Config...").style(STYLE))
                .append(Component.text("                         │").style(STYLE)));
        reloadConfiguration();
        logger.info(Component.text("│              ")
                .style(STYLE).append(Component.text(" ✓  Done.").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text("                                     │").style(STYLE)));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("│                                                            │").style(STYLE));
        logger.info(Component.text("╰────────────────────────────────────────────────────────────╯").style(STYLE));
    }

    @Override
    public void onDisable() {
        VillagerOptimizerModule.modules.forEach(VillagerOptimizerModule::disable);
        VillagerOptimizerModule.modules.clear();
        if (villagerCache != null) {
            villagerCache.cacheMap().clear();
            villagerCache = null;
        }
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
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
    public static @NotNull ComponentLogger getLog() {
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
            for (String fileName : getDefaultLanguageFiles()) {
                final String localeString = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.lastIndexOf('.'));
                if (startup) logger.info(
                        Component.text("│                       ").style(STYLE)
                                .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                                .append(Component.text("                            │").style(STYLE)));
                else logger.info(String.format("Found language file for %s", localeString));
                languageCacheMap.put(localeString, new LanguageCache(localeString));
            }
            final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                final Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    String localeString = langMatcher.group(1).toLowerCase();
                    if (!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        if (startup) logger.info(
                                Component.text("│                       ").style(STYLE)
                                        .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                                        .append(Component.text("                            │").style(STYLE)));
                        else logger.info(String.format("Found language file for %s", localeString));
                        languageCacheMap.put(localeString, new LanguageCache(localeString));
                    }
                }
            }
        } catch (Exception e) {
            if (startup) logger.error(
                    Component.text("│                      ").style(STYLE)
                            .append(Component.text("LANG ERROR").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                            .append(Component.text("                            │").style(STYLE)), e);
            else logger.error("Error loading language files! Language files will not reload to avoid errors, make sure to correct this before restarting the server!", e);
        }
    }

    private @NotNull Set<String> getDefaultLanguageFiles() {
        try (final JarFile pluginJarFile = new JarFile(this.getFile())) {
            return pluginJarFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("lang" + File.separator) && name.endsWith(".yml"))
                    .collect(Collectors.toSet());
        } catch (IOException ioException) {
            logger.error("Failed getting default lang files!", ioException);
            return Collections.emptySet();
        }
    }
}
