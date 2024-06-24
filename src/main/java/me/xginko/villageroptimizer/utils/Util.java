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
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class Util {

    public static final @NotNull TextColor PL_COLOR;
    public static final @NotNull Style PL_STYLE;
    private static final @NotNull Map<Material, Villager.Profession> PROFESSION_MAP;
    private static boolean canUseIsEntitiesLoaded;

    static {
        PL_COLOR = TextColor.color(102,255,230);
        PL_STYLE = Style.style(PL_COLOR, TextDecoration.BOLD);
        PROFESSION_MAP = new EnumMap<>(Material.class);
        PROFESSION_MAP.put(Material.LOOM,               Villager.Profession.SHEPHERD);
        PROFESSION_MAP.put(Material.BARREL,             Villager.Profession.FISHERMAN);
        PROFESSION_MAP.put(Material.SMOKER,             Villager.Profession.BUTCHER);
        PROFESSION_MAP.put(Material.LECTERN,            Villager.Profession.LIBRARIAN);
        PROFESSION_MAP.put(Material.CAULDRON,           Villager.Profession.LEATHERWORKER);
        PROFESSION_MAP.put(Material.COMPOSTER,          Villager.Profession.FARMER);
        PROFESSION_MAP.put(Material.GRINDSTONE,         Villager.Profession.WEAPONSMITH);
        PROFESSION_MAP.put(Material.STONECUTTER,        Villager.Profession.MASON);
        PROFESSION_MAP.put(Material.BREWING_STAND,      Villager.Profession.CLERIC);
        PROFESSION_MAP.put(Material.BLAST_FURNACE,      Villager.Profession.ARMORER);
        PROFESSION_MAP.put(Material.SMITHING_TABLE,     Villager.Profession.TOOLSMITH);
        PROFESSION_MAP.put(Material.FLETCHING_TABLE,    Villager.Profession.FLETCHER);
        PROFESSION_MAP.put(Material.CARTOGRAPHY_TABLE,  Villager.Profession.CARTOGRAPHER);
        try {
            Chunk.class.getMethod("isEntitiesLoaded");
            canUseIsEntitiesLoaded = true;
        } catch (NoSuchMethodException e) {
            canUseIsEntitiesLoaded = false;
        }
    }

    public static @Nullable Villager.Profession getWorkstationProfession(@NotNull Material workstation) {
        return PROFESSION_MAP.getOrDefault(workstation, null);
    }

    public static boolean isChunkLoaded(@NotNull Chunk chunk) {
        return canUseIsEntitiesLoaded ? chunk.isEntitiesLoaded() : chunk.isLoaded();
    }

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
        String[] lowercaseWords = input.name().toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i < lowercaseWords.length; i++) {
            String word = lowercaseWords[i];
            // Capitalize first letter for each word
            lowercaseWords[i] = word.substring(0, 1).toUpperCase() + word.substring(1);
        }
        // return as nice string
        return String.join(" ", lowercaseWords);
    }
}
