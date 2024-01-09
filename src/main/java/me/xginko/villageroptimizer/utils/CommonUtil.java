package me.xginko.villageroptimizer.utils;

import org.bukkit.Chunk;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class CommonUtil {
    public static @NotNull String formatTime(final long millis) {
        Duration duration = Duration.ofMillis(millis);
        final int seconds = duration.toSecondsPart();
        final int minutes = duration.toMinutesPart();
        final int hours = duration.toHoursPart();

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }

    private static boolean specificChunkLoadedMethodAvailable = true;
    public static boolean isEntitiesLoaded(@NotNull Chunk chunk) {
        if (!specificChunkLoadedMethodAvailable) {
            return chunk.isLoaded();
        }
        try {
            return chunk.isEntitiesLoaded();
        } catch (NoSuchMethodError e) {
            specificChunkLoadedMethodAvailable = false;
            return chunk.isLoaded();
        }
    }

    public static void shakeHead(@NotNull Villager villager) {
        try {
            villager.shakeHead();
        } catch (NoSuchMethodError ignored) {}
    }
}
