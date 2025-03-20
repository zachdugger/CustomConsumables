package com.blissy.customConsumables.compat;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fml.ModList;

/**
 * Improved Pixelmon integration class that handles all interactions with the Pixelmon mod.
 * This uses NBT data to pass information between mods without direct dependencies.
 */
public class PixelmonIntegration {
    private static final boolean PIXELMON_LOADED = ModList.get().isLoaded("pixelmon");

    // NBT keys for storing effect data
    private static final String MOD_TAG = "CustomConsumables";
    private static final String LEGENDARY_BOOST_KEY = "legendaryBoost";
    private static final String SHINY_BOOST_KEY = "shinyBoost";
    private static final String HIDDEN_ABILITY_KEY = "hiddenAbilityBoost";
    private static final String TYPE_BOOST_KEY = "typeBoost";
    private static final String TYPE_VALUE_KEY = "boostedType";
    private static final String TYPE_MULTIPLIER_KEY = "typeMultiplier";

    // Key for storing chance values
    private static final String CHANCE_KEY = "chance";
    private static final String DURATION_KEY = "duration";
    private static final String OVERRIDE_KEY = "override"; // For shiny, indicates absolute replacement

    /**
     * Check if Pixelmon is loaded
     */
    public static boolean isPixelmonLoaded() {
        return PIXELMON_LOADED;
    }

    /**
     * Apply a legendary boost effect to a player
     */
    public static void applyLegendaryBoost(PlayerEntity player, int durationTicks, float chance) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getOrCreateModTag(player);
        CompoundNBT effectData = new CompoundNBT();

        effectData.putInt(DURATION_KEY, durationTicks);
        effectData.putFloat(CHANCE_KEY, chance);

        playerData.put(LEGENDARY_BOOST_KEY, effectData);
        saveModTag(player, playerData);

        CustomConsumables.getLogger().info("Applied legendary boost to {} with {}% chance for {} ticks",
                player.getName().getString(), chance, durationTicks);
    }

    /**
     * Apply a shiny boost effect to a player
     * This will override the normal shiny chance with the specified absolute chance
     */
    public static void applyShinyBoost(PlayerEntity player, int durationTicks, float absoluteChance) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getOrCreateModTag(player);
        CompoundNBT effectData = new CompoundNBT();

        effectData.putInt(DURATION_KEY, durationTicks);
        effectData.putFloat(CHANCE_KEY, absoluteChance);
        effectData.putBoolean(OVERRIDE_KEY, true); // Mark as absolute override

        playerData.put(SHINY_BOOST_KEY, effectData);
        saveModTag(player, playerData);

        CustomConsumables.getLogger().info("Applied shiny boost to {} with absolute {}% chance for {} ticks",
                player.getName().getString(), absoluteChance, durationTicks);
    }

    /**
     * Apply a hidden ability boost effect to a player
     */
    public static void applyHiddenAbilityBoost(PlayerEntity player, int durationTicks, float chance) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getOrCreateModTag(player);
        CompoundNBT effectData = new CompoundNBT();

        effectData.putInt(DURATION_KEY, durationTicks);
        effectData.putFloat(CHANCE_KEY, chance);

        playerData.put(HIDDEN_ABILITY_KEY, effectData);
        saveModTag(player, playerData);

        CustomConsumables.getLogger().info("Applied hidden ability boost to {} with {}% chance for {} ticks",
                player.getName().getString(), chance, durationTicks);
    }

    /**
     * Apply a type boost effect to a player
     * This increases spawn rates for a specific type by the given multiplier
     */
    public static void applyTypeBoost(PlayerEntity player, String type, int durationTicks, float multiplier) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getOrCreateModTag(player);
        CompoundNBT effectData = new CompoundNBT();

        effectData.putInt(DURATION_KEY, durationTicks);
        effectData.putFloat(TYPE_MULTIPLIER_KEY, multiplier);
        effectData.putString(TYPE_VALUE_KEY, type.toLowerCase());

        playerData.put(TYPE_BOOST_KEY, effectData);
        saveModTag(player, playerData);

        CustomConsumables.getLogger().info("Applied {} type boost to {} with {}x multiplier for {} ticks",
                type, player.getName().getString(), multiplier, durationTicks);
    }

    /**
     * Check if a player has an active legendary boost
     */
    public static boolean hasLegendaryBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED) return false;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(LEGENDARY_BOOST_KEY);
            return effectData.getInt(DURATION_KEY) > 0;
        }

        return false;
    }

    /**
     * Get the legendary spawn chance for a player
     */
    public static float getLegendaryChance(PlayerEntity player, float defaultChance) {
        if (!PIXELMON_LOADED) return defaultChance;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(LEGENDARY_BOOST_KEY);
            return effectData.getFloat(CHANCE_KEY);
        }

        return defaultChance;
    }

    /**
     * Check if a player has an active shiny boost
     */
    public static boolean hasShinyBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED) return false;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(SHINY_BOOST_KEY);
            return effectData.getInt(DURATION_KEY) > 0;
        }

        return false;
    }

    /**
     * Get the shiny spawn chance for a player
     * Returns the absolute chance (like 50%) if override is true
     */
    public static float getShinyChance(PlayerEntity player, float defaultChance) {
        if (!PIXELMON_LOADED) return defaultChance;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(SHINY_BOOST_KEY);
            if (effectData.getBoolean(OVERRIDE_KEY)) {
                // This is an absolute chance override (like 50%)
                return effectData.getFloat(CHANCE_KEY);
            } else {
                // This is a multiplier (original behavior)
                return defaultChance * effectData.getFloat(CHANCE_KEY);
            }
        }

        return defaultChance;
    }

    /**
     * Check if player has a type boost active
     */
    public static boolean hasTypeBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED) return false;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(TYPE_BOOST_KEY);
            return effectData.getInt(DURATION_KEY) > 0;
        }

        return false;
    }

    /**
     * Get the hidden ability chance for a player
     */
    public static float getHiddenAbilityChance(PlayerEntity player, float defaultChance) {
        if (!PIXELMON_LOADED) return defaultChance;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(HIDDEN_ABILITY_KEY)) {
            CompoundNBT effectData = playerData.getCompound(HIDDEN_ABILITY_KEY);
            return effectData.getFloat(CHANCE_KEY);
        }

        return defaultChance;
    }

    /**
     * Get the type boost multiplier for a player
     */
    public static float getTypeBoostMultiplier(PlayerEntity player, float defaultMultiplier) {
        if (!PIXELMON_LOADED) return defaultMultiplier;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(TYPE_BOOST_KEY);
            return effectData.getFloat(TYPE_MULTIPLIER_KEY);
        }

        return defaultMultiplier;
    }

    /**
     * Get the boosted type for a player
     */
    public static String getBoostedType(PlayerEntity player) {
        if (!PIXELMON_LOADED) return "";

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(TYPE_BOOST_KEY);
            return effectData.getString(TYPE_VALUE_KEY);
        }

        return "";
    }

    /**
     * Get remaining duration of legendary boost in ticks
     */
    public static int getLegendaryBoostDuration(PlayerEntity player) {
        if (!PIXELMON_LOADED) return 0;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(LEGENDARY_BOOST_KEY);
            return effectData.getInt(DURATION_KEY);
        }

        return 0;
    }

    /**
     * Get remaining duration of shiny boost in ticks
     */
    public static int getShinyBoostDuration(PlayerEntity player) {
        if (!PIXELMON_LOADED) return 0;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(SHINY_BOOST_KEY);
            return effectData.getInt(DURATION_KEY);
        }

        return 0;
    }

    /**
     * Get remaining duration of type boost in ticks
     */
    public static int getTypeBoostDuration(PlayerEntity player) {
        if (!PIXELMON_LOADED) return 0;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(TYPE_BOOST_KEY);
            return effectData.getInt(DURATION_KEY);
        }

        return 0;
    }

    /**
     * Update effect durations for a player
     */
    public static void updateEffects(PlayerEntity player) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getModTag(player);
        if (playerData == null) return;

        boolean updated = false;

        // Update legendary boost
        if (playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(LEGENDARY_BOOST_KEY);
            int duration = effectData.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effectData.putInt(DURATION_KEY, duration);
                playerData.put(LEGENDARY_BOOST_KEY, effectData);
                updated = true;
            }
        }

        // Update shiny boost
        if (playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(SHINY_BOOST_KEY);
            int duration = effectData.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effectData.putInt(DURATION_KEY, duration);
                playerData.put(SHINY_BOOST_KEY, effectData);
                updated = true;
            }
        }

        // Update hidden ability boost
        if (playerData.contains(HIDDEN_ABILITY_KEY)) {
            CompoundNBT effectData = playerData.getCompound(HIDDEN_ABILITY_KEY);
            int duration = effectData.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effectData.putInt(DURATION_KEY, duration);
                playerData.put(HIDDEN_ABILITY_KEY, effectData);
                updated = true;
            }
        }

        // Update type boost
        if (playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effectData = playerData.getCompound(TYPE_BOOST_KEY);
            int duration = effectData.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effectData.putInt(DURATION_KEY, duration);
                playerData.put(TYPE_BOOST_KEY, effectData);
                updated = true;
            }
        }

        // Save updated data
        if (updated) {
            saveModTag(player, playerData);
        }
    }

    // Helper methods to manage NBT data

    private static CompoundNBT getOrCreateModTag(PlayerEntity player) {
        CompoundNBT persistentData = player.getPersistentData();

        if (persistentData.contains(MOD_TAG)) {
            return persistentData.getCompound(MOD_TAG);
        } else {
            CompoundNBT modTag = new CompoundNBT();
            persistentData.put(MOD_TAG, modTag);
            return modTag;
        }
    }

    private static CompoundNBT getModTag(PlayerEntity player) {
        CompoundNBT persistentData = player.getPersistentData();

        if (persistentData.contains(MOD_TAG)) {
            return persistentData.getCompound(MOD_TAG);
        }

        return null;
    }

    private static void saveModTag(PlayerEntity player, CompoundNBT modTag) {
        player.getPersistentData().put(MOD_TAG, modTag);
    }
}