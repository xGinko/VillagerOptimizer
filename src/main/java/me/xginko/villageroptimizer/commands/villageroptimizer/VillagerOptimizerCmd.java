package me.xginko.villageroptimizer.commands.villageroptimizer;

import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.commands.villageroptimizer.subcommands.DisableSubCmd;
import me.xginko.villageroptimizer.commands.villageroptimizer.subcommands.ReloadSubCmd;
import me.xginko.villageroptimizer.commands.villageroptimizer.subcommands.VersionSubCmd;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VillagerOptimizerCmd extends VillagerOptimizerCommand {

    private final List<SubCommand> subCommands;
    private final List<String> tabCompletes;

    public VillagerOptimizerCmd() {
        super(
                "villageroptimizer",
                "VillagerOptimizer admin commands",
                "/villageroptimizer [ reload, version, disable ]",
                Arrays.asList("voptimizer", "vo")
        );
        subCommands = Arrays.asList(new ReloadSubCmd(), new VersionSubCmd(), new DisableSubCmd());
        tabCompletes = subCommands.stream().map(SubCommand::label).collect(Collectors.toList());
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
            throws CommandException, IllegalArgumentException
    {
        if (args.length == 1) {
            return tabCompletes;
        }

        if (args.length >= 2) {
            for (SubCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.label())) {
                    return subCommand.tabComplete(sender, alias, args);
                }
            }
        }

        return Collections.emptyList();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length >= 1) {
            for (SubCommand subCommand : subCommands) {
                if (args[0].equalsIgnoreCase(subCommand.label())) {
                    return subCommand.execute(sender, commandLabel, args);
                }
            }
        }

        overview(sender);
        return true;
    }

    private void overview(CommandSender sender) {
        if (!sender.hasPermission(Permissions.Commands.RELOAD.get()) && !sender.hasPermission(Permissions.Commands.VERSION.get())) return;
        KyoriUtil.sendMessage(sender, Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        KyoriUtil.sendMessage(sender, Component.text("VillagerOptimizer Commands").color(Util.PL_COLOR));
        KyoriUtil.sendMessage(sender, Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
        subCommands.forEach(subCommand -> KyoriUtil.sendMessage(sender,
                subCommand.syntax().append(Component.text(" - ").color(NamedTextColor.DARK_GRAY)).append(subCommand.description())));
        KyoriUtil.sendMessage(sender,
                Component.text("/optimizevillagers <blockradius>").color(Util.PL_COLOR)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Optimize villagers in a radius").color(NamedTextColor.GRAY))
        );
        KyoriUtil.sendMessage(sender,
                Component.text("/unoptmizevillagers <blockradius>").color(Util.PL_COLOR)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Unoptimize villagers in a radius").color(NamedTextColor.GRAY))
        );
        KyoriUtil.sendMessage(sender, Component.text("-----------------------------------------------------").color(NamedTextColor.GRAY));
    }
}