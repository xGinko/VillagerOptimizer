package me.xginko.villageroptimizer.modules;

import java.util.HashSet;

public interface VillagerOptimizerModule {

    void enable();
    void disable();
    boolean shouldEnable();

    HashSet<VillagerOptimizerModule> modules = new HashSet<>();

    static void reloadModules() {
        modules.forEach(VillagerOptimizerModule::disable);
        modules.clear();

        modules.add(new VillagerChunkLimit());
        modules.add(new NametagOptimization());
        modules.add(new BlockOptimization());
        modules.add(new WorkstationOptimization());
        modules.add(new PreventVillagerDamage());
        modules.add(new PreventVillagerTargetting());
        modules.add(new RestockTrades());
        modules.add(new LevelVillagers());

        for (VillagerOptimizerModule module : modules) {
            if (module.shouldEnable()) module.enable();
        }
    }
}
