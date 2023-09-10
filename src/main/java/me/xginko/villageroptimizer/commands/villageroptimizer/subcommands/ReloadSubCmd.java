package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ReloadSubCmd extends SubCommand {
    @Override
    public String getLabel() {
        return "reload";
    }
    @Override
    public TextComponent getDescription() {
        return Component.text("Reload the plugin configuration.").color(NamedTextColor.GRAY);
    }
    @Override
    public TextComponent getSyntax() {
        return Component.text("/villageroptimizer reload").color(NamedTextColor.BLUE);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender.hasPermission(Permissions.Commands.RELOAD.get())) {
            sender.sendMessage(Component.text("Reloading VillagerOptimizer...").color(NamedTextColor.BLUE));
            VillagerOptimizer plugin = VillagerOptimizer.getInstance();
            plugin.getServer().getAsyncScheduler().runNow(plugin, reloadPlugin -> {
                plugin.reloadPlugin();
                sender.sendMessage(Component.text("Reload complete.").color(NamedTextColor.AQUA));
            });
        } else {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
        }
    }
}
