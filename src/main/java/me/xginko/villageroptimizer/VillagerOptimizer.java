package me.xginko.villageroptimizer;

import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.config.LanguageCache;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
    private static CachedVillagers cachedVillagers;
    private static HashMap<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static Logger logger;

    private final static Style plugin_style = Style.style(TextColor.color(102,255,230), TextDecoration.BOLD);

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        ConsoleCommandSender console = getServer().getConsoleSender();
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
        console.sendMessage(Component.text("│        ").style(plugin_style).append(Component.text("https://github.com/xGinko/VillagerOptimizer").color(NamedTextColor.GRAY)).append(Component.text("         │").style(plugin_style)));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│              ").style(plugin_style).append(Component.text(" ➤  Loading Translations...").style(plugin_style)).append(Component.text("                   │")).decorate(TextDecoration.BOLD));
        reloadLang(true);
        console.sendMessage(Component.text("│              ").style(plugin_style).append(Component.text(" ➤  Loading Config...").style(plugin_style)).append(Component.text("                         │")).decorate(TextDecoration.BOLD));
        reloadConfiguration();
        console.sendMessage(Component.text("│              ").style(plugin_style).append(Component.text(" ➤  Registering Commands...").style(plugin_style)).append(Component.text("                   │").style(plugin_style)));
        VillagerOptimizerCommand.reloadCommands();
        console.sendMessage(Component.text("│              ").style(plugin_style).append(Component.text(" ✓  Done.").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component.text("                                     │").style(plugin_style)));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("│                                                            │").style(plugin_style));
        console.sendMessage(Component.text("╰────────────────────────────────────────────────────────────╯").style(plugin_style));
    }

    public static VillagerOptimizer getInstance()  {
        return instance;
    }
    public static CachedVillagers getCachedVillagers() {
        return cachedVillagers;
    }
    public static Config getConfiguration() {
        return config;
    }
    public static NamespacedKey getKey(String key) {
        return new NamespacedKey(instance, key);
    }
    public static LanguageCache getLang(String lang) {
        return config.auto_lang ? languageCacheMap.getOrDefault(lang.replace("-", "_"), languageCacheMap.get(config.default_lang.toString().toLowerCase())) : languageCacheMap.get(config.default_lang.toString().toLowerCase());
    }
    public static LanguageCache getLang(Locale locale) {
        return getLang(locale.toString().toLowerCase());
    }
    public static LanguageCache getLang(CommandSender commandSender) {
        return commandSender instanceof Player player ? getLang(player.locale()) : getLang(config.default_lang);
    }
    public static Logger getLog() {
        return logger;
    }

    public void reloadPlugin() {
        reloadLang(false);
        reloadConfiguration();
        VillagerOptimizerCommand.reloadCommands();
    }

    private void reloadConfiguration() {
        try {
            config = new Config();
            cachedVillagers = new CachedVillagers(config.cache_keep_time_seconds);
            VillagerOptimizerModule.reloadModules();
            config.saveConfig();
        } catch (Exception e) {
            logger.severe("Error while loading config! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void reloadLang(boolean startup) {
        languageCacheMap = new HashMap<>();
        ConsoleCommandSender console = getServer().getConsoleSender();
        try {
            File langDirectory = new File(getDataFolder() + "/lang");
            Files.createDirectories(langDirectory.toPath());
            for (String fileName : getDefaultLanguageFiles()) {
                String localeString = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
                if (startup) console.sendMessage(
                        Component.text("│                       ").style(plugin_style)
                        .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(Component.text("                            │").style(plugin_style)));
                else logger.info(String.format("Found language file for %s", localeString));
                LanguageCache langCache = new LanguageCache(localeString);
                languageCacheMap.put(localeString, langCache);
            }
            Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    String localeString = langMatcher.group(1).toLowerCase();
                    if (!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        if (startup) console.sendMessage(
                                Component.text("│                       ").style(plugin_style)
                                .append(Component.text("    "+localeString).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                                .append(Component.text("                            │").style(plugin_style)));
                        else logger.info(String.format("Found language file for %s", localeString));
                        LanguageCache langCache = new LanguageCache(localeString);
                        languageCacheMap.put(localeString, langCache);
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
        Set<String> languageFiles = new HashSet<>();
        try (JarFile jarFile = new JarFile(this.getFile())) {
            jarFile.entries().asIterator().forEachRemaining(jarFileEntry -> {
                final String path = jarFileEntry.getName();
                if (path.startsWith("lang/") && path.endsWith(".yml"))
                    languageFiles.add(path);
            });
        } catch (IOException e) {
            logger.severe("Error while getting default language file names! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
        return languageFiles;
    }
}
