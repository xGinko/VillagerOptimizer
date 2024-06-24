package me.xginko.villageroptimizer.commands.villageroptimizer.subcommands;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.SubCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.utils.KyoriUtil;
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
        return Component.text("/villageroptimizer disable").color(Util.PL_COLOR);
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.Commands.DISABLE.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return;
        }

        KyoriUtil.sendMessage(sender, Component.text("Disabling VillagerOptimizer...").color(NamedTextColor.RED));
        VillagerOptimizerModule.ENABLED_MODULES.forEach(VillagerOptimizerModule::disable);
        VillagerOptimizerModule.ENABLED_MODULES.clear();
        VillagerOptimizer.getCache().cacheMap().clear();
        KyoriUtil.sendMessage(sender, Component.text("Disabled all plugin listeners and tasks.").color(NamedTextColor.GREEN));
        KyoriUtil.sendMessage(sender, Component.text("You can enable the plugin again using the reload command.").color(NamedTextColor.YELLOW));
    }
}