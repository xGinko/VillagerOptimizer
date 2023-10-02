package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.extras.PreventUnoptimizedTrading;
import me.xginko.villageroptimizer.modules.extras.PreventVillagerDamage;
import me.xginko.villageroptimizer.modules.extras.PreventVillagerTargetting;
import me.xginko.villageroptimizer.modules.extras.MakeVillagersSpawnAdult;
import me.xginko.villageroptimizer.modules.mechanics.LevelVillagers;
import me.xginko.villageroptimizer.modules.mechanics.RestockTrades;
import me.xginko.villageroptimizer.modules.optimizations.OptimizeByBlock;
import me.xginko.villageroptimizer.modules.optimizations.OptimizeByNametag;
import me.xginko.villageroptimizer.modules.optimizations.OptimizeByWorkstation;
import org.bukkit.event.HandlerList;

import java.util.HashSet;

public interface VillagerOptimizerModule {

    void enable();
    boolean shouldEnable();

    HashSet<VillagerOptimizerModule> modules = new HashSet<>();

    static void reloadModules() {
        VillagerOptimizer plugin = VillagerOptimizer.getInstance();
        HandlerList.unregisterAll(plugin);
        plugin.getServer().getScheduler().cancelTasks(plugin);
        modules.clear();

        modules.add(new OptimizeByNametag());
        modules.add(new OptimizeByBlock());
        modules.add(new OptimizeByWorkstation());

        modules.add(new RestockTrades());
        modules.add(new LevelVillagers());

        modules.add(new MakeVillagersSpawnAdult());
        modules.add(new PreventUnoptimizedTrading());
        modules.add(new PreventVillagerDamage());
        modules.add(new PreventVillagerTargetting());

        modules.add(new VillagerChunkLimit());
        modules.add(new RenameOptimized());

        modules.forEach(module -> {
            if (module.shouldEnable()) module.enable();
        });
    }
}
