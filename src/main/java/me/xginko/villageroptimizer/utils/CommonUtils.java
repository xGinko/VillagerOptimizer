package me.xginko.villageroptimizer.utils;

import java.time.Duration;

public class CommonUtils {

    public static String formatTime(long millis) {
        Duration duration = Duration.ofMillis(millis);
        final int seconds = duration.toSecondsPart();
        final int minutes = duration.toMinutesPart();
        final int hours = duration.toHoursPart();

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else {
            return minutes > 0 ? String.format("%02dm %02ds", minutes, seconds) : String.format("%02ds", seconds);
        }
    }

}
