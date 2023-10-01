package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

public class VersionSubCmd extends SubCommand {

    @Override
    public String getLabel() {
        return "version";
    }

    @Override
    public TextComponent getDescription() {
        return Component.text("Show the plugin version.").color(NamedTextColor.GRAY);
    }

    @Override
    public TextComponent getSyntax() {
        return Component.text("/villageroptimizer version").color(VillagerOptimizer.plugin_style.color());
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (sender.hasPermission(Permissions.Commands.VERSION.get())) {
            final PluginDescriptionFile pluginMeta = VillagerOptimizer.getInstance().getDescription();
            sender.sendMessage(
                    Component.newline()
                    .append(
                            Component.text(pluginMeta.getName()+" "+pluginMeta.getVersion())
                            .style(VillagerOptimizer.plugin_style)
                            .clickEvent(ClickEvent.openUrl(pluginMeta.getWebsite()))
                    )
                    .append(Component.text(" by ").color(NamedTextColor.GRAY))
                    .append(
                            Component.text(pluginMeta.getAuthors().get(0))
                            .color(NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.openUrl("https://github.com/xGinko"))
                    )
                    .append(Component.newline())
            );
        } else {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
        }
    }
}