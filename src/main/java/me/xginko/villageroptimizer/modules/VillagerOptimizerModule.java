package me.xginko.villageroptimizer.modules;

import me.xginko.villageroptimizer.VillagerOptimizer;
import me.xginko.villageroptimizer.modules.gameplay.*;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByBlock;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByNametag;
import me.xginko.villageroptimizer.modules.optimization.OptimizeByWorkstation;
import me.xginko.villageroptimizer.utils.Util;
import net.kyori.adventure.text.Component;

import java.util.HashSet;

public interface VillagerOptimizerModule {

    String configPath();
    void enable();
    void disable();
    boolean shouldEnable();

    HashSet<VillagerOptimizerModule> MODULES = new HashSet<>(14);

    static void reloadModules() {
        MODULES.forEach(VillagerOptimizerModule::disable);
        MODULES.clear();

        MODULES.add(new OptimizeByNametag());
        MODULES.add(new OptimizeByBlock());
        MODULES.add(new OptimizeByWorkstation());

        MODULES.add(new EnableLeashingVillagers());
        MODULES.add(new FixOptimisationAfterCure());
        MODULES.add(new RestockOptimizedTrades());
        MODULES.add(new LevelOptimizedProfession());
        MODULES.add(new VisuallyHighlightOptimized());
        MODULES.add(new MakeVillagersSpawnAdult());
        MODULES.add(new PreventUnoptimizedTrading());
        MODULES.add(new PreventOptimizedTargeting());
        MODULES.add(new PreventOptimizedDamage());
        MODULES.add(new UnoptimizeOnJobLoose());

        MODULES.add(new VillagerChunkLimit());

        MODULES.forEach(module -> {
            if (module.shouldEnable()) module.enable();
        });
    }

    default void trace(String prefix, String message, Throwable t) {
        VillagerOptimizer.getPrefixedLogger().trace("<{}> {}", prefix, message, t);
    }

    default void error(String message, Throwable t) {
        VillagerOptimizer.getPrefixedLogger().error("<{}> {}", logPrefix(), message, t);
    }

    default void error(String message) {
        VillagerOptimizer.getPrefixedLogger().error("<{}> {}", logPrefix(), message);
    }

    default void warn(String message) {
        VillagerOptimizer.getPrefixedLogger().warn("<{}> {}", logPrefix(), message);
    }

    default void info(String message) {
        VillagerOptimizer.getPrefixedLogger().info(Component.text("<" + logPrefix() + "> " + message).color(Util.PL_COLOR));
    }

    default String logPrefix() {
        String[] split = configPath().split("\\.");
        if (split.length <= 2) return configPath();
        return split[split.length - 2] + "." + split[split.length - 1];
    }
}
