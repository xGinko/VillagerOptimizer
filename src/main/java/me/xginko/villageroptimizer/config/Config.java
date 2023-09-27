package me.xginko.villageroptimizer.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import me.xginko.villageroptimizer.VillagerOptimizer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Config {

    private final @NotNull ConfigFile config;
    public final @NotNull Locale default_lang;
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
        this.addComment("", "");
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
        createTitledSection("Optimization Methods", "optimization-methods");
        addComment("optimization-methods", """
                BE AWARE:\s
                It is recommended to choose preferably one (no more than 2) of the below methods, as this can\s
                get confusing and depending on your config exploitable otherwise.
                """);
        config.addDefault("optimization-methods.nametag-optimization.enable", true);
        createTitledSection("Villager Chunk Limit", "villager-chunk-limit");
        createTitledSection("Gameplay", "gameplay");
        config.addDefault("gameplay.villagers-spawn-as-adults", false);
        config.addDefault("gameplay.prevent-trading-with-unoptimized.enable", false);
        config.addDefault("gameplay.villager-leveling.enable", true);
        config.addDefault("gameplay.trade-restocking.enable", true);
        config.addDefault("gameplay.prevent-targeting.enable", true);
        config.addDefault("gameplay.prevent-damage.enable", true);
    }

    public void createTitledSection(@NotNull String title, @NotNull String path) {
        config.addSection(title);
        config.addDefault(path, null);
    }

    public @NotNull ConfigFile master() {
        return config;
    }

    public boolean getBoolean(@NotNull String path, boolean def, @NotNull String comment) {
        config.addDefault(path, def, comment);
        return config.getBoolean(path, def);
    }

    public boolean getBoolean(@NotNull String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, def);
    }

    public @NotNull String getString(@NotNull String path, @NotNull String def, @NotNull String comment) {
        config.addDefault(path, def, comment);
        return config.getString(path, def);
    }

    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        config.addDefault(path, def);
        return config.getString(path, def);
    }

    public double getDouble(@NotNull String path, @NotNull Double def, @NotNull String comment) {
        config.addDefault(path, def, comment);
        return config.getDouble(path, def);
    }

    public double getDouble(@NotNull String path, @NotNull Double def) {
        config.addDefault(path, def);
        return config.getDouble(path, def);
    }

    public int getInt(@NotNull String path, int def, @NotNull String comment) {
        config.addDefault(path, def, comment);
        return config.getInteger(path, def);
    }

    public int getInt(@NotNull String path, int def) {
        config.addDefault(path, def);
        return config.getInteger(path, def);
    }

    public @NotNull List<String> getList(@NotNull String path, @NotNull List<String> def, @NotNull String comment) {
        config.addDefault(path, def, comment);
        return config.getStringList(path);
    }

    public @NotNull List<String> getList(@NotNull String path, @NotNull List<String> def) {
        config.addDefault(path, def);
        return config.getStringList(path);
    }

    public @NotNull ConfigSection getConfigSection(@NotNull String path, @NotNull Map<String, Object> defaultKeyValue) {
        config.addDefault(path, null);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path+"."+string, object));
        return config.getConfigSection(path);
    }

    public @NotNull ConfigSection getConfigSection(@NotNull String path, @NotNull Map<String, Object> defaultKeyValue, @NotNull String comment) {
        config.addDefault(path, null, comment);
        config.makeSectionLenient(path);
        defaultKeyValue.forEach((string, object) -> config.addExample(path+"."+string, object));
        return config.getConfigSection(path);
    }

    public void addComment(@NotNull String path, @NotNull String comment) {
        config.addComment(path, comment);
    }
}
