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
    public final boolean auto_lang, enable_nametag_optimization, enable_workstation_optimization, enable_block_optimization,
        cache_enabled;
    public final int state_change_cooldown;
    public final double workstation_max_distance;
    public final long cache_keep_time_seconds;

    public final HashSet<String> nametags = new HashSet<>(2);
    public final HashSet<Material> blocks_that_disable = new HashSet<>(3);
    public final HashSet<Material> workstations_that_disable = new HashSet<>(13);

    public Config() throws Exception {
        this.config = loadConfig(new File(VillagerOptimizer.getInstance().getDataFolder(), "config.yml"));
        structureConfig();
        /**
         * General
         */
        this.default_lang = Locale.forLanguageTag(
                getString("general.default-language", "en_us",
                        "The default language that will be used if auto-language is false or no matching language file was found.")
                .replace("_", "-"));
        this.auto_lang = getBoolean("general.auto-language", true, "If set to true, will display messages based on client language");
        this.cache_keep_time_seconds = getInt("general.cache-keep-time-seconds", 30, "The amount of time in seconds a villager will be kept in the plugin's cache.");
        /**
         * Optimization
         */
        this.state_change_cooldown = getInt("optimization.state-change-cooldown-in-seconds", 600);
        // Nametags
        this.enable_nametag_optimization = getBoolean("optimization.methods.by-nametag.enable", true);
        this.nametags.addAll(getList("optimization.methods.by-nametag.names", List.of("Optimize", "DisableAI"), "Names are case insensitive")
                .stream().map(String::toLowerCase).toList());
        // Workstations
        this.enable_workstation_optimization = getBoolean("optimization.methods.by-workstation.enable", true, """
                        Optimize villagers that are standing near their acquired workstations /s
                        Values here need to be valid bukkit Material enums for your server version.
                        """);
        this.workstation_max_distance = getDouble("optimization.methods.by-workstation.disable-range-in-blocks", 4.0,
                "How close in blocks a villager needs to be to get optimized by its workstation");
        this.getList("optimization.methods.by-workstation.workstation-materials", List.of(
                "COMPOSTER", "SMOKER", "BARREL", "LOOM", "BLAST_FURNACE", "BREWING_STAND", "CAULDRON",
                "FLETCHING_TABLE", "CARTOGRAPHY_TABLE", "LECTERN", "SMITHING_TABLE", "STONECUTTER", "GRINDSTONE"
        )).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("optimization.methods.by-workstation", configuredMaterial);
            }
        });
        // Blocks
        this.enable_block_optimization = getBoolean("optimization.methods.by-specific-block.enable", true, """
                        Optimize villagers that are standing on these specific block materials /s
                        Values here need to be valid bukkit Material enums for your server version.
                        """);
        this.getList("optimization.methods.by-specific-block.materials", List.of(
                "LAPIS_BLOCK", "GLOWSTONE", "IRON_BLOCK"
        )).forEach(configuredMaterial -> {
            try {
                Material disableBlock = Material.valueOf(configuredMaterial);
                this.blocks_that_disable.add(disableBlock);
            } catch (IllegalArgumentException e) {
                LogUtils.materialNotRecognized("optimization.methods.by-specific-block", configuredMaterial);
            }
        });
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
