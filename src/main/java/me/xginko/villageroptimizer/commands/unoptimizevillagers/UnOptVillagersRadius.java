package me.xginko.villageroptimizer.commands.unoptimizevillagers;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.permissions.Commands;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UnOptVillagersRadius implements VillagerOptimizerCommand {

    private final int max_radius;

    public UnOptVillagersRadius() {
        this.max_radius = VillagerOptimizer.getConfiguration().getInt("optimization-methods.commands.unoptimizevillagers.max-block-radius", 100);
    }

    @Override
    public String label() {
        return "unoptimizevillagers";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? RADIUS_TABCOMPLETES : NO_TABCOMPLETES;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission(Commands.UNOPTIMIZE_RADIUS.get())) {
            sender.sendMessage(VillagerOptimizer.getLang(sender).no_permission);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by a player.")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            return true;
        }

        if (args.length != 1) {
            VillagerOptimizer.getLang(player.locale()).command_specify_radius.forEach(player::sendMessage);
            return true;
        }

        try {
            final int specifiedRadius = Integer.parseInt(args[0]);
            // Turn negative numbers into positive ones
            final int safeRadius = (int) Math.sqrt(specifiedRadius * specifiedRadius);

            if (safeRadius == 0) {
                VillagerOptimizer.getLang(player.locale()).command_radius_invalid.forEach(player::sendMessage);
                return true;
            }

            if (safeRadius > max_radius) {
                final TextReplacementConfig limit = TextReplacementConfig.builder()
                        .matchLiteral("%distance%")
                        .replacement(Integer.toString(max_radius))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_radius_limit_exceed.forEach(line -> player.sendMessage(line.replaceText(limit)));
                return true;
            }

            VillagerCache villagerCache = VillagerOptimizer.getCache();
            int successCount = 0;

            for (Entity entity : player.getNearbyEntities(safeRadius, safeRadius, safeRadius)) {
                if (!entity.getType().equals(EntityType.VILLAGER)) continue;
                Villager villager = (Villager) entity;
                Villager.Profession profession = villager.getProfession();
                if (profession.equals(Villager.Profession.NITWIT) || profession.equals(Villager.Profession.NONE)) continue;

                WrappedVillager wVillager = villagerCache.getOrAdd(villager);

                if (wVillager.isOptimized()) {
                    VillagerUnoptimizeEvent unOptimizeEvent = new VillagerUnoptimizeEvent(wVillager, player, OptimizationType.COMMAND);
                    if (unOptimizeEvent.callEvent()) {
                        wVillager.setOptimizationType(OptimizationType.NONE);
                        successCount++;
                    }
                }
            }

            if (successCount <= 0) {
                final TextReplacementConfig radius = TextReplacementConfig.builder()
                        .matchLiteral("%radius%")
                        .replacement(Integer.toString(safeRadius))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_no_villagers_nearby.forEach(line -> player.sendMessage(line.replaceText(radius)));
            } else {
                final TextReplacementConfig success_amount = TextReplacementConfig.builder()
                        .matchLiteral("%amount%")
                        .replacement(Integer.toString(successCount))
                        .build();
                final TextReplacementConfig radius = TextReplacementConfig.builder()
                        .matchLiteral("%radius%")
                        .replacement(Integer.toString(safeRadius))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_unoptimize_success.forEach(line -> player.sendMessage(line
                        .replaceText(success_amount)
                        .replaceText(radius)
                ));
            }
        } catch (NumberFormatException e) {
            VillagerOptimizer.getLang(player.locale()).command_radius_invalid.forEach(player::sendMessage);
        }

        return true;
    }
}