package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import io.papermc.paper.plugin.configuration.PluginMeta;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.utils.GenericUtil;
import me.xginko.villageroptimizer.utils.KyoriUtil;
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
        return Component.text("/villageroptimizer version").color(GenericUtil.COLOR);
    }

    @Override
    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.Commands.VERSION.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return;
        }

        String name, version, website, author;

        try {
            final PluginMeta pluginMeta = VillagerOptimizer.getInstance().getPluginMeta();
            name = pluginMeta.getName();
            version = pluginMeta.getVersion();
            website = pluginMeta.getWebsite();
            author = pluginMeta.getAuthors().get(0);
        } catch (Throwable versionIncompatible) {
            final PluginDescriptionFile pluginYML = VillagerOptimizer.getInstance().getDescription();
            name = pluginYML.getName();
            version = pluginYML.getVersion();
            website = pluginYML.getWebsite();
            author = pluginYML.getAuthors().get(0);
        }

        KyoriUtil.sendMessage(sender, Component.newline()
                .append(
                        Component.text(name + " " + version)
                                .style(GenericUtil.STYLE)
                                .clickEvent(ClickEvent.openUrl(website))
                )
                .append(Component.text(" by ").color(NamedTextColor.GRAY))
                .append(
                        Component.text(author)
                                .color(NamedTextColor.WHITE)
                                .clickEvent(ClickEvent.openUrl("https://github.com/xGinko"))
                )
                .append(Component.newline())
        );
    }
}