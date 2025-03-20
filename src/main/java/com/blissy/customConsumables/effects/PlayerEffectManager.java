package com.blissy.customConsumables.effects;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import com.blissy.customConsumables.events.VisualFeedbackSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Manages custom effects for players, such as the legendary lure effect.
 * This class handles tracking, applying, and clearing effects.
 */
public class PlayerEffectManager {
    // Key used to store effects in player NBT data
    private static final String LEGENDARY_LURE_KEY = "customConsumables.legendaryLure";
    private static final String SHINY_BOOST_KEY = "customConsumables.shinyBoost";
    private static final String HIDDEN_ABILITY_BOOST_KEY = "customConsumables.hiddenAbilityBoost";
    private static final String TYPE_BOOST_KEY = "customConsumables.typeBoost";

    // Common keys for effect properties
    private static final String DURATION_KEY = "duration";
    private static final String CHANCE_KEY = "chance";
    private static final String TYPE_VALUE_KEY = "type";
    private static final String OVERRIDE_KEY = "override"; // For absolute values

    // Cache of active effects for quick access
    private static final Map<UUID, CompoundNBT> playerEffectsCache = new HashMap<>();

    // Random number generator for chance calculations
    private static final Random random = new Random();

    //
    // Legendary Lure Effect Methods
    //

    /**
     * Applies the legendary lure effect to a player for a specified duration and chance.
     *
     * @param player The player to receive the effect
     * @param durationTicks Duration of the effect in ticks (20 ticks = 1 second)
     * @param chance The percentage chance (0-100) for legendary spawns
     */
    public static void applyLegendaryLureEffect(PlayerEntity player, int durationTicks, float chance) {
        CompoundNBT playerData = getOrCreatePlayerData(player);

        // Create effect data
        CompoundNBT lureEffect = new CompoundNBT();
        lureEffect.putFloat(CHANCE_KEY, chance);
        lureEffect.putInt(DURATION_KEY, durationTicks);

        // Save to player data
        playerData.put(LEGENDARY_LURE_KEY, lureEffect);

        // Update cache
        playerEffectsCache.put(player.getUUID(), playerData);

        // Add visual feedback
        VisualFeedbackSystem.registerLegendaryLureEffect(player, durationTicks);

        // Apply to Pixelmon integration if available
        if (PixelmonIntegration.isPixelmonLoaded()) {
            PixelmonIntegration.applyLegendaryBoost(player, durationTicks, chance);
        }

        // Log effect application
        CustomConsumables.getLogger().info("Applied legendary lure effect to player {} with {}% chance for {} ticks",
                player.getName().getString(), chance, durationTicks);
    }

    /**
     * Checks if a player has an active legendary lure effect.
     *
     * @param player The player to check
     * @return true if the player has an active lure effect
     */
    public static boolean hasLegendaryLureEffect(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(LEGENDARY_LURE_KEY)) {
            CompoundNBT lureEffect = playerData.getCompound(LEGENDARY_LURE_KEY);
            int remainingDuration = lureEffect.getInt(DURATION_KEY);

            // Effect is active if duration is greater than 0
            return remainingDuration > 0;
        }
        return false;
    }

    /**
     * Gets the legendary lure chance percentage for a player.
     *
     * @param player The player to check
     * @param defaultChance Default chance to return if no effect is present
     * @return The current legendary spawn chance percentage (0-100)
     */
    public static float getLegendaryLureChance(PlayerEntity player, float defaultChance) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(LEGENDARY_LURE_KEY)) {
            CompoundNBT lureEffect = playerData.getCompound(LEGENDARY_LURE_KEY);
            return lureEffect.getFloat(CHANCE_KEY);
        }
        return defaultChance;
    }

    /**
     * Gets the remaining duration of a legendary lure effect.
     *
     * @param player The player to check
     * @return Remaining duration in ticks, or 0 if no effect is active
     */
    public static int getLegendaryLureRemainingDuration(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(LEGENDARY_LURE_KEY)) {
            CompoundNBT lureEffect = playerData.getCompound(LEGENDARY_LURE_KEY);
            return lureEffect.getInt(DURATION_KEY);
        }
        return 0;
    }

    //
    // Shiny Boost Effect Methods
    //

    /**
     * Applies the shiny boost effect to a player for a specified duration and chance.
     *
     * @param player The player to receive the effect
     * @param durationTicks Duration of the effect in ticks (20 ticks = 1 second)
     * @param absoluteChance The absolute percentage chance (0-100) for shiny encounters
     */
    public static void applyShinyBoostEffect(PlayerEntity player, int durationTicks, float absoluteChance) {
        CompoundNBT playerData = getOrCreatePlayerData(player);

        // Create effect data
        CompoundNBT effect = new CompoundNBT();
        effect.putFloat(CHANCE_KEY, absoluteChance);
        effect.putInt(DURATION_KEY, durationTicks);
        effect.putBoolean(OVERRIDE_KEY, true); // Mark as absolute chance override

        // Save to player data
        playerData.put(SHINY_BOOST_KEY, effect);

        // Update cache
        playerEffectsCache.put(player.getUUID(), playerData);

        // Apply to Pixelmon integration if available
        if (PixelmonIntegration.isPixelmonLoaded()) {
            PixelmonIntegration.applyShinyBoost(player, durationTicks, absoluteChance);
        }

        // Log effect application
        CustomConsumables.getLogger().info("Applied shiny boost effect to player {} with absolute {}% chance for {} ticks",
                player.getName().getString(), absoluteChance, durationTicks);
    }

    /**
     * Checks if a player has an active shiny boost effect.
     *
     * @param player The player to check
     * @return true if the player has an active shiny boost effect
     */
    public static boolean hasShinyBoostEffect(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            int remainingDuration = effect.getInt(DURATION_KEY);

            // Effect is active if duration is greater than 0
            return remainingDuration > 0;
        }
        return false;
    }

    /**
     * Gets the shiny boost multiplier or absolute chance for a player.
     *
     * @param player The player to check
     * @param defaultValue Default value to return if no effect is present
     * @return The current shiny boost value (absolute percentage or multiplier)
     */
    public static float getShinyBoostMultiplier(PlayerEntity player, float defaultValue) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            return effect.getFloat(CHANCE_KEY);
        }
        return defaultValue;
    }

    /**
     * Checks if the shiny boost is an absolute chance override or a multiplier.
     *
     * @param player The player to check
     * @return true if the shiny boost is an absolute chance, false if it's a multiplier
     */
    public static boolean isShinyBoostAbsolute(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            return effect.getBoolean(OVERRIDE_KEY);
        }
        return false;
    }

    //
    // Hidden Ability Effect Methods
    //

    /**
     * Applies the hidden ability effect to a player for a specified duration and chance.
     *
     * @param player The player to receive the effect
     * @param durationTicks Duration of the effect in ticks (20 ticks = 1 second)
     * @param chance The percentage chance (0-100) for hidden abilities
     */
    public static void applyHiddenAbilityEffect(PlayerEntity player, int durationTicks, float chance) {
        CompoundNBT playerData = getOrCreatePlayerData(player);

        // Create effect data
        CompoundNBT effect = new CompoundNBT();
        effect.putFloat(CHANCE_KEY, chance);
        effect.putInt(DURATION_KEY, durationTicks);

        // Save to player data
        playerData.put(HIDDEN_ABILITY_BOOST_KEY, effect);

        // Update cache
        playerEffectsCache.put(player.getUUID(), playerData);

        // Apply to Pixelmon integration if available
        if (PixelmonIntegration.isPixelmonLoaded()) {
            PixelmonIntegration.applyHiddenAbilityBoost(player, durationTicks, chance);
        }

        // Log effect application
        CustomConsumables.getLogger().info("Applied hidden ability effect to player {} with {}% chance for {} ticks",
                player.getName().getString(), chance, durationTicks);
    }

    /**
     * Checks if a player has an active hidden ability effect.
     *
     * @param player The player to check
     * @return true if the player has an active hidden ability effect
     */
    public static boolean hasHiddenAbilityEffect(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(HIDDEN_ABILITY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(HIDDEN_ABILITY_BOOST_KEY);
            int remainingDuration = effect.getInt(DURATION_KEY);

            // Effect is active if duration is greater than 0
            return remainingDuration > 0;
        }
        return false;
    }

    /**
     * Gets the hidden ability chance percentage for a player.
     *
     * @param player The player to check
     * @param defaultChance Default chance to return if no effect is present
     * @return The current hidden ability chance percentage (0-100)
     */
    public static float getHiddenAbilityChance(PlayerEntity player, float defaultChance) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(HIDDEN_ABILITY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(HIDDEN_ABILITY_BOOST_KEY);
            return effect.getFloat(CHANCE_KEY);
        }
        return defaultChance;
    }

    //
    // Type Attractor Effect Methods
    //

    /**
     * Applies the type attractor effect to a player for a specified duration and chance.
     *
     * @param player The player to receive the effect
     * @param type The Pokémon type to attract (e.g., "fire", "water", etc.)
     * @param durationTicks Duration of the effect in ticks (20 ticks = 1 second)
     * @param multiplierOrChance The multiplier value (1.0 = 100%, 5.0 = 500%)
     */
    public static void applyTypeAttractorEffect(PlayerEntity player, String type, int durationTicks, float multiplierOrChance) {
        CompoundNBT playerData = getOrCreatePlayerData(player);

        // Create effect data
        CompoundNBT effect = new CompoundNBT();
        effect.putFloat(CHANCE_KEY, multiplierOrChance);
        effect.putInt(DURATION_KEY, durationTicks);
        effect.putString(TYPE_VALUE_KEY, type.toLowerCase());

        // Save to player data
        playerData.put(TYPE_BOOST_KEY, effect);

        // Update cache
        playerEffectsCache.put(player.getUUID(), playerData);

        // Apply to Pixelmon integration if available
        if (PixelmonIntegration.isPixelmonLoaded()) {
            // If value is over 100, assume it's a percentage and convert to multiplier
            float multiplier = multiplierOrChance > 100 ? multiplierOrChance / 100.0f : multiplierOrChance;
            PixelmonIntegration.applyTypeBoost(player, type, durationTicks, multiplier);
        }

        // Log effect application
        CustomConsumables.getLogger().info("Applied {} type boost to player {} with {}% multiplier for {} ticks",
                type, player.getName().getString(), multiplierOrChance, durationTicks);
    }

    /**
     * Checks if a player has an active type attractor effect.
     *
     * @param player The player to check
     * @return true if the player has an active type attractor effect
     */
    public static boolean hasTypeAttractorEffect(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            int remainingDuration = effect.getInt(DURATION_KEY);

            // Effect is active if duration is greater than 0
            return remainingDuration > 0;
        }
        return false;
    }

    /**
     * Gets the type attractor chance percentage or multiplier for a player.
     *
     * @param player The player to check
     * @param defaultValue Default value to return if no effect is present
     * @return The current type attractor value (percentage or multiplier)
     */
    public static float getTypeAttractorChance(PlayerEntity player, float defaultValue) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getFloat(CHANCE_KEY);
        }
        return defaultValue;
    }

    /**
     * Gets the Pokémon type that is being attracted for a player.
     *
     * @param player The player to check
     * @return The Pokémon type or empty string if no effect is present
     */
    public static String getTypeAttractorType(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getString(TYPE_VALUE_KEY);
        }
        return "";
    }

    /**
     * Determines if a specific Pokémon type should be spawned
     * based on player's active type attractor effect.
     *
     * @param player The player to check for effects
     * @param pokemonType The type of the Pokémon being considered for spawn
     * @return true if the type should be spawned, false otherwise
     */
    public static boolean shouldSpawnType(PlayerEntity player, String pokemonType) {
        if (player == null) return true;

        boolean hasTypeEffect = hasTypeAttractorEffect(player);
        if (!hasTypeEffect) return true; // Allow all types if no effect

        String boostedType = getTypeAttractorType(player);
        float multiplier = getTypeAttractorChance(player, 0) / 100.0f; // Convert to multiplier

        // For high multiplier, HEAVILY prioritize the boosted type
        if (multiplier >= 5.0f) {
            boolean isMatch = boostedType.equalsIgnoreCase(pokemonType);

            if (isMatch) {
                // Always allow the boosted type
                return true;
            } else {
                // Block other types with 85% probability for a strong boost effect
                float roll = random.nextFloat();
                return roll > 0.85f; // Only allow 15% of other types to spawn
            }
        }

        // For smaller multipliers, boost the chance but don't block other types as aggressively
        if (boostedType.equalsIgnoreCase(pokemonType)) {
            // This is the boosted type, give it a higher chance
            return true; // Always allow the boosted type
        }

        // For non-boosted types with small multiplier
        // Allow them at a reduced rate
        float roll = random.nextFloat();
        return roll <= (1.0f / multiplier); // Inverse relation to multiplier
    }

    //
    // Legacy Methods for Compatibility
    //

    /**
     * Legacy method for compatibility with other classes
     */
    public static void applyLegendaryBoost(PlayerEntity player, int duration) {
        applyLegendaryLureEffect(player, duration, 100.0f);
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static void applyShinyBoost(PlayerEntity player, int duration) {
        // Changed to absolute 50% chance instead of 3x multiplier
        applyShinyBoostEffect(player, duration, 50.0f);
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static void applyHiddenAbilityBoost(PlayerEntity player, int duration) {
        applyHiddenAbilityEffect(player, duration, 100.0f);
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static void applyTypeBoost(PlayerEntity player, String type, int duration) {
        // 500% multiplier for type boost
        applyTypeAttractorEffect(player, type, duration, 500.0f);
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static int getRemainingLegendaryBoostTime(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(LEGENDARY_LURE_KEY)) {
            CompoundNBT effect = playerData.getCompound(LEGENDARY_LURE_KEY);
            return effect.getInt(DURATION_KEY);
        }
        return 0;
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static int getRemainingShinyBoostTime(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            return effect.getInt(DURATION_KEY);
        }
        return 0;
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static int getRemainingHiddenAbilityBoostTime(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(HIDDEN_ABILITY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(HIDDEN_ABILITY_BOOST_KEY);
            return effect.getInt(DURATION_KEY);
        }
        return 0;
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static int getRemainingTypeBoostTime(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getInt(DURATION_KEY);
        }
        return 0;
    }

    /**
     * Legacy method for compatibility with other classes
     */
    public static String getTypeBoost(PlayerEntity player) {
        return getTypeAttractorType(player);
    }

    //
    // Effect Management Methods
    //

    /**
     * Updates all player effects, reducing durations and removing expired effects.
     * This should be called periodically, such as every server tick.
     *
     * @param player The player to update
     */
    public static void updatePlayerEffects(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData == null) return;

        boolean updated = false;

        // Update legendary lure effect
        if (playerData.contains(LEGENDARY_LURE_KEY)) {
            updated = updateEffectDuration(playerData, LEGENDARY_LURE_KEY, player) || updated;
        }

        // Update other effects
        if (playerData.contains(SHINY_BOOST_KEY)) {
            updated = updateEffectDuration(playerData, SHINY_BOOST_KEY, player) || updated;
        }

        if (playerData.contains(HIDDEN_ABILITY_BOOST_KEY)) {
            updated = updateEffectDuration(playerData, HIDDEN_ABILITY_BOOST_KEY, player) || updated;
        }

        if (playerData.contains(TYPE_BOOST_KEY)) {
            updated = updateEffectDuration(playerData, TYPE_BOOST_KEY, player) || updated;
        }

        // Only update cache if something changed
        if (updated) {
            playerEffectsCache.put(player.getUUID(), playerData);
        }
    }

    /**
     * Method to support ConsumableEvents
     */
    public static void tickEffects(PlayerEntity player) {
        updatePlayerEffects(player);
    }

    /**
     * Updates the duration of a specific effect
     */
    private static boolean updateEffectDuration(CompoundNBT playerData, String effectKey, PlayerEntity player) {
        CompoundNBT effect = playerData.getCompound(effectKey);
        int duration = effect.getInt(DURATION_KEY);

        if (duration > 0) {
            duration--;
            effect.putInt(DURATION_KEY, duration);
            playerData.put(effectKey, effect);

            // Log when effect expires
            if (duration == 0) {
                CustomConsumables.getLogger().info("Effect {} expired for player {}",
                        effectKey, player.getName().getString());

                // Notify player when effect expires
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                    // Different messages based on effect type
                    if (effectKey.equals(LEGENDARY_LURE_KEY)) {
                        serverPlayer.sendMessage(
                                new StringTextComponent(TextFormatting.GOLD + "Your Legendary Lure effect has expired!"),
                                player.getUUID()
                        );
                    } else if (effectKey.equals(SHINY_BOOST_KEY)) {
                        serverPlayer.sendMessage(
                                new StringTextComponent(TextFormatting.AQUA + "Your Shiny Charm effect has expired!"),
                                player.getUUID()
                        );
                    } else if (effectKey.equals(HIDDEN_ABILITY_BOOST_KEY)) {
                        serverPlayer.sendMessage(
                                new StringTextComponent(TextFormatting.LIGHT_PURPLE + "Your Hidden Ability boost has expired!"),
                                player.getUUID()
                        );
                    } else if (effectKey.equals(TYPE_BOOST_KEY)) {
                        String typeName = effect.getString(TYPE_VALUE_KEY);
                        if (!typeName.isEmpty()) {
                            typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1).toLowerCase();
                            serverPlayer.sendMessage(
                                    new StringTextComponent(TextFormatting.RED + "Your " + typeName + " Type Attractor has expired!"),
                                    player.getUUID()
                            );
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Gets or creates player effect data.
     */
    private static CompoundNBT getOrCreatePlayerData(PlayerEntity player) {
        // Check cache first
        UUID playerID = player.getUUID();
        if (playerEffectsCache.containsKey(playerID)) {
            return playerEffectsCache.get(playerID);
        }

        // Otherwise get from player's persistent data
        CompoundNBT persistentData = player.getPersistentData();
        CompoundNBT playerData;

        if (persistentData.contains(CustomConsumables.MOD_ID)) {
            playerData = persistentData.getCompound(CustomConsumables.MOD_ID);
        } else {
            playerData = new CompoundNBT();
            persistentData.put(CustomConsumables.MOD_ID, playerData);
        }

        // Update cache
        playerEffectsCache.put(playerID, playerData);
        return playerData;
    }

    /**
     * Gets existing player data or null if none exists.
     */
    private static CompoundNBT getPlayerData(PlayerEntity player) {
        // Check cache first
        UUID playerID = player.getUUID();
        if (playerEffectsCache.containsKey(playerID)) {
            return playerEffectsCache.get(playerID);
        }

        // Otherwise check player's persistent data
        CompoundNBT persistentData = player.getPersistentData();
        if (persistentData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT playerData = persistentData.getCompound(CustomConsumables.MOD_ID);
            playerEffectsCache.put(playerID, playerData);
            return playerData;
        }

        return null;
    }

    /**
     * Determines if a Pokémon spawn should be forced to be shiny
     * based on player's active effects.
     *
     * @param player The player to check for effects
     * @return true if the spawn should be shiny
     */
    public static boolean shouldBeShiny(PlayerEntity player) {
        if (player == null) return false;

        boolean hasShinyEffect = hasShinyBoostEffect(player);
        if (!hasShinyEffect) return false;

        float chance = getShinyBoostMultiplier(player, 0); // Get absolute percentage

        // Roll for the chance (now using percentage directly)
        float roll = random.nextFloat() * 100.0f;
        boolean success = roll <= chance;

        // Debug output
        if (success && player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).sendMessage(
                    new StringTextComponent(TextFormatting.AQUA +
                            "★ Shiny boost activated! Pokémon will be shiny! ★"),
                    player.getUUID()
            );

            CustomConsumables.getLogger().info(
                    "Player {} rolled {} vs {}% for shiny. SUCCESS!",
                    player.getName().getString(), roll, chance
            );
        }

        return success;
    }
}