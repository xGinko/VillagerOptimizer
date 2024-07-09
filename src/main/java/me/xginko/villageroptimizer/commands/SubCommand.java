package me.xginko.villageroptimizer.commands;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public abstract class SubCommand implements CommandExecutor, TabCompleter {

    private final String label;
    private final TextComponent syntax, description;

    public SubCommand(String label, TextComponent syntax, TextComponent description) {
        this.label = label;
        this.syntax = syntax;
        this.description = description;
    }

    public @NotNull String mergeArgs(@NotNull String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    public @NotNull String label() {
        return label;
    }

    public @NotNull TextComponent syntax() {
        return syntax;
    }

    public @NotNull TextComponent description() {
        return description;
    }
}
