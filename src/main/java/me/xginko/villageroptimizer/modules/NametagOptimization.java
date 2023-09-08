package me.xginko.villageroptimizer.modules;

import io.papermc.paper.event.player.PlayerNameEntityEvent;
import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.cache.VillagerManager;
import me.xginko.villageroptimizer.config.Config;
import me.xginko.villageroptimizer.enums.OptimizationType;
import me.xginko.villageroptimizer.models.WrappedVillager;
import me.xginko.villageroptimizer.utils.CommonUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class NametagOptimization implements VillagerOptimizerModule, Listener {

    private final VillagerManager villagerManager;
    private final Config config;
    private final boolean shouldLog, shouldNotifyPlayer, consumeNametag;
    private final long cooldown;

    protected NametagOptimization() {
        this.villagerManager = VillagerOptimizer.getVillagerManager();
        this.config = VillagerOptimizer.getConfiguration();
        this.config.addComment("optimization.methods.by-nametag.enable", """
                Enable optimization by naming villagers to one of the names configured below.\s
                Nametag optimized villagers will be unoptimized again when they are renamed to something else.
                """);
        this.consumeNametag = config.getBoolean("optimization.methods.by-nametag.nametags-get-consumed", true,
                "Enable or disable consumption of the used nametag item.");
        this.cooldown = config.getInt("optimization.methods.by-workstation.optimize-cooldown-seconds", 600, """
                Cooldown in seconds until a villager can be optimized again using a nametag. \s
                Here for configuration freedom. Recommended to leave as is to not enable any exploitable behavior.
                """) * 1000L;
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
        WrappedVillager wVillager = villagerManager.getOrAdd((Villager) event.getEntity());
        Player player = event.getPlayer();

        if (config.nametags.contains(nameTag.toLowerCase())) {
            if (wVillager.isOptimized()) return;
            if (wVillager.canOptimize(cooldown)) {
                wVillager.setOptimization(OptimizationType.NAMETAG);
                wVillager.saveOptimizeTime();
                if (!consumeNametag) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemStack offHand = player.getInventory().getItemInOffHand();
                    if (mainHand.getType().equals(Material.NAME_TAG)) mainHand.add();
                    else if (offHand.getType().equals(Material.NAME_TAG)) offHand.add();
                }
                if (shouldNotifyPlayer)
                    VillagerOptimizer.getLang(player.locale()).nametag_optimize_success.forEach(player::sendMessage);
                if (shouldLog)
                    VillagerOptimizer.getLog().info(player.getName() + " optimized a villager using nametag: '" + nameTag + "'");
            } else {
                event.setCancelled(true);
                if (shouldNotifyPlayer) {
                    final long optimizeCoolDown = wVillager.getOptimizeCooldownMillis(cooldown);
                    VillagerOptimizer.getLang(player.locale()).nametag_on_optimize_cooldown.forEach(line -> player.sendMessage(line
                            .replaceText(TextReplacementConfig.builder().matchLiteral("%time%").replacement(CommonUtils.formatTime(optimizeCoolDown)).build())));
                }
            }
        } else {
            if (wVillager.getOptimizationType().equals(OptimizationType.NAMETAG)) {
                wVillager.setOptimization(OptimizationType.OFF);
                if (shouldNotifyPlayer)
                    VillagerOptimizer.getLang(player.locale()).nametag_unoptimize_success.forEach(player::sendMessage);
                if (shouldLog)
                    VillagerOptimizer.getLog().info(event.getPlayer().getName() + " disabled optimizations for a villager using nametag: '" + nameTag + "'");
            }
        }
    }
}
