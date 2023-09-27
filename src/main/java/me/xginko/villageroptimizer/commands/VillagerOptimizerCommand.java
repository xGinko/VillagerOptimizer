package me.xginko.villageroptimizer.commands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.optimizevillagers.OptVillagersRadius;
import me.xginko.villageroptimizer.commands.unoptimizevillagers.UnOptVillagersRadius;
import me.xginko.villageroptimizer.commands.villageroptimizer.VillagerOptimizerCmd;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public interface VillagerOptimizerCommand extends CommandExecutor {

    String label();

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

    @Override
    boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args);
}
