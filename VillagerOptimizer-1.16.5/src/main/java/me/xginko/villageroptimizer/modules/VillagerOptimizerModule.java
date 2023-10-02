package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.gameplay.*;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByBlock;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByNametag;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByWorkstation;
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

        modules.add(new RestockOptimizedTrades());
        modules.add(new LevelOptimizedVillagers());
        modules.add(new RenameOptimizedVillagers());
        modules.add(new MakeVillagersSpawnAdult());
        modules.add(new PreventUnoptimizedTrading());
        modules.add(new PreventVillagerTargetting());
        modules.add(new PreventVillagerDamage());

        modules.add(new VillagerChunkLimit());

        modules.forEach(module -> {
            if (module.shouldEnable()) module.enable();
        });
    }
}
