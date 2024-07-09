package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ReloadSubCmd extends SubCommand {

    @Override
    public @NotNull String label() {
        return "reload";
    }

    @Override
    public @NotNull TextComponent description() {
        return Component.text("Reload the plugin configuration.").color(NamedTextColor.GRAY);
    }

    @Override
    public @NotNull TextComponent syntax() {
        return Component.text("/villageroptimizer reload").color(Util.PL_COLOR);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
            throws CommandException, IllegalArgumentException
    {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!sender.hasPermission(Permissions.Commands.RELOAD.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return true;
        }

        KyoriUtil.sendMessage(sender, Component.text("Reloading VillagerOptimizer...").color(NamedTextColor.WHITE));
        VillagerOptimizer.scheduling().asyncScheduler().run(reload -> {
            VillagerOptimizer.getInstance().reloadPlugin();
            KyoriUtil.sendMessage(sender, Component.text("Reload complete.").color(NamedTextColor.GREEN));
        });
        return true;
    }
}