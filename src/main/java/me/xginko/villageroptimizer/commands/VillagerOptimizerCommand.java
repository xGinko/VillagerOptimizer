package me.xginko.villageroptimizer.commands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.utils.Disableable;
import me.xginko.villageroptimizer.utils.Enableable;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class VillagerOptimizerCommand implements Enableable, Disableable, CommandExecutor, TabCompleter  {

    public static final Set<VillagerOptimizerCommand> COMMANDS = new HashSet<>();
    public static final List<String> RADIUS_SUGGESTIONS = Arrays.asList("5", "10", "25", "50");
    public static final Reflections COMMANDS_PACKAGE = new Reflections(VillagerOptimizerCommand.class.getPackage().getName());

    public final PluginCommand pluginCommand;

    protected VillagerOptimizerCommand(@NotNull String name) throws CommandException {
        PluginCommand pluginCommand = VillagerOptimizer.getInstance().getCommand(name);
        if (pluginCommand != null) this.pluginCommand = pluginCommand;
        else throw new CommandException("Command cannot be enabled because it's not defined in the plugin.yml.");
    }

    public static void reloadCommands() {
        COMMANDS.forEach(VillagerOptimizerCommand::disable);
        COMMANDS.clear();

        for (Class<?> clazz : COMMANDS_PACKAGE.get(Scanners.SubTypes.of(VillagerOptimizerCommand.class).asClass())) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;

            try {
                COMMANDS.add((VillagerOptimizerCommand) clazz.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                VillagerOptimizer.logger().error("Failed initialising command class '{}'.", clazz.getSimpleName(), e);
            }
        }

        COMMANDS.forEach(VillagerOptimizerCommand::enable);
    }

    @Override
    public void enable() {
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public void disable() {
        pluginCommand.unregister(VillagerOptimizer.commandRegistration().getServerCommandMap());
    }
}
