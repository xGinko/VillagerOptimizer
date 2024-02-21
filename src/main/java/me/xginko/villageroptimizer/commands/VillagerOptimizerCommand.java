package me.xginko.villageroptimizer.commands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.optimizevillagers.OptVillagersRadius;
import me.xginko.villageroptimizer.commands.unoptimizevillagers.UnOptVillagersRadius;
import me.xginko.villageroptimizer.commands.villageroptimizer.VillagerOptimizerCmd;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public interface VillagerOptimizerCommand extends CommandExecutor, TabCompleter {

    String label();

    List<String> NO_TABCOMPLETES = Collections.emptyList();
    List<String> RADIUS_TABCOMPLETES = Arrays.asList("5", "10", "25", "50");

    HashSet<VillagerOptimizerCommand> commands = new HashSet<>();

    static void reloadCommands() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        CommandMap commandMap = plugin.getServer().getCommandMap();
        commands.forEach(command -> plugin.getCommand(command.label()).unregister(commandMap));
        commands.clear();

        commands.add(new VillagerOptimizerCmd());
        commands.add(new OptVillagersRadius());
        commands.add(new UnOptVillagersRadius());

        commands.forEach(command -> plugin.getCommand(command.label()).setExecutor(command));
    }
}
