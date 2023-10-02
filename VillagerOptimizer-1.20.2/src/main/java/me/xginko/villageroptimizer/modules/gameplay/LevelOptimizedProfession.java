package me.xginko.villageroptimizer.modules.gameplay;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.CommonUtil;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LevelOptimizedProfession implements VillagerOptimizerModule, Listener {

    private final VillagerOptimizer plugin;
    private final VillagerCache villagerCache;
    private final boolean notify_player;
    private final long cooldown;

    public LevelOptimizedProfession() {
        shouldEnable();
        this.plugin = VillagerOptimizer.getInstance();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("gameplay.level-optimized-profession", """
                This is needed to allow optimized villagers to level up.\s
                Temporarily enables the villagers AI to allow it to level up and then disables it again.""");
        this.cooldown = config.getInt("gameplay.level-optimized-profession.level-check-cooldown-seconds", 5, """
                Cooldown in seconds until the level of a villager will be checked and updated again.\s
                Recommended to leave as is.""") * 1000L;
        this.notify_player = config.getBoolean("gameplay.level-optimized-profession.notify-player", true,
                "Tell players to wait when a villager is leveling up.");
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onTradeScreenClose(InventoryCloseEvent event) {
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager villager
        ) {
            WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            if (!wVillager.isOptimized()) return;

            if (wVillager.canLevelUp(cooldown)) {
                if (wVillager.calculateLevel() > villager.getVillagerLevel()) {
                    villager.getScheduler().run(plugin, enableAI -> {
                        villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 120, 120, false, false));
                        villager.setAware(true);
                    }, null);
                    villager.getScheduler().runDelayed(plugin, disableAI -> {
                        villager.setAware(false);
                        wVillager.saveLastLevelUp();
                    }, null, 100L);
                }
            } else {
                if (notify_player) {
                    Player player = (Player) event.getPlayer();
                    final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                            .matchLiteral("%time%")
                            .replacement(CommonUtil.formatTime(wVillager.getLevelCooldownMillis(cooldown)))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).villager_leveling_up.forEach(line -> player.sendMessage(line.replaceText(timeLeft)));
                }
            }
        }
    }
}
