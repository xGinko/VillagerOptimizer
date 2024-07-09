package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class DisableSubCmd extends SubCommand {

    public DisableSubCmd() {
        super(
                "disable",
                Component.text("/villageroptimizer disable").color(Util.PL_COLOR),
                Component.text("Disable all plugin tasks and listeners.").color(NamedTextColor.GRAY)
        );
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
        if (!sender.hasPermission(Permissions.Commands.DISABLE.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return true;
        }

        KyoriUtil.sendMessage(sender, Component.text("Disabling VillagerOptimizer...").color(NamedTextColor.RED));
        VillagerOptimizerModule.ENABLED_MODULES.forEach(VillagerOptimizerModule::disable);
        VillagerOptimizerModule.ENABLED_MODULES.clear();
        VillagerOptimizer.getCache().cacheMap().clear();
        KyoriUtil.sendMessage(sender, Component.text("Disabled all plugin listeners and tasks.").color(NamedTextColor.GREEN));
        KyoriUtil.sendMessage(sender, Component.text("You can enable the plugin again using the reload command.").color(NamedTextColor.YELLOW));
        return true;
    }
}