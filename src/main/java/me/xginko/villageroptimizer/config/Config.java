package me.xginko.villageroptimizer.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import me.xginko.villageroptimizer.VillagerOptimizer;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Config {

    private final ConfigFile config;
    public final Locale default_lang;
    public final boolean auto_lang;
    public final long cache_keep_time_seconds;

    public Config() throws Exception {
        this.config = loadConfig(new File(VillagerOptimizer.getInstance().getDataFolder(), "config.yml"));
        structureConfig();
        this.default_lang = Locale.forLanguageTag(
                getString("general.default-language", "en_us",
                        "The default language that will be used if auto-language is false or no matching language file was found.")
                .replace("_", "-"));
        this.auto_lang = getBoolean("general.auto-language", true,
                "If set to true, will display messages based on client language");
        this.cache_keep_time_seconds = getInt("general.cache-keep-time-seconds", 30,
                "The amount of time in seconds a villager will be kept in the plugin's cache.");
    }

    private ConfigFile loadConfig(File ymlFile) throws Exception {
        File parent = new File(ymlFile.getParent());
        if (!parent.exists() && !parent.mkdir())
            VillagerOptimizer.getLog().severe("Unable to create plugin config directory.");
        if (!ymlFile.exists())
            ymlFile.createNewFile(); // Result can be ignored because this method only returns false if the file already exists
        return ConfigFile.loadConfig(ymlFile);
    }

    public void saveConfig() {
        try {
            config.save();
        } catch (Exception e) {
            VillagerOptimizer.getLog().severe("Failed to save config file! - " + e.getLocalizedMessage());
        }
    }

    private void structureConfig() {
        config.addDefault("config-version", 1.00);
        createTitledSection("General", "general");
        createTitledSection("Optimization", "optimization");
        config.addDefault("optimization.villager-chunk-limit.enable", false);
        config.addDefault("optimization.prevent-trading-with-unoptimized.enable", false);
        config.addDefault("optimization.methods.by-nametag.enable", true);
        config.addDefault("optimization.behavior.villager-leveling.enable", true);
        config.addDefault("optimization.behavior.trade-restocking.enable", true);
        config.addDefault("optimization.behavior.prevent-targeting.enable", true);
        config.addDefault("optimization.behavior.prevent-damage.enable", true);
    }

    public void createTitledSection(String title, String path) {
        config.addSection(title);
        config.addDefault(path, null);
    }

    public boolean getBoolean(String path, boolean def, String comment) {
        config.addDefault(path, def, comment);
        return config.getBoolean(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, def);
    }

    public String getString(String path, String def, String comment) {
        config.addDefault(path, def, comment);
        return config.getString(path, def);
    }

    public String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, def);
    }

    public double getDouble(String path, Double def, String comment) {
        config.addDefault(path, def, comment);
        return config.getDouble(path, def);
    }

    public double getDouble(String path, Double def) {
        config.addDefault(path, def);
        return config.getDouble(path, def);
    }

    public int getInt(String path, int def, String comment) {
        config.addDefault(path, def, comment);
        return config.getInteger(path, def);
    }

    public int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInteger(path, def);
    }

    public List<String> getList(String path, List<String> def, String comment) {
        config.addDefault(path, def, comment);
        return config.getStringList(path);
    }

    public List<String> getList(String path, List<String> def) {
        config.addDefault(path, def);
        return config.getStringList(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue) {
        config.addDefault(path, null);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path+"."+string, object));
        return config.getConfigSection(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue, String comment) {
        config.addDefault(path, null, comment);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path+"."+string, object));
        return config.getConfigSection(path);
    }

    public void addComment(String path, String comment) {
        config.addComment(path, comment);
    }

    public void addComments(String path, String... comments) {
        config.addComments(path, comments);
    }
}
