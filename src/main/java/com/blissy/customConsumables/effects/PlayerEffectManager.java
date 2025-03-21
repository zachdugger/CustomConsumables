package com.blissy.customConsumables.effects;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Simplified Manager for custom player effects like type boosting
 */
public class PlayerEffectManager {
    // NBT keys for storing effect data
    private static final String LEGENDARY_LURE_KEY = "customConsumables.legendaryLure";
    private static final String SHINY_BOOST_KEY = "customConsumables.shinyBoost";
    private static final String TYPE_BOOST_KEY = "customConsumables.typeBoost";

    // Common keys for effect properties
    private static final String DURATION_KEY = "duration";
    private static final String CHANCE_KEY = "chance";
    private static final String TYPE_VALUE_KEY = "type";

    // Cache of active effects for quick access
    private static final Map<UUID, CompoundNBT> playerEffectsCache = new HashMap<>();

    // Random number generator for chance calculations
    private static final Random random = new Random();

    /**
     * Applies the type attractor effect to a player for a specified duration.
     */
    public static void applyTypeAttractorEffect(PlayerEntity player, String type, int durationTicks, float multiplier) {
        CompoundNBT playerData = getOrCreatePlayerData(player);

        // Create effect data
        CompoundNBT effect = new CompoundNBT();
        effect.putFloat(CHANCE_KEY, multiplier);
        effect.putInt(DURATION_KEY, durationTicks);
        effect.putString(TYPE_VALUE_KEY, type.toLowerCase());

        // Save to player data
        playerData.put(TYPE_BOOST_KEY, effect);

        // Update cache
        playerEffectsCache.put(player.getUUID(), playerData);

        // Format duration for display
        int durationSeconds = durationTicks / 20;
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;

        // Notify the player if this is a server player
        if (player instanceof ServerPlayerEntity) {
            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

            player.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN +
                            typeName + " Type Attractor activated! " +
                            TextFormatting.YELLOW + "(" + minutes + "m " + seconds + "s)"),
                    player.getUUID()
            );
        }

        CustomConsumables.getLogger().info("Applied {} type boost to {} with {}x multiplier for {} ticks",
                type, player.getName().getString(), multiplier, durationTicks);
    }

    /**
     * Checks if a player has an active type attractor effect.
     */
    public static boolean hasTypeAttractorEffect(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            int remainingDuration = effect.getInt(DURATION_KEY);
            return remainingDuration > 0;
        }
        return false;
    }

    /**
     * Gets the type attractor multiplier for a player.
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
     * Get remaining duration of type boost in ticks
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
     * Updates all player effects, reducing durations and removing expired effects.
     * Call this periodically, such as every server tick.
     */
    public static void tickEffects(PlayerEntity player) {
        CompoundNBT playerData = getPlayerData(player);
        if (playerData == null) return;

        boolean updated = false;

        // Update type boost effect
        if (playerData.contains(TYPE_BOOST_KEY)) {
            updated = updateEffectDuration(playerData, TYPE_BOOST_KEY, player) || updated;
        }

        // Update other effects (legacy support)
        if (playerData.contains(LEGENDARY_LURE_KEY)) {
            updated = updateEffectDuration(playerData, LEGENDARY_LURE_KEY, player) || updated;
        }

        if (playerData.contains(SHINY_BOOST_KEY)) {
            updated = updateEffectDuration(playerData, SHINY_BOOST_KEY, player) || updated;
        }

        // Only update cache if something changed
        if (updated) {
            playerEffectsCache.put(player.getUUID(), playerData);
        }
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

            // Log and notify when effect expires
            if (duration == 0) {
                CustomConsumables.getLogger().info("Effect {} expired for player {}",
                        effectKey, player.getName().getString());

                // Notify player when effect expires
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                    if (effectKey.equals(TYPE_BOOST_KEY)) {
                        String typeName = effect.getString(TYPE_VALUE_KEY);
                        if (!typeName.isEmpty()) {
                            typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1).toLowerCase();
                            serverPlayer.sendMessage(
                                    new StringTextComponent(TextFormatting.RED + "Your " + typeName + " Type Attractor has expired!"),
                                    player.getUUID()
                            );

                            // Clear boost with command when expired
                            if (serverPlayer.getServer() != null) {
                                try {
                                    serverPlayer.getServer().getCommands().performCommand(
                                            serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                                            "pokespawn clearboost"
                                    );
                                } catch (Exception e) {
                                    // Just log and continue
                                    CustomConsumables.getLogger().debug("Could not clear boost: {}", e.getMessage());
                                }
                            }
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
}