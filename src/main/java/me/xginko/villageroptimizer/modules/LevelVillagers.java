package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LevelVillagers implements VillagerOptimizerModule, Listener {

    private final VillagerOptimizer plugin;
    private final VillagerManager villagerManager;
    private final long cooldown;

    public LevelVillagers() {
        this.plugin = VillagerOptimizer.getInstance();
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        Config config = VillagerOptimizer.getConfiguration();
        config.addComment("optimization.villager-leveling.enable", """
                This is needed to allow optimized villagers to level up. s\
                Temporarily enables the villagers AI to allow it to level up and then disables it again.
                """);
        this.cooldown = config.getInt("optimization.villager-leveling.level-check-cooldown-seconds", 5, """
                Cooldown in seconds the level of a villager will be checked and updated again. \s
                Recommended to leave as is.
                """) * 1000L;
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
        return VillagerOptimizer.getConfiguration().getBoolean("optimization.villager-leveling.enable", true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onTradeScreenClose(InventoryCloseEvent event) {
        if (
                event.getInventory().getType().equals(InventoryType.MERCHANT)
                && event.getInventory().getHolder() instanceof Villager villager
        ) {
            WrappedVillager wVillager = villagerManager.getOrAdd(villager);
            if (
                    wVillager.isOptimized()
                    && wVillager.canLevelUp(cooldown)
                    && wVillager.calculateLevel() > villager.getVillagerLevel()
            ) {
                villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (20 + (cooldown / 50L)), 120, false, false));
                final OptimizationType previousOptimization = wVillager.getOptimizationType();
                wVillager.setOptimization(OptimizationType.OFF);
                villager.getScheduler().runDelayed(plugin, reOptimize -> {
                    wVillager.setOptimization(previousOptimization);
                    wVillager.saveLastLevelUp();
                }, null, 100L);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().getType().equals(EntityType.VILLAGER)) return;
        WrappedVillager wVillager = villagerManager.getOrAdd((Villager) event.getRightClicked());
        if (wVillager.isOptimized() && !wVillager.canLevelUp(cooldown)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String timeLeft = CommonUtils.formatTime(wVillager.getLevelCooldownMillis(cooldown));
            VillagerOptimizer.getLang(player.locale()).villager_leveling_up.forEach(line -> player.sendMessage(line
                    .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(timeLeft).build())
            ));
        }
    }
}
