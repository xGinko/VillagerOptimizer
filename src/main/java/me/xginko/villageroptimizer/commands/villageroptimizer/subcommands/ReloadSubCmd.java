package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.utils.GenericUtil;
import me.xginko.villageroptimizer.utils.KyoriUtil;
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
        return Component.text("/villageroptimizer reload").color(GenericUtil.COLOR);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.Commands.RELOAD.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return;
        }

        KyoriUtil.sendMessage(sender, Component.text("Reloading VillagerOptimizer...").color(NamedTextColor.WHITE));
        VillagerOptimizer.getFoliaLib().getImpl().runNextTick(reload -> { // Reload in sync with the server
            VillagerOptimizer.getInstance().reloadPlugin();
            KyoriUtil.sendMessage(sender, Component.text("Reload complete.").color(NamedTextColor.GREEN));
        });
    }
}