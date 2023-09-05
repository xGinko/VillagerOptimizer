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

        modules.add(new AntiVillagerDamage());
        modules.add(new AntiVillagerTargetting());

        modules.add(new NametagOptimization());
        modules.add(new BlockOptimization());
        modules.add(new WorkstationOptimization());

        modules.add(new ChunkLimit());

        for (VillagerOptimizerModule module : modules) {
            if (module.shouldEnable()) module.enable();
        }
    }
}
