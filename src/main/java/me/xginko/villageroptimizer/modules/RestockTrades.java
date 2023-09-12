package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.CachedVillagers;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.WrappedVillager;
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

    /*
     * TODO: Disable notify message for cooldown bypassers
     * */

    private final CachedVillagers cachedVillagers;
    private final long restock_delay_millis;
    private final boolean shouldLog, notifyPlayer;

    protected RestockTrades() {
        shouldEnable();
        this.cachedVillagers = VillagerOptimizer.getCachedVillagers();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("gameplay.trade-restocking.enable", """
                This is for automatic restocking of trades for optimized villagers. Optimized Villagers\s
                Don't have enough AI to do trade restocks themselves, so this needs to always be enabled.""");
        this.restock_delay_millis = config.getInt("gameplay.trade-restocking.delay-in-ticks", 1000,
                "1 second = 20 ticks. There are 24.000 ticks in a single minecraft day.") * 50L;
        this.notifyPlayer = config.getBoolean("gameplay.trade-restocking.notify-player", true,
                "Sends the player a message when the trades were restocked on a clicked villager.");
        this.shouldLog = config.getBoolean("gameplay.trade-restocking.log", false);
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
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.trade-restocking.enable", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;

        WrappedVillager wVillager = cachedVillagers.getOrAdd((Villager) event.getRightClicked());
        if (!wVillager.isOptimized()) return;
        Player player = event.getPlayer();

        if (wVillager.canRestock(restock_delay_millis) || player.hasPermission(Permissions.Bypass.RESTOCK_COOLDOWN.get())) {
            wVillager.restock();
            wVillager.saveRestockTime();
            if (notifyPlayer) {
                final String timeLeft = CommonUtils.formatTime(wVillager.getRestockCooldownMillis(restock_delay_millis));
                VillagerOptimizer.getLang(player.locale()).trades_restocked.forEach(line -> player.sendMessage(line
                        .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build()))
                );
            }
            if (shouldLog)
                VillagerOptimizer.getLog().info("Restocked optimized villager at "+ wVillager.villager().getLocation());
        }
    }
}
