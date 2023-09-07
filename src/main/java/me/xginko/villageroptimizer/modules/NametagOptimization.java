package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.VillagerCache;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class NametagOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerCache cache;
    private final Config config;
    private final boolean shouldLog, shouldNotifyPlayer;

    protected NametagOptimization() {
        this.cache = VillagerOptimizer.getVillagerCache();
        this.config = VillagerOptimizer.getConfiguration();
        this.config.addComment("optimization.methods.by-nametag.enable",
                """
                Enable optimization by naming villagers to one of the names configured below.\s
                Nametag optimized villagers will be unoptimized again when they are renamed to something else.
                """
        );
        this.shouldLog = config.getBoolean("optimization.methods.by-nametag.log", false);
        this.shouldNotifyPlayer = config.getBoolean("optimization.methods.by-nametag.notify-player", true);
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
        return config.enable_nametag_optimization;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPlayerNameEntity(PlayerNameEntityEvent event) {
        if (!event.getEntity().getType().equals(EntityType.VILLAGER)) return;
        Component name = event.getName();
        if (name == null) return;

        final String nameTag = PlainTextComponentSerializer.plainText().serialize(name);
        WrappedVillager wVillager = cache.getOrAdd((Villager) event.getEntity());

        if (config.nametags.contains(nameTag.toLowerCase())) {
            if (!wVillager.isOptimized()) {
                if (wVillager.setOptimization(OptimizationType.NAMETAG)) {
                    if (shouldNotifyPlayer) {
                        Player player = event.getPlayer();
                        VillagerOptimizer.getLang(player.locale()).nametag_optimize_success.forEach(player::sendMessage);
                    }
                    if (shouldLog)
                        VillagerOptimizer.getLog().info(event.getPlayer().getName() + " optimized a villager using nametag: '" + nameTag + "'");
                } else {
                    if (shouldNotifyPlayer) {
                        Player player = event.getPlayer();
                        VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                                .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(CommonUtils.formatTime(wVillager.getOptimizeCooldown())).build())));
                    }
                }
            }
        } else {
            if (wVillager.getOptimizationType().equals(OptimizationType.NAMETAG)) {
                wVillager.setOptimization(OptimizationType.OFF);
                if (shouldNotifyPlayer) {
                    Player player = event.getPlayer();
                    VillagerOptimizer.getLang(player.locale()).nametag_unoptimize_success.forEach(player::sendMessage);
                }
                if (shouldLog)
                    VillagerOptimizer.getLog().info(event.getPlayer().getName() + " disabled optimizations for a villager using nametag: '" + nameTag + "'");
            }
        }
    }
}
