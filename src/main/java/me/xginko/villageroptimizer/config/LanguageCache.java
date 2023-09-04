package me.xginko.villageroptimizer.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.xginko.villageroptimizer.VillagerOptimizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.File;
import java.util.List;

public class LanguageCache {

    private final ConfigFile lang;
    private final MiniMessage miniMessage;

    public final Component no_permission;

    public LanguageCache(String lang) throws Exception {
        this.lang = loadLang(new File(VillagerOptimizer.getInstance().getDataFolder() + File.separator + "lang", lang + ".yml"));
        this.miniMessage = MiniMessage.miniMessage();

        // No Permission
        this.no_permission = getTranslation("no-permission", "<red>You don't have permission to use this command.", false);

        saveLang();
    }

    private ConfigFile loadLang(File ymlFile) throws Exception {
        File parent = new File(ymlFile.getParent());
        if (!parent.exists())
            if (!parent.mkdir())
                VillagerOptimizer.getLog().severe("Unable to create lang directory.");
        if (!ymlFile.exists())
            ymlFile.createNewFile(); // Result can be ignored because this method only returns false if the file already exists
        return ConfigFile.loadConfig(ymlFile);
    }

    private void saveLang() {
        try {
            lang.save();
        } catch (Exception e) {
            VillagerOptimizer.getLog().severe("Failed to save language file: "+ lang.getFile().getName() +" - " + e.getLocalizedMessage());
        }
    }

    public Component getTranslation(String path, String defaultTranslation, boolean upperCase) {
        lang.addDefault(path, defaultTranslation);
        return miniMessage.deserialize(upperCase ? lang.getString(path, defaultTranslation).toUpperCase() : lang.getString(path, defaultTranslation));
    }

    public Component getTranslation(String path, String defaultTranslation, boolean upperCase, String comment) {
        lang.addDefault(path, defaultTranslation, comment);
        return miniMessage.deserialize(upperCase ? lang.getString(path, defaultTranslation).toUpperCase() : lang.getString(path, defaultTranslation));
    }

    public List<Component> getListTranslation(String path, List<String> defaultTranslation, boolean upperCase) {
        lang.addDefault(path, defaultTranslation);
        return lang.getStringList(path).stream().map(line -> miniMessage.deserialize(upperCase ? line.toUpperCase() : line)).toList();
    }

    public List<Component> getListTranslation(String path, List<String> defaultTranslation, boolean upperCase, String comment) {
        lang.addDefault(path, defaultTranslation, comment);
        return lang.getStringList(path).stream().map(line -> miniMessage.deserialize(upperCase ? line.toUpperCase() : line)).toList();
    }
}
