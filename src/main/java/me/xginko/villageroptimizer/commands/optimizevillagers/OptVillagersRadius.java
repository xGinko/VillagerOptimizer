package me.xginko.villageroptimizer.commands.optimizevillagers;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.models.WrappedVillager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OptVillagersRadius implements VillagerOptimizerCommand, TabCompleter {

    /*
    * TODO: Radius limit, Cooldown, Compatibility with other types
    *
    * */

    @Override
    public String label() {
        return "optimizevillagers";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of("5", "10", "25", "50");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender.hasPermission(Permissions.Commands.OPTIMIZE_RADIUS.get())) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be executed as a player.")
                        .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                return true;
            }

            if (args.length != 1) {
                VillagerOptimizer.getLang(player.locale()).command_specify_radius.forEach(player::sendMessage);
                return true;
            }

            try {
                int specifiedRadius = Integer.parseInt(args[0]) / 2;

                VillagerManager villagerManager = VillagerOptimizer.getVillagerManager();
                int successCount = 0;
                int failCount = 0;

                for (Entity entity : player.getNearbyEntities(specifiedRadius, specifiedRadius, specifiedRadius)) {
                    if (!entity.getType().equals(EntityType.VILLAGER)) continue;
                    Villager villager = (Villager) entity;
                    Villager.Profession profession = villager.getProfession();
                    if (profession.equals(Villager.Profession.NITWIT) || profession.equals(Villager.Profession.NONE)) continue;

                    WrappedVillager wVillager = villagerManager.getOrAdd(villager);

                    if (!wVillager.isOptimized()) {
                        wVillager.setOptimization(OptimizationType.COMMAND);
                        wVillager.saveOptimizeTime();
                        successCount++;
                    } else {
                        failCount++;
                    }
                }

                final String success = Integer.toString(successCount);
                final String radius = Integer.toString(specifiedRadius);
                VillagerOptimizer.getLang(player.locale()).command_optimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%amount%").replacement(success).build())
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%radius%").replacement(radius).build())
                ));
                if (failCount > 0) {
                    final String alreadyOptimized = Integer.toString(failCount);
                    VillagerOptimizer.getLang(player.locale()).command_optimize_fail.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%amount%").replacement(alreadyOptimized).build())
                    ));
                }
            } catch (NumberFormatException e) {
                VillagerOptimizer.getLang(player.locale()).command_radius_invalid.forEach(player::sendMessage);
            }
        } else {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
        }
        return true;
    }
}
