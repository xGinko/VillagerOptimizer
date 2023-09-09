package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;

public class PreventUnoptimizedTrading implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final boolean notifyPlayer;

    protected PreventUnoptimizedTrading() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization.prevent-trading-with-unoptimized-villagers.enable", """
                Will prevent players from selecting and using trades of unoptimized villagers. s\
                Use this if you have a lot of villagers and therefore want to force your players to optimize them. s\
                Inventories can still be opened so players can move villagers around.
                """);
        this.notifyPlayer = config.getBoolean("optimization.prevent-trading-with-unoptimized-villagers.notify-player", true);
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.prevent-trading-with-unoptimized-villagers.enable", false);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onTradeOpen(TradeSelectEvent event) {
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager villager
                && !villagerManager.getOrAdd(villager).isOptimized()
        ) {
            event.setCancelled(true);
            if (!notifyPlayer) return;
            Player player = (Player) event.getWhoClicked();
            VillagerOptimizer.getLang(player.locale()).optimize_for_trading.forEach(player::sendMessage);
        }
    }
}
