package me.xginko.villageroptimizer.config;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import me.xginko.villageroptimizer.VillagerOptimizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class LanguageCache {

    private final @NotNull ConfigFile lang;

    public final @NotNull Component no_permission;
    public final @NotNull List<Component> nametag_optimize_success, nametag_on_optimize_cooldown, nametag_unoptimize_success,
            block_optimize_success, block_on_optimize_cooldown, block_unoptimize_success,
            workstation_optimize_success, workstation_on_optimize_cooldown, workstation_unoptimize_success,
            command_optimize_success, command_radius_limit_exceed, command_optimize_fail, command_unoptimize_success,
            command_specify_radius, command_radius_invalid, command_no_villagers_nearby,
            trades_restocked, optimize_for_trading, villager_leveling_up;

    public LanguageCache(String locale) throws Exception {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        File langYML = new File(plugin.getDataFolder() + File.separator + "lang", locale + ".yml");
        // Check if the lang folder has already been created
        File parent = langYML.getParentFile();
        if (!parent.exists() && !parent.mkdir())
            VillagerOptimizer.getLog().error("Failed to create lang directory.");
        // Check if the file already exists and save the one from the plugins resources folder if it does not
        if (!langYML.exists())
            plugin.saveResource("lang" + File.separator + locale + ".yml", false);
        // Finally load the lang file with configmaster
        this.lang = ConfigFile.loadConfig(langYML);

        // General
        this.no_permission = getTranslation("messages.no-permission",
                "<red>You don't have permission to use this command.");
        this.trades_restocked = getListTranslation("messages.trades-restocked",
                List.of("<green>All trades have been restocked! Next restock in %time%"));
        this.optimize_for_trading = getListTranslation("messages.optimize-to-trade",
                List.of("<red>You need to optimize this villager before you can trade with it."));
        this.villager_leveling_up = getListTranslation("messages.villager-leveling-up",
                List.of("<yellow>Villager is currently leveling up! You can use the villager again in %time%."));
        // Nametag
        this.nametag_optimize_success = getListTranslation("messages.nametag.optimize-success",
                List.of("<green>Successfully optimized villager by using a nametag."));
        this.nametag_on_optimize_cooldown = getListTranslation("messages.nametag.optimize-on-cooldown",
                List.of("<gray>You need to wait %time% until you can optimize this villager again."));
        this.nametag_unoptimize_success = getListTranslation("messages.nametag.unoptimize-success",
                List.of("<green>Successfully unoptimized villager by using a nametag."));
        // Block
        this.block_optimize_success = getListTranslation("messages.block.optimize-success",
                List.of("<green>%villagertype% villager successfully optimized using block %blocktype%."));
        this.block_on_optimize_cooldown = getListTranslation("messages.block.optimize-on-cooldown",
                List.of("<gray>You need to wait %time% until you can optimize this villager again."));
        this.block_unoptimize_success = getListTranslation("messages.block.unoptimize-success",
                List.of("<green>Successfully unoptimized %villagertype% villager by removing %blocktype%."));
        // Workstation
        this.workstation_optimize_success = getListTranslation("messages.workstation.optimize-success",
                List.of("<green>%villagertype% villager successfully optimized using workstation %workstation%."));
        this.workstation_on_optimize_cooldown = getListTranslation("messages.workstation.optimize-on-cooldown",
                List.of("<gray>You need to wait %time% until you can optimize this villager again."));
        this.workstation_unoptimize_success = getListTranslation("messages.workstation.unoptimize-success",
                List.of("<green>Successfully unoptimized %villagertype% villager by removing workstation block %workstation%."));
        // Command
        this.command_optimize_success = getListTranslation("messages.command.optimize-success",
                List.of("<green>Successfully optimized %amount% villager(s) in a radius of %radius% blocks."));
        this.command_radius_limit_exceed = getListTranslation("messages.command.radius-limit-exceed",
                List.of("<red>The radius you entered exceeds the limit of %distance% blocks."));
        this.command_optimize_fail = getListTranslation("messages.command.optimize-fail",
                List.of("<gray>%amount% villagers couldn't be optimized because they have recently been optimized."));
        this.command_unoptimize_success = getListTranslation("messages.command.unoptimize-success",
                List.of("<green>Successfully unoptimized %amount% villager(s) in a radius of %radius% blocks."));
        this.command_specify_radius = getListTranslation("messages.command.specify-radius",
                List.of("<red>Please specify a radius."));
        this.command_radius_invalid = getListTranslation("messages.command.radius-invalid",
                List.of("<red>The radius you entered is not a valid number. Try again."));
        this.command_no_villagers_nearby = getListTranslation("messages.command.no-villagers-nearby",
                List.of("<gray>Couldn't find any employed villagers within a radius of %radius%."));

        try {
            this.lang.save();
        } catch (Exception e) {
            VillagerOptimizer.getLog().error("Failed to save language file: "+ langYML.getName() +" - " + e.getLocalizedMessage());
        }
    }

    public @NotNull Component getTranslation(@NotNull String path, @NotNull String defaultTranslation) {
        this.lang.addDefault(path, defaultTranslation);
        return MiniMessage.miniMessage().deserialize(this.lang.getString(path, defaultTranslation));
    }

    public @NotNull Component getTranslation(@NotNull String path, @NotNull String defaultTranslation, @NotNull String comment) {
        this.lang.addDefault(path, defaultTranslation, comment);
        return MiniMessage.miniMessage().deserialize(this.lang.getString(path, defaultTranslation));
    }

    public @NotNull List<Component> getListTranslation(@NotNull String path, @NotNull List<String> defaultTranslation) {
        this.lang.addDefault(path, defaultTranslation);
        return this.lang.getStringList(path).stream().map(MiniMessage.miniMessage()::deserialize).toList();
    }

    public @NotNull List<Component> getListTranslation(@NotNull String path, @NotNull List<String> defaultTranslation, @NotNull String comment) {
        this.lang.addDefault(path, defaultTranslation, comment);
        return this.lang.getStringList(path).stream().map(MiniMessage.miniMessage()::deserialize).toList();
    }
}