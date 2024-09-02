package me.xginko.villageroptimizer.modules.gameplay;

import com.cryptomorin.xseries.XEntityType;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.struct.enums.Permissions;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import me.xginko.villageroptimizer.utils.LocationUtil;
import me.xginko.villageroptimizer.utils.Util;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class RestockOptimizedTrades extends VillagerOptimizerModule implements Listener {

    private final SortedSet<Long> restockDayTimes;
    private final boolean log_enabled, notify_player;

    public RestockOptimizedTrades() {
        super("gameplay.restock-optimized-trades");
        config.master().addComment(configPath,
                "This is for automatic restocking of trades for optimized villagers. Optimized Villagers\n" +
                "don't have enough AI to restock their trades naturally, so this is here as a workaround.");
        this.restockDayTimes = new TreeSet<>(Comparator.reverseOrder());
        this.restockDayTimes.addAll(config.getList(configPath + ".restock-times", Arrays.asList(1000L, 13000L),
                "At which (tick-)times during the day villagers will restock.\n" +
                        "There are 24.000 ticks in a single minecraft day."));
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
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != XEntityType.VILLAGER.get()) return;

        WrappedVillager wrapped = wrapperCache.get((Villager) event.getRightClicked(), WrappedVillager::new);
        if (!wrapped.isOptimized()) return;

        if (event.getPlayer().hasPermission(Permissions.Bypass.RESTOCK_COOLDOWN.get())) {
            wrapped.restock();
            return;
        }

        long lastRestockFullTimeTicks = wrapped.getLastRestockFullTime();
        long currentFullTimeTicks = wrapped.currentFullTimeTicks();
        long currentDayTimeTicks = wrapped.currentDayTimeTicks();

        long currentDay = currentFullTimeTicks - currentDayTimeTicks;
        long ticksTillRestock = (24000 + currentDay + restockDayTimes.first()) - currentFullTimeTicks;

        boolean restocked = false;

        for (Long restockDayTime : restockDayTimes) {
            long restockTimeToday = currentDay + restockDayTime;

            if (currentFullTimeTicks < restockTimeToday || lastRestockFullTimeTicks >= restockTimeToday) {
                ticksTillRestock = Math.min(ticksTillRestock, restockTimeToday - currentFullTimeTicks);
                continue;
            }

            if (!restocked) {
                wrapped.restock();
                wrapped.saveRestockTime();
                restocked = true;
            }
        }

        if (!restocked) return;

        if (notify_player) {
            final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                    .matchLiteral("%time%")
                    .replacement(Util.formatDuration(Duration.ofMillis(ticksTillRestock * 50L)))
                    .build();
            VillagerOptimizer.getLang(event.getPlayer().locale()).trades_restocked
                    .forEach(line -> KyoriUtil.sendMessage(event.getPlayer(), line.replaceText(timeLeft)));
        }

        if (log_enabled) {
            info("Restocked optimized villager at " + LocationUtil.toString(wrapped.villager.getLocation()));
        }
    }
}
