package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class DisableSubCmd extends SubCommand {

    @Override
    public String getLabel() {
        return "disable";
    }

    @Override
    public TextComponent getDescription() {
        return Component.text("Disable all plugin tasks and listeners.").color(NamedTextColor.GRAY);
    }

    @Override
    public TextComponent getSyntax() {
        return Component.text("/villageroptimizer disable").color(VillagerOptimizer.plugin_style.color());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender.hasPermission(Permissions.Commands.DISABLE.get())) {
            sender.sendMessage(Component.text("Disabling VillagerOptimizer...").color(NamedTextColor.RED));
            VillagerOptimizerModule.modules.forEach(VillagerOptimizerModule::disable);
            VillagerOptimizerModule.modules.clear();
            VillagerOptimizer.getCache().cacheMap().clear();
            sender.sendMessage(Component.text("Disabled all plugin listeners and tasks.").color(NamedTextColor.GREEN));
            sender.sendMessage(Component.text("You can enable the plugin again using the reload command.").color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
        }
    }
}