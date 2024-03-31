package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.wrapper.WrappedVillager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.GenericUtil;
import me.xginko.villageroptimizer.utils.KyoriUtil;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.time.Duration;

public class RestockOptimizedTrades implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final long restock_delay_millis;
    private final boolean log_enabled, notify_player;

    public RestockOptimizedTrades() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment(configPath(),
                "This is for automatic restocking of trades for optimized villagers. Optimized Villagers\n" +
                "don't have enough AI to restock their trades naturally, so this is here as a workaround.");
        this.restock_delay_millis = config.getInt(configPath() + ".delay-in-ticks", 1000,
                "1 second = 20 ticks. There are 24.000 ticks in a single minecraft day.") * 50L;
        this.notify_player = config.getBoolean(configPath() + ".notify-player", true,
                "Sends the player a message when the trades were restocked on a clicked villager.");
        this.log_enabled = config.getBoolean(configPath() + ".log", false);
    }

    @Override
    public String configPath() {
        return "gameplay.restock-optimized-trades";
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
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;

        final WrappedVillager wVillager = villagerCache.getOrAdd((Villager) event.getRightClicked());
        if (!wVillager.isOptimized()) return;

        final Player player = event.getPlayer();
        final boolean player_bypassing = player.hasPermission(Permissions.Bypass.RESTOCK_COOLDOWN.get());

        if (wVillager.canRestock(restock_delay_millis) || player_bypassing) {
            wVillager.restock();
            wVillager.saveRestockTime();

            if (notify_player && !player_bypassing) {
                final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                        .matchLiteral("%time%")
                        .replacement(GenericUtil.formatDuration(Duration.ofMillis(wVillager.getRestockCooldownMillis(restock_delay_millis))))
                        .build();
                VillagerOptimizer.getLang(player.locale()).trades_restocked
                        .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
            }

            if (log_enabled) {
                info("Restocked optimized villager at " + GenericUtil.formatLocation(wVillager.villager().getLocation()));
            }
        }
    }
}
