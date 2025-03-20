package com.blissy.customConsumables.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.ModList;
import net.minecraft.nbt.CompoundNBT;

/**
 * This class handles Pixelmon API compatibility
 * without directly depending on Pixelmon classes during compile time.
 */
public class PixelmonCompat {

    private static final boolean PIXELMON_LOADED = ModList.get().isLoaded("pixelmon");

    // Check if Pixelmon is loaded
    public static boolean isPixelmonLoaded() {
        return PIXELMON_LOADED;
    }

    // Safely apply legendary boost through reflection
    public static void applyLegendaryBoost(PlayerEntity player, int durationTicks) {
        if (!PIXELMON_LOADED) return;

        // Store effect in player NBT data for Pixelmon to read
        player.getPersistentData().getCompound("CustomConsumables")
                .putInt("legendaryBoostTicks", durationTicks);
    }

    // Safely apply shiny boost through reflection
    public static void applyShinyBoost(PlayerEntity player, int durationTicks) {
        if (!PIXELMON_LOADED) return;

        // Store effect in player NBT data for Pixelmon to read
        player.getPersistentData().getCompound("CustomConsumables")
                .putInt("shinyBoostTicks", durationTicks);
    }

    // Safely apply hidden ability boost through reflection
    public static void applyHiddenAbilityBoost(PlayerEntity player, int durationTicks) {
        if (!PIXELMON_LOADED) return;

        // Store effect in player NBT data for Pixelmon to read
        player.getPersistentData().getCompound("CustomConsumables")
                .putInt("hiddenAbilityBoostTicks", durationTicks);
    }

    // Safely apply type boost through reflection
    public static void applyTypeBoost(PlayerEntity player, String type, int durationTicks) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT tag = player.getPersistentData().getCompound("CustomConsumables");
        tag.putInt("typeBoostTicks", durationTicks);
        tag.putString("boostedType", type);
        player.getPersistentData().put("CustomConsumables", tag);
    }
}