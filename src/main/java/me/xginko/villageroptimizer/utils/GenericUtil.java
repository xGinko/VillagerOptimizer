package me.xginko.villageroptimizer.utils;

import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Locale;

public class GenericUtil {

    public static final @NotNull TextColor COLOR = TextColor.color(102,255,230);
    public static final @NotNull Style STYLE = Style.style(COLOR, TextDecoration.BOLD);

    public static @NotNull String formatDuration(@NotNull Duration duration) {
        if (duration.isNegative()) duration = duration.negated();

        final int seconds = (int) (duration.getSeconds() % 60);
        final int minutes = (int) (duration.toMinutes() % 60);
        final int hours = (int) (duration.toHours() % 24);

        if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02dm %02ds", minutes, seconds);
        } else {
            return String.format("%02ds", seconds);
        }
    }

    public static @NotNull String formatEnum(@NotNull Enum<?> input) {
        // Turn something like "REDSTONE_TORCH" into "redstone torch"
        String[] lowercaseWords = input.name().toLowerCase(Locale.ROOT).split("-");
        for (int i = 0; i < lowercaseWords.length; i++) {
            String word = lowercaseWords[i];
            // Capitalize first letter for each word
            lowercaseWords[i] = word.substring(0, 1).toUpperCase() + word.substring(1);
        }
        // return as nice string
        return String.join(" ", lowercaseWords);
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

    public static @Nullable Villager.Profession getWorkstationProfession(@NotNull Material workstation) {
        switch (workstation) {
            case BARREL:
                return Villager.Profession.FISHERMAN;
            case CARTOGRAPHY_TABLE:
                return Villager.Profession.CARTOGRAPHER;
            case SMOKER:
                return Villager.Profession.BUTCHER;
            case SMITHING_TABLE:
                return Villager.Profession.TOOLSMITH;
            case GRINDSTONE:
                return Villager.Profession.WEAPONSMITH;
            case BLAST_FURNACE:
                return Villager.Profession.ARMORER;
            case CAULDRON:
                return Villager.Profession.LEATHERWORKER;
            case BREWING_STAND:
                return Villager.Profession.CLERIC;
            case COMPOSTER:
                return Villager.Profession.FARMER;
            case FLETCHING_TABLE:
                return Villager.Profession.FLETCHER;
            case LOOM:
                return Villager.Profession.SHEPHERD;
            case LECTERN:
                return Villager.Profession.LIBRARIAN;
            case STONECUTTER:
                return Villager.Profession.MASON;
            default:
                return null;
        }
    }
}
