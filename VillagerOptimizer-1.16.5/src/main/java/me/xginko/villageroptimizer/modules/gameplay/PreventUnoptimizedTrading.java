package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.Permissions;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;

public class PreventUnoptimizedTrading implements VillagerOptimizerModule, Listener {

    private final VillagerCache villagerCache;
    private final boolean notify_player;

    public PreventUnoptimizedTrading() {
        shouldEnable();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("gameplay.prevent-trading-with-unoptimized.enable", """
                Will prevent players from selecting and using trades of unoptimized villagers.\s
                Use this if you have a lot of villagers and therefore want to force your players to optimize them.\s
                Inventories can still be opened so players can move villagers around.""");
        this.notify_player = config.getBoolean("gameplay.prevent-trading-with-unoptimized.notify-player", true,
                "Sends players a message when they try to trade with an unoptimized villager.");
    }

    @Override
    public void enable() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean shouldEnable() {
        return VillagerOptimizer.getConfiguration().getBoolean("gameplay.prevent-trading-with-unoptimized.enable", false);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onTradeOpen(TradeSelectEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.hasPermission(Permissions.Bypass.TRADE_PREVENTION.get())) return;
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager villager
                && !villagerCache.getOrAdd(villager).isOptimized()
        ) {
            event.setCancelled(true);
            if (notify_player)
                VillagerOptimizer.getLang(player.locale()).optimize_for_trading.forEach(player::sendMessage);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (player.hasPermission(Permissions.Bypass.TRADE_PREVENTION.get())) return;
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager villager
                && !villagerCache.getOrAdd(villager).isOptimized()
        ) {
            event.setCancelled(true);
            if (notify_player)
                VillagerOptimizer.getLang(player.locale()).optimize_for_trading.forEach(player::sendMessage);
        }
    }
}
