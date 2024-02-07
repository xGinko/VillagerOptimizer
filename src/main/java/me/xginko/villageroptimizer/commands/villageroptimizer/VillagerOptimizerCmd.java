package me.xginko.villageroptimizer.commands.villageroptimizer;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.commands.villageroptimizer.subcommands.DisableSubCmd;
import me.xginko.villageroptimizer.commands.villageroptimizer.subcommands.ReloadSubCmd;
import me.xginko.villageroptimizer.commands.villageroptimizer.subcommands.VersionSubCmd;
import me.xginko.villageroptimizer.enums.permissions.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VillagerOptimizerCmd implements VillagerOptimizerCommand {

    private final List<SubCommand> subCommands;
    private final List<String> tabCompleter;

    public VillagerOptimizerCmd() {
        subCommands = List.of(new ReloadSubCmd(), new VersionSubCmd(), new DisableSubCmd());
        tabCompleter = subCommands.stream().map(SubCommand::getLabel).toList();
    }

    @Override
    public String label() {
        return "villageroptimizer";
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return args.length == 1 ? tabCompleter : NO_TABCOMPLETES;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendCommandOverview(sender);
            return true;
        }

        for (final SubCommand subCommand : subCommands) {
            if (args[0].equalsIgnoreCase(subCommand.getLabel())) {
                subCommand.perform(sender, args);
                return true;
            }
        }

        sendCommandOverview(sender);
        return true;
    }

    private void sendCommandOverview(CommandSender sender) {
        if (!sender.hasPermission(Commands.RELOAD.get()) && !sender.hasPermission(Commands.VERSION.get())) return;
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("VillagerOptimizer Commands").color(VillagerOptimizer.plugin_style.color()));
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        subCommands.forEach(subCommand -> sender.sendMessage(
                subCommand.getSyntax().append(Component.text(" - ").color(NamedTextColor.DARK_GRAY)).append(subCommand.getDescription())));
        sender.sendMessage(
                Component.text("/optimizevillagers <blockradius>").color(VillagerOptimizer.plugin_style.color())
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Optimize villagers in a radius").color(NamedTextColor.GRAY))
        );
        sender.sendMessage(
                Component.text("/unoptmizevillagers <blockradius>").color(VillagerOptimizer.plugin_style.color())
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Unoptimize villagers in a radius").color(NamedTextColor.GRAY))
        );
        sender.sendMessage(Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
    }
}