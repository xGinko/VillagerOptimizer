package me.xginko.villageroptimizer.commands.unoptimizevillagers;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.enums.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnOptVillagersRadius implements VillagerOptimizerCommand {

    @Override
    public String label() {
        return "unoptimizevillagers";
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission(Permissions.Commands.UNOPTIMIZE_RADIUS.get())) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be executed as a player.")
                        .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                return true;
            }

            if (args.length < 1) {

                return true;
            }


        } else {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
        }
        return true;
    }
}
