package me.xginko.villageroptimizer.utils;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

import static java.lang.String.format;

public class CommonUtil {

    public static @NotNull String formatTime(final long millis) {
        Duration duration = Duration.ofMillis(millis);
        final int seconds = duration.toSecondsPart();
        final int minutes = duration.toMinutesPart();
        final int hours = duration.toHoursPart();

        if (hours > 0) {
            return format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return format("%02dm %02ds", minutes, seconds);
        } else {
            return format("%02ds", seconds);
        }
    }

}
