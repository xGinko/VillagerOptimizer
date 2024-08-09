package me.xginko.villageroptimizer.modules.optimization;

import com.cryptomorin.xseries.XEntityType;
import com.cryptomorin.xseries.XMaterial;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.events.VillagerOptimizeEvent;
import me.xginko.villageroptimizer.events.VillagerUnoptimizeEvent;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.LocationUtil;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OptimizeByNametag extends VillagerOptimizerModule implements Listener {

    private final Set<String> nametags;
    private final long cooldown;
    private final boolean consume_nametag, notify_player, log_enabled;

    public OptimizeByNametag() {
        super("optimization-methods.nametag-optimization");
        config.master().addComment(configPath + ".enable",
                "Enable optimization by naming villagers to one of the names configured below.\n" +
                "Nametag optimized villagers will be unoptimized again when they are renamed to something else.");
        this.nametags = config.getList(configPath + ".names", Arrays.asList("Optimize", "DisableAI"),
                "Names are case insensitive, capital letters won't matter.")
                .stream().map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));
        this.consume_nametag = config.getBoolean(configPath + ".nametags-get-consumed", true,
                "Enable or disable consumption of the used nametag item.");
        this.cooldown = TimeUnit.SECONDS.toMillis(
                config.getInt(configPath + ".optimize-cooldown-seconds", 600,
                "Cooldown in seconds until a villager can be optimized again using a nametag.\n" +
                "Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior."));
        this.notify_player = config.getBoolean(configPath + ".notify-player", true,
                "Sends players a message when they successfully optimized a villager.");
        this.log_enabled = config.getBoolean(configPath + ".log", false);
    }

    @Override
    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean shouldEnable() {
        return config.getBoolean(configPath + ".enable", true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != XEntityType.VILLAGER.get()) return;
        final Player player = event.getPlayer();
        if (!player.hasPermission(Permissions.Optimize.NAMETAG.get())) return;

        final ItemStack usedItem = player.getInventory().getItem(event.getHand());
        if (usedItem != null && usedItem.getType() != XMaterial.NAME_TAG.parseMaterial()) return;
        if (!usedItem.hasItemMeta()) return;
        final ItemMeta meta = usedItem.getItemMeta();
        if (!meta.hasDisplayName()) return;

        final String nameTagPlainText = ChatColor.stripColor(meta.getDisplayName());
        final WrappedVillager wrapped = wrapperCache.get((Villager) event.getRightClicked(), WrappedVillager::new);

        if (nametags.contains(nameTagPlainText.toLowerCase())) {
            if (wrapped.canOptimize(cooldown) || player.hasPermission(Permissions.Bypass.NAMETAG_COOLDOWN.get())) {
                VillagerOptimizeEvent optimizeEvent = new VillagerOptimizeEvent(
                        wrapped,
                        OptimizationType.NAMETAG,
                        player,
                        event.isAsynchronous()
                );

                if (!optimizeEvent.callEvent()) return;

                if (!consume_nametag) {
                    player.getInventory().addItem(usedItem.asOne());
                    player.updateInventory();
                }

                wrapped.setOptimizationType(optimizeEvent.getOptimizationType());
                wrapped.saveOptimizeTime();

                if (notify_player) {
                    VillagerOptimizer.getLang(player.locale()).nametag_optimize_success
                            .forEach(line -> KyoriUtil.sendMessage(player, line));
                }

                if (log_enabled) {
                    info(player.getName() + " optimized villager using nametag '" + nameTagPlainText + "' at " +
                         LocationUtil.toString(wrapped.villager.getLocation()));
                }
            } else {
                event.setCancelled(true);
                wrapped.sayNo();
                if (notify_player) {
                    final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                            .matchLiteral("%time%")
                            .replacement(Util.formatDuration(Duration.ofMillis(wrapped.getOptimizeCooldownMillis(cooldown))))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown
                            .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
                }
            }
        } else {
            if (wrapped.isOptimized()) {
                VillagerUnoptimizeEvent unOptimizeEvent = new VillagerUnoptimizeEvent(
                        wrapped,
                        player,
                        OptimizationType.NAMETAG,
                        event.isAsynchronous()
                );

                if (!unOptimizeEvent.callEvent()) return;
                wrapped.setOptimizationType(OptimizationType.NONE);

                if (notify_player) {
                    VillagerOptimizer.getLang(player.locale()).nametag_unoptimize_success
                            .forEach(line -> KyoriUtil.sendMessage(player, line));
                }

                if (log_enabled) {
                    info(player.getName() + " unoptimized villager using nametag '" + nameTagPlainText + "' at " +
                         LocationUtil.toString(wrapped.villager.getLocation()));
                }
            }
        }
    }
}