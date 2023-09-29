package me.xginko.villageroptimizer.commands.optimizevillagers;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.commands.VillagerOptimizerCommand;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.WrappedVillager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class OptVillagersRadius implements VillagerOptimizerCommand, TabCompleter {

    private final List<String> tabCompletes = List.of("5", "10", "25", "50");
    private final Component optimizeName;
    private final long cooldown;
    private final int maxRadius;
    private final boolean shouldRename, overwrite_name;

    public OptVillagersRadius() {
        Config config = VillagerOptimizer.getConfiguration();
        this.maxRadius = config.getInt("optimization-methods.commands.optimizevillagers.max-block-radius", 100);
        this.cooldown = config.getInt("optimization-methods.commands.optimizevillagers.cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again using the command.\s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.""") * 1000L;
        this.shouldRename = config.getBoolean("optimization-methods.commands.rename-optimized-villagers.enable", true,
                "Renames villagers to what you configure below when they're optimized.");
        this.overwrite_name = config.getBoolean("optimization-methods.commands.rename-optimized-villagers.overwrite-previous-name", false,
                "Whether to overwrite the previous name or not.");
        this.optimizeName = MiniMessage.miniMessage().deserialize(config.getString("optimization-methods.commands.name-villager.name", "<green>Optimized",
                "The MiniMessage formatted name to give optimized villagers."));
    }

    @Override
    public String label() {
        return "optimizevillagers";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return args.length == 1 ? tabCompletes : Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be executed by a player.")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            return true;
        }

        if (sender.hasPermission(Permissions.Commands.OPTIMIZE_RADIUS.get())) {
            if (args.length != 1) {
                VillagerOptimizer.getLang(player.locale()).command_specify_radius.forEach(player::sendMessage);
                return true;
            }

            try {
                int specifiedRadius = Integer.parseInt(args[0]);

                if (specifiedRadius > maxRadius) {
                    final TextReplacementConfig limit = TextReplacementConfig.builder()
                            .matchLiteral("%distance%")
                            .replacement(Integer.toString(maxRadius))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).command_radius_limit_exceed.forEach(line -> player.sendMessage(line.replaceText(limit)));
                    return true;
                }

                VillagerCache villagerCache = VillagerOptimizer.getCache();
                int successCount = 0;
                int failCount = 0;

                for (Entity entity : player.getNearbyEntities(specifiedRadius, specifiedRadius, specifiedRadius)) {
                    if (!entity.getType().equals(EntityType.VILLAGER)) continue;
                    Villager villager = (Villager) entity;
                    Villager.Profession profession = villager.getProfession();
                    if (profession.equals(Villager.Profession.NITWIT) || profession.equals(Villager.Profession.NONE)) continue;

                    WrappedVillager wVillager = villagerCache.getOrAdd(villager);

                    if (wVillager.canOptimize(cooldown)) {
                        wVillager.setOptimization(OptimizationType.COMMAND);
                        wVillager.saveOptimizeTime();

                        if (shouldRename) {
                            if (overwrite_name) {
                                villager.customName(optimizeName);
                            } else {
                                if (villager.customName() == null)
                                    villager.customName(optimizeName);
                            }
                        }

                        successCount++;
                    } else {
                        failCount++;
                    }
                }

                if (successCount <= 0 && failCount <= 0) {
                    final TextReplacementConfig radius = TextReplacementConfig.builder()
                            .matchLiteral("%radius%")
                            .replacement(Integer.toString(specifiedRadius))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).command_no_villagers_nearby.forEach(line -> player.sendMessage(line.replaceText(radius)));
                    return true;
                }

                if (successCount > 0) {
                    final TextReplacementConfig success_amount = TextReplacementConfig.builder()
                            .matchLiteral("%amount%")
                            .replacement(Integer.toString(successCount))
                            .build();
                    final TextReplacementConfig radius = TextReplacementConfig.builder()
                            .matchLiteral("%radius%")
                            .replacement(Integer.toString(specifiedRadius))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).command_optimize_success.forEach(line -> player.sendMessage(line
                            .replaceText(success_amount)
                            .replaceText(radius)
                    ));
                }
                if (failCount > 0) {
                    final TextReplacementConfig alreadyOptimized = TextReplacementConfig.builder()
                            .matchLiteral("%amount%")
                            .replacement(Integer.toString(failCount))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).command_optimize_fail.forEach(line -> player.sendMessage(line.replaceText(alreadyOptimized)));
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