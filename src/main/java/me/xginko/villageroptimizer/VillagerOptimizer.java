package me.xginko.villageroptimizer;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class VillagerOptimizer extends JavaPlugin {
    public static final Style plugin_style = Style.style(TextColor.color(102,255,230), TextDecoration.BOLD);

    private static VillagerOptimizer instance;
    private static VillagerCache villagerCache;
    private static FoliaLib foliaLib;
    private static HashMap<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static ConsoleCommandSender console;
    private static Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        console = getServer().getConsoleSender();
        foliaLib = new FoliaLib(this);

        console.sendMessage(Component.text("╭────────────────────────────────────────────────────────────╮").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│             _   __ _  __ __                                │").style(plugin_style));
        console.sendMessage(Component.text("│            | | / /(_)/ // /___ _ ___ _ ___  ____           │").style(plugin_style));
        console.sendMessage(Component.text("│            | |/ // // // // _ `// _ `// -_)/ __/           │").style(plugin_style));
        console.sendMessage(Component.text("│            |___//_//_//_/ \\_,_/ \\_, / \\__//_/              │").style(plugin_style));
        console.sendMessage(Component.text("│          ____        __   _    /___/_                      │").style(plugin_style));
        console.sendMessage(Component.text("│         / __ \\ ___  / /_ (_)__ _   (_)___ ___  ____        │").style(plugin_style));
        console.sendMessage(Component.text("│        / /_/ // _ \\/ __// //  ' \\ / //_ // -_)/ __/        │").style(plugin_style));
        console.sendMessage(Component.text("│        \\____// .__/\\__//_//_/_/_//_/ /__/\\__//_/           │").style(plugin_style));
        console.sendMessage(Component.text("│             /_/         by xGinko                          │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│        ")
                .style(plugin_style).append(Component.text("https://github.com/xGinko/VillagerOptimizer")
                .color(NamedTextColor.GRAY)).append(Component.text("         │").style(plugin_style)));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│              ")
                .style(plugin_style).append(Component.text(" ➤  Loading Translations...").style(plugin_style))
                .append(Component.text("                   │").style(plugin_style)));
        reloadLang(true);
        console.sendMessage(Component.text("│              ")
                .style(plugin_style).append(Component.text(" ➤  Loading Config...").style(plugin_style))
                .append(Component.text("                         │").style(plugin_style)));
        reloadConfiguration();
        console.sendMessage(Component.text("│              ")
                .style(plugin_style).append(Component.text(" ✓  Done.").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text("                                     │").style(plugin_style)));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("╰────────────────────────────────────────────────────────────╯").style(plugin_style));

        new Metrics(this, 19954);
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
    public static VillagerCache getCache() {
        return villagerCache;
    }
    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }
    public static ServerImplementation getScheduler() {
        return foliaLib.getImpl();
    }
    public static ConsoleCommandSender getConsole() {
        return console;
    }
    public static Logger getLog() {
        return logger;
    }
    public static LanguageCache getLang(Locale locale) {
        return getLang(locale.toString().toLowerCase());
    }
    public static LanguageCache getLang(CommandSender commandSender) {
        return commandSender instanceof Player player ? getLang(player.locale()) : getLang(config.default_lang);
    }
    public static LanguageCache getLang(String lang) {
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
        } catch (Exception e) {
            logger.severe("Error loading config! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void reloadLang(boolean startup) {
        languageCacheMap = new HashMap<>();
        try {
            File langDirectory = new File(getDataFolder() + File.separator + "lang");
            Files.createDirectories(langDirectory.toPath());
            for (String fileName : getDefaultLanguageFiles()) {
                final String localeString = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.lastIndexOf('.'));
                if (startup) console.sendMessage(
                        Component.text("│                       ").style(plugin_style)
                                .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                                .append(Component.text("                            │").style(plugin_style)));
                else logger.info(String.format("Found language file for %s", localeString));
                languageCacheMap.put(localeString, new LanguageCache(localeString));
            }
            final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                final Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    String localeString = langMatcher.group(1).toLowerCase();
                    if (!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        if (startup) console.sendMessage(
                                Component.text("│                       ").style(plugin_style)
                                        .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                                        .append(Component.text("                            │").style(plugin_style)));
                        else logger.info(String.format("Found language file for %s", localeString));
                        languageCacheMap.put(localeString, new LanguageCache(localeString));
                    }
                }
            }
        } catch (Exception e) {
            if (startup) console.sendMessage(
                    Component.text("│                      ").style(plugin_style)
                            .append(Component.text("LANG ERROR").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                            .append(Component.text("                            │").style(plugin_style)));
            else logger.severe("Error loading language files! Language files will not reload to avoid errors, make sure to correct this before restarting the server!");
            e.printStackTrace();
        }
    }

    private Set<String> getDefaultLanguageFiles() {
        try (final JarFile pluginJarFile = new JarFile(this.getFile())) {
            return pluginJarFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("lang" + File.separator) && name.endsWith(".yml"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            logger.severe("Failed getting default lang files! - "+e.getLocalizedMessage());
            return Collections.emptySet();
        }
    }
}
