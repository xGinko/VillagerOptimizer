package me.xginko.villageroptimizer.modules.gameplay;

import com.cryptomorin.xseries.XEntityType;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.LocationUtil;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.time.Duration;

public class RestockOptimizedTrades extends VillagerOptimizerModule implements Listener {

    private final long restock_delay_millis;
    private final boolean log_enabled, notify_player;

    public RestockOptimizedTrades() {
        super("gameplay.restock-optimized-trades");
        config.master().addComment(configPath,
                "This is for automatic restocking of trades for optimized villagers. Optimized Villagers\n" +
                "don't have enough AI to restock their trades naturally, so this is here as a workaround.");
        this.restock_delay_millis = config.getInt(configPath + ".delay-in-ticks", 1000,
                "1 second = 20 ticks. There are 24.000 ticks in a single minecraft day.") * 50L;
        this.notify_player = config.getBoolean(configPath + ".notify-player", true,
                "Sends the player a message when the trades were restocked on a clicked villager.");
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
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != XEntityType.VILLAGER.get()) return;

        final WrappedVillager wrapped = wrapperCache.get((Villager) event.getRightClicked(), WrappedVillager::new);
        if (!wrapped.isOptimized()) return;

        final Player player = event.getPlayer();
        final boolean player_bypassing = player.hasPermission(Permissions.Bypass.RESTOCK_COOLDOWN.get());
        if (!wrapped.canRestock(restock_delay_millis) && !player_bypassing) return;

        wrapped.restock();
        wrapped.saveRestockTime();

        if (notify_player && !player_bypassing) {
            final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                    .matchLiteral("%time%")
                    .replacement(Util.formatDuration(Duration.ofMillis(wrapped.getRestockCooldownMillis(restock_delay_millis))))
                    .build();
            VillagerOptimizer.getLang(player.locale()).trades_restocked
                    .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
        }

        if (log_enabled) {
            info("Restocked optimized villager at " + LocationUtil.toString(wrapped.villager.getLocation()));
        }
    }
}
