package me.xginko.villageroptimizer.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.utils.LogUtils;
import org.bukkit.Material;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Config {

    private final ConfigFile config;

    public final Locale default_lang;
    public final boolean auto_lang;

    public final HashSet<String> names_that_disable = new HashSet<>(2);
    public final HashSet<Material> blocks_that_disable = new HashSet<>(2);
    public final HashSet<Material> workstations_that_disable = new HashSet<>(13);

    public Config() throws Exception {
        this.config = loadConfig(new File(VillagerOptimizer.getInstance().getDataFolder(), "config.yml"));
        structureConfig();

        // Language Settings
        this.default_lang = Locale.forLanguageTag(getString("language.default-language", "en_us", "The default language that will be used if auto-language is false or no matching language file was found.").replace("_", "-"));
        this.auto_lang = getBoolean("language.auto-language", true, "If set to true, will display messages based on client language");


        // AI-Disabling
        this.names_that_disable.addAll(getList("ai-disabling.names-that-disable", List.of("Optimize", "DisableAI")).stream().map(String::toLowerCase).toList());
        getList("ai-disabling.blocks-that-disable", List.of("EMERALD_BLOCK", "COBBLESTONE")).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("blocks-that-disable", configuredMaterial);
            }
        });
        getList("ai-disabling.workstations-that-disable", List.of(
                "COMPOSTER", "SMOKER", "BARREL", "LOOM", "BLAST_FURNACE", "BREWING_STAND", "CAULDRON",
                "FLETCHING_TABLE", "CARTOGRAPHY_TABLE", "LECTERN", "SMITHING_TABLE", "STONECUTTER", "GRINDSTONE"
        )).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("workstations-that-disable", configuredMaterial);
            }
        });
    }

    private ConfigFile loadConfig(File ymlFile) throws Exception {
        File parent = new File(ymlFile.getParent());
        if (!parent.exists())
            if (!parent.mkdir())
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
        createTitledSection("Language", "language");
        createTitledSection("AI Disabling", "ai-disabling");

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
        config.makeSectionLenient(path);
        config.addDefault(path, defaultKeyValue);
        return config.getConfigSection(path);
    }

    public ConfigSection getConfigSection(String path, Map<String, Object> defaultKeyValue, String comment) {
        config.makeSectionLenient(path);
        config.addDefault(path, defaultKeyValue, comment);
        return config.getConfigSection(path);
    }

    public void addComment(String path, String comment) {
        config.addComment(path, comment);
    }

    public void addComments(String path, String... comments) {
        config.addComments(path, comments);
    }
}
