package me.xginko.villageroptimizer.commands;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public abstract class SubCommand implements CommandExecutor, TabCompleter {

    public abstract @NotNull String label();
    public abstract @NotNull TextComponent description();
    public abstract @NotNull TextComponent syntax();

}
