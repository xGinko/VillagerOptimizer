package me.xginko.villageroptimizer.commands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.utils.Disableable;
import me.xginko.villageroptimizer.utils.Enableable;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class VillagerOptimizerCommand implements Enableable, Disableable, CommandExecutor, TabCompleter  {

    public static final Set<VillagerOptimizerCommand> COMMANDS = new HashSet<>();
    public static final List<String> RADIUS_SUGGESTIONS = Arrays.asList("5", "10", "25", "50");
    public static final Reflections COMMANDS_PACKAGE = new Reflections(VillagerOptimizerCommand.class.getPackage().getName());

    public final String label;

    protected VillagerOptimizerCommand(@NotNull String name) {
        this.label = name;
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
    @SuppressWarnings("DataFlowIssue")
    public void enable() {
        PluginCommand pluginCommand = VillagerOptimizer.getInstance().getCommand(label);
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void disable() {
        VillagerOptimizer.getInstance().getCommand(label)
                .unregister(VillagerOptimizer.commandRegistration().getServerCommandMap());
    }
}
