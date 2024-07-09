package me.xginko.villageroptimizer.commands;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class SubCommand {

    public abstract @NotNull String label();
    public abstract @NotNull TextComponent description();
    public abstract @NotNull TextComponent syntax();
    public abstract @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
            throws CommandException, IllegalArgumentException;
    public abstract boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args);

}
