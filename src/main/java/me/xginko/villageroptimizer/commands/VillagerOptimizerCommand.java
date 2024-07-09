package me.xginko.villageroptimizer.commands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.utils.Disableable;
import me.xginko.villageroptimizer.utils.Enableable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class VillagerOptimizerCommand extends Command
        implements Enableable, Disableable, PluginIdentifiableCommand, CommandExecutor, TabCompleter  {

    public static final Set<VillagerOptimizerCommand> COMMANDS = new HashSet<>();
    public static final List<String> RADIUS_SUGGESTIONS = Arrays.asList("5", "10", "25", "50");
    public static final Reflections COMMANDS_PACKAGE = new Reflections(VillagerOptimizerCommand.class.getPackage().getName());

    protected VillagerOptimizerCommand(
            @NotNull String name, @NotNull String description, @NotNull String usageMessage, @NotNull List<String> aliases
    ) {
        super(name, description, usageMessage, aliases);
    }

    public static void reloadCommands() {
        COMMANDS.forEach(Disableable::disable);
        COMMANDS.clear();

        COMMANDS.addAll(COMMANDS_PACKAGE.get(Scanners.SubTypes.of(VillagerOptimizerCommand.class).asClass())
                .stream()
                .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                .map(clazz -> {
                    try {
                        return (VillagerOptimizerCommand) clazz.getDeclaredConstructor().newInstance();
                    } catch (Throwable t) {
                        VillagerOptimizer.logger().warn("Failed initialising command '{}'. This should not happen.", clazz.getSimpleName());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        COMMANDS.forEach(Enableable::enable);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, @NotNull String[] args
    ) {
        return tabComplete(sender, commandLabel, args);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, @NotNull String[] args
    ) {
        return execute(sender, commandLabel, args);
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return VillagerOptimizer.getInstance();
    }

    @Override
    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        VillagerOptimizer.commandRegistration().getServerCommandMap()
                .register(plugin.getDescription().getName().toLowerCase(), this);
        plugin.getCommand(getName()).setExecutor(this);
        plugin.getCommand(getName()).setTabCompleter(this);
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void disable() {
        VillagerOptimizer.getInstance().getCommand(getName())
                .unregister(VillagerOptimizer.commandRegistration().getServerCommandMap());
    }
}
