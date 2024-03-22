package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.gameplay.*;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByBlock;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByNametag;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByWorkstation;

import java.util.HashSet;

public interface VillagerOptimizerModule {

    String configPath();
    void enable();
    void disable();
    boolean shouldEnable();

    HashSet<VillagerOptimizerModule> modules = new HashSet<>();

    static void reloadModules() {
        modules.forEach(VillagerOptimizerModule::disable);
        modules.clear();

        modules.add(new OptimizeByNametag());
        modules.add(new OptimizeByBlock());
        modules.add(new OptimizeByWorkstation());

        modules.add(new EnableLeashingVillagers());
        modules.add(new FixOptimisationAfterCure());
        modules.add(new RestockOptimizedTrades());
        modules.add(new LevelOptimizedProfession());
        modules.add(new VisuallyHighlightOptimized());
        modules.add(new MakeVillagersSpawnAdult());
        modules.add(new PreventUnoptimizedTrading());
        modules.add(new PreventOptimizedTargeting());
        modules.add(new PreventOptimizedDamage());
        modules.add(new UnoptimizeOnJobLoose());

        modules.add(new VillagerChunkLimit());

        modules.forEach(module -> {
            if (module.shouldEnable()) module.enable();
        });
    }

    default void trace(String message, Throwable throwable) {
        VillagerOptimizer.getPrefixedLogger().trace(logPrefix() + message, throwable);
    }

    default void error(String message) {
        VillagerOptimizer.getPrefixedLogger().error(logPrefix() + message);
    }

    default void warn(String message) {
        VillagerOptimizer.getPrefixedLogger().warn(logPrefix() + message);
    }

    default void info(String message) {
        VillagerOptimizer.getPrefixedLogger().info(logPrefix() + message);
    }

    default String logPrefix() {
        String[] split = configPath().split("\\.");
        if (split.length <= 2) return "<" + configPath() + "> ";
        return "<" + String.join(".", split[split.length - 2], split[split.length - 1]) + "> ";
    }
}
