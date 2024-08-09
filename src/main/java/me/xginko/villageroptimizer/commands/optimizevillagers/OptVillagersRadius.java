package me.xginko.villageroptimizer.commands.optimizevillagers;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
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

import java.util.Collections;
import java.util.List;

public class OptVillagersRadius extends VillagerOptimizerCommand {

    private final long cooldown;
    private final int max_radius;

    public OptVillagersRadius() {
        super("optimizevillagers");
        Config config = VillagerOptimizer.config();
        this.max_radius = config.getInt("optimization-methods.commands.optimizevillagers.max-block-radius", 100);
        this.cooldown = config.getInt("optimization-methods.commands.optimizevillagers.cooldown-seconds", 600,
                "Cooldown in seconds until a villager can be optimized again using the command.\n" +
                "Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.") * 1000L;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, @NotNull String[] args
    ) {
        return args.length == 1 ? RADIUS_SUGGESTIONS : Collections.emptyList();
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, @NotNull String[] args
    ) {
        if (!sender.hasPermission(Permissions.Commands.OPTIMIZE_RADIUS.get())) {
            KyoriUtil.sendMessage(sender, VillagerOptimizer.getLang(sender).no_permission);
            return true;
        }

        if (!(sender instanceof Player)) {
            KyoriUtil.sendMessage(sender, Component.text("This command can only be executed by a player.")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            VillagerOptimizer.getLang(player.locale()).command_specify_radius
                    .forEach(line -> KyoriUtil.sendMessage(sender, line));
            return true;
        }

        try {
            final int specifiedRadius = Integer.parseInt(args[0]);
            // Turn negative numbers into positive ones
            final int safeRadius = (int) Math.sqrt(specifiedRadius * specifiedRadius);

            if (safeRadius == 0) {
                VillagerOptimizer.getLang(player.locale()).command_radius_invalid
                        .forEach(line -> KyoriUtil.sendMessage(sender, line));
                return true;
            }

            if (safeRadius > max_radius) {
                final TextReplacementConfig limit = TextReplacementConfig.builder()
                        .matchLiteral("%distance%")
                        .replacement(Integer.toString(max_radius))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_radius_limit_exceed
                        .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(limit)));
                return true;
            }

            int successCount = 0;
            int failCount = 0;
            final boolean player_has_cooldown_bypass = player.hasPermission(Permissions.Bypass.COMMAND_COOLDOWN.get());

            for (Entity entity : player.getNearbyEntities(safeRadius, safeRadius, safeRadius)) {
                if (!entity.getType().equals(EntityType.VILLAGER)) continue;
                Villager villager = (Villager) entity;
                Villager.Profession profession = villager.getProfession();
                if (profession.equals(Villager.Profession.NITWIT) || profession.equals(Villager.Profession.NONE)) continue;

                WrappedVillager wVillager = VillagerOptimizer.wrappers().get(villager, WrappedVillager::new);

                if (player_has_cooldown_bypass || wVillager.canOptimize(cooldown)) {
                    VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(wVillager, OptimizationType.COMMAND, player);
                    if (optimizeEvent.callEvent()) {
                        wVillager.setOptimizationType(optimizeEvent.getOptimizationType());
                        wVillager.saveOptimizeTime();
                        successCount++;
                    }
                } else {
                    failCount++;
                }
            }

            if (successCount <= 0 && failCount <= 0) {
                final TextReplacementConfig radius = TextReplacementConfig.builder()
                        .matchLiteral("%radius%")
                        .replacement(Integer.toString(safeRadius))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_no_villagers_nearby
                        .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(radius)));
                return true;
            }

            if (successCount > 0) {
                final TextReplacementConfig success_amount = TextReplacementConfig.builder()
                        .matchLiteral("%amount%")
                        .replacement(Integer.toString(successCount))
                        .build();
                final TextReplacementConfig radius = TextReplacementConfig.builder()
                        .matchLiteral("%radius%")
                        .replacement(Integer.toString(safeRadius))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_optimize_success
                        .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(success_amount).replaceText(radius)));
            }
            if (failCount > 0) {
                final TextReplacementConfig alreadyOptimized = TextReplacementConfig.builder()
                        .matchLiteral("%amount%")
                        .replacement(Integer.toString(failCount))
                        .build();
                VillagerOptimizer.getLang(player.locale()).command_optimize_fail
                        .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(alreadyOptimized)));
            }
        } catch (NumberFormatException e) {
            VillagerOptimizer.getLang(player.locale()).command_radius_invalid
                    .forEach(line -> KyoriUtil.sendMessage(player, line));
        }

        return true;
    }
}