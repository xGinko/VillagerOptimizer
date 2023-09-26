package me.xginko.villageroptimizer.modules.optimizations;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.CommonUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;

public class OptimizeByNametag implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final HashSet<String> nametags = new HashSet<>(4);
    private final boolean shouldLog, shouldNotifyPlayer, consumeNametag;
    private final long cooldown;

    public OptimizeByNametag() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization-methods.nametag-optimization.enable", """
                Enable optimization by naming villagers to one of the names configured below.\s
                Nametag optimized villagers will be unoptimized again when they are renamed to something else.""");
        this.nametags.addAll(config.getList("optimization-methods.nametag-optimization.names", List.of("Optimize", "DisableAI"),
                "Names are case insensitive, capital letters won't matter.").stream().map(String::toLowerCase).toList());
        this.consumeNametag = config.getBoolean("optimization-methods.nametag-optimization.nametags-get-consumed", true,
                "Enable or disable consumption of the used nametag item.");
        this.cooldown = config.getInt("optimization-methods.nametag-optimization.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again using a nametag.\s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.""") * 1000L;
        this.shouldNotifyPlayer = config.getBoolean("optimization-methods.nametag-optimization.notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.shouldLog = config.getBoolean("optimization-methods.nametag-optimization.log", false);
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("optimization-methods.nametag-optimization.enable", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerNameEntity(PlayerNameEntityEvent event) {
        if (!event.getEntity().getType().equals(EntityType.VILLAGER)) return;
        Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.NAMETAG.get())) return;
        Component name = event.getName();
        if (name == null) return;

        final String nameTag = PlainTextComponentSerializer.plainText().serialize(name);
        WrappedVillager wVillager = villagerCache.getOrAdd((Villager) event.getEntity());

        if (nametags.contains(nameTag.toLowerCase())) {
            if (wVillager.isOptimized()) return;

            if (wVillager.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.NAMETAG_COOLDOWN.get())) {
                wVillager.setOptimization(OptimizationType.NAMETAG);
                wVillager.saveOptimizeTime();
                if (!consumeNametag) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemStack offHand = player.getInventory().getItemInOffHand();
                    if (mainHand.getType().equals(Material.NAME_TAG)) mainHand.add();
                    else if (offHand.getType().equals(Material.NAME_TAG)) offHand.add();
                }
                if (shouldNotifyPlayer)
                    VillagerOptimizer.getLang(player.locale()).nametag_optimize_success.forEach(player::sendMessage);
                if (shouldLog)
                    VillagerOptimizer.getLog().info(player.getName() + " optimized a villager using nametag: '" + nameTag + "'");
            } else {
                event.setCancelled(true);
                wVillager.villager().shakeHead();
                if (shouldNotifyPlayer) {
                    final String time = CommonUtils.formatTime(wVillager.getOptimizeCooldownMillis(cooldown));
                    VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(time).build())));
                }
            }
        } else {
            if (wVillager.getOptimizationType().equals(OptimizationType.NAMETAG)) {
                wVillager.setOptimization(OptimizationType.NONE);
                if (shouldNotifyPlayer)
                    VillagerOptimizer.getLang(player.locale()).nametag_unoptimize_success.forEach(player::sendMessage);
                if (shouldLog)
                    VillagerOptimizer.getLog().info(event.getPlayer().getName() + " disabled optimizations for a villager using nametag: '" + nameTag + "'");
            }
        }
    }
}
