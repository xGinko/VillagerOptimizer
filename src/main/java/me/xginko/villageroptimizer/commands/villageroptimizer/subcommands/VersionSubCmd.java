package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import io.papermc.paper.plugin.configuration.PluginMeta;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class VersionSubCmd extends SubCommand {

    @Override
    public @NotNull String label() {
        return "version";
    }

    @Override
    public @NotNull TextComponent description() {
        return Component.text("Show the plugin version.").color(NamedTextColor.GRAY);
    }

    @Override
    public @NotNull TextComponent syntax() {
        return Component.text("/villageroptimizer version").color(Util.PL_COLOR);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, @NotNull String[] args
    ) {
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, @NotNull String[] args
    ) {
        if (!sender.hasPermission(Permissions.Commands.VERSION.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return true;
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
                                .style(Util.PL_STYLE)
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

         return true;
    }
}