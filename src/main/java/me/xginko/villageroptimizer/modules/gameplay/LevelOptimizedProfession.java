package me.xginko.villageroptimizer.modules.gameplay;

import com.tcoded.folialib.impl.ServerImplementation;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.VillagerCache;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.WrappedVillager;
import me.xginko.villageroptimizer.modules.VillagerOptimizerModule;
import me.xginko.villageroptimizer.utils.GenericUtil;
import me.xginko.villageroptimizer.utils.KyoriUtil;
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class LevelOptimizedProfession implements VillagerOptimizerModule, Listener {

    private final ServerImplementation scheduler;
    private final VillagerCache villagerCache;
    private final boolean notify_player;
    private final long cooldown_millis;

    public LevelOptimizedProfession() {
        shouldEnable();
        this.scheduler = VillagerOptimizer.getFoliaLib().getImpl();
        this.villagerCache = VillagerOptimizer.getCache();
        Config config = VillagerOptimizer.getConfiguration();
        config.master().addComment("gameplay.level-optimized-profession",
                "This is needed to allow optimized villagers to level up.\n" +
                "Temporarily enables the villagers AI to allow it to level up and then disables it again.");
        this.cooldown_millis = TimeUnit.SECONDS.toMillis(
                config.getInt("gameplay.level-optimized-profession.level-check-cooldown-seconds", 5,
                "Cooldown in seconds until the level of a villager will be checked and updated again.\n" +
                "Recommended to leave as is."));
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onTradeScreenClose(InventoryCloseEvent event) {
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager
        ) {
            final Villager villager = (Villager) event.getInventory().getHolder();
            final WrappedVillager wVillager = villagerCache.getOrAdd(villager);
            if (!wVillager.isOptimized()) return;

            if (wVillager.canLevelUp(cooldown_millis)) {
                if (wVillager.calculateLevel() <= villager.getVillagerLevel()) return;

                scheduler.runAtEntity(villager, enableAI -> {
                    villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 120, 120, false, false));
                    villager.setAware(true);
                    scheduler.runAtEntityLater(villager, disableAI -> {
                        villager.setAware(false);
                        wVillager.saveLastLevelUp();
                    }, 5, TimeUnit.SECONDS);
                });
            } else {
                if (notify_player) {
                    Player player = (Player) event.getPlayer();
                    final TextReplacementConfig timeLeft = TextReplacementConfig.builder()
                            .matchLiteral("%time%")
                            .replacement(GenericUtil.formatDuration(Duration.ofMillis(wVillager.getLevelCooldownMillis(cooldown_millis))))
                            .build();
                    VillagerOptimizer.getLang(player.locale()).villager_leveling_up
                            .forEach(line -> KyoriUtil.sendMessage(player, line.replaceText(timeLeft)));
                }
            }
        }
    }
}
