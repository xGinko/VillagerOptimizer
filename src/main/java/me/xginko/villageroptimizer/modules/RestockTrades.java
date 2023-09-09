package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class RestockTrades implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final long restock_delay_millis;
    private final boolean shouldLog, notifyPlayer;

    protected RestockTrades() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization.trade-restocking.enable", """
                This is for automatic restocking of trades for optimized villagers. Optimized Villagers\s
                Don't have enough AI to do trade restocks themselves, so this needs to always be enabled.
                """);
        this.restock_delay_millis = config.getInt("optimization.trade-restocking.delay-in-ticks", 1000,
                "1 second = 20 ticks. There are 24.000 ticks in a single minecraft day.") * 50L;
        this.shouldLog = config.getBoolean("optimization.trade-restocking.log", false);
        this.notifyPlayer = config.getBoolean("optimization.trade-restocking.notify-player", true,
                "Sends the player a message when the trades were restocked on a clicked villager.");
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.trade-restocking.enable", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;

        WrappedVillager wVillager = villagerManager.getOrAdd((Villager) event.getRightClicked());

        if (wVillager.isOptimized() && wVillager.canRestock(restock_delay_millis)) {
            wVillager.restock();
            wVillager.saveRestockTime();
            if (notifyPlayer) {
                Player player = event.getPlayer();
                VillagerOptimizer.getLang(player.locale()).trades_restocked.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(CommonUtils.formatTime(restock_delay_millis)).build()))
                );
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info("Restocked optimized villager at "+ wVillager.villager().getLocation());
        }
    }
}
