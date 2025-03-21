package com.blissy.customConsumables.compat;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Integration with Pixelmon for the Custom Consumables mod.
 * Uses NBT data to track effects and interfaces with Pixelmon's spawn system.
 */
public class PixelmonIntegration {
    private static final boolean PIXELMON_LOADED = ModList.get().isLoaded("pixelmon");
    private static final Random RANDOM = new Random();

    // NBT keys for storing effect data
    private static final String MOD_TAG = "CustomConsumables";
    private static final String LEGENDARY_BOOST_KEY = "legendaryBoost";
    private static final String SHINY_BOOST_KEY = "shinyBoost";
    private static final String TYPE_BOOST_KEY = "typeBoost";
    private static final String TYPE_VALUE_KEY = "boostedType";
    private static final String TYPE_MULTIPLIER_KEY = "typeMultiplier";

    // Common keys for effect properties
    private static final String DURATION_KEY = "duration";
    private static final String CHANCE_KEY = "chance";
    private static final String OVERRIDE_KEY = "override";

    // Cache for notification cooldowns to prevent spam
    private static final Map<UUID, Long> notificationCooldowns = new HashMap<>();

    /**
     * Initialize the Pixelmon integration
     */
    public static void initialize() {
        if (!PIXELMON_LOADED) {
            CustomConsumables.getLogger().info("Pixelmon not detected, integration features will be limited");
            return;
        }

        CustomConsumables.getLogger().info("Initializing Pixelmon integration");
    }

    /**
     * Check if Pixelmon is loaded
     */
    public static boolean isPixelmonLoaded() {
        return PIXELMON_LOADED;
    }

    /**
     * Force spawn a Pokémon of a specific type near a player
     */
    public static boolean forceSpawnType(PlayerEntity player, String type) {
        if (!PIXELMON_LOADED || !(player instanceof ServerPlayerEntity)) return false;

        try {
            // Use Pixelmon's pokespawn command with the type parameter
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Generate a random offset for more natural looking spawns
            int xOffset = RANDOM.nextInt(10) - 5;
            int zOffset = RANDOM.nextInt(10) - 5;

            // Execute the command with the type name
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn " + type.toLowerCase() + " ~" + xOffset + " ~ ~" + zOffset
            );

            // Notify player with cooldown
            notifyPlayerWithCooldown(player,
                    TextFormatting.GREEN + "Your Type Attractor is working!",
                    10000); // 10 second cooldown

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error forcing spawn: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Force spawn a specific Pokémon species near a player
     */
    public static boolean forceSpawnSpecies(PlayerEntity player, String speciesName) {
        if (!PIXELMON_LOADED || !(player instanceof ServerPlayerEntity)) return false;

        try {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Generate a random offset for more natural looking spawns
            int xOffset = RANDOM.nextInt(10) - 5;
            int zOffset = RANDOM.nextInt(10) - 5;

            // Execute the spawn command with the species name
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn " + speciesName.toLowerCase() + " ~" + xOffset + " ~ ~" + zOffset
            );

            // Notify player with cooldown to prevent spam
            notifyPlayerWithCooldown(player,
                    TextFormatting.GREEN + "Your Type Attractor summoned a " +
                            capitalize(speciesName) + "!",
                    15000); // 15 second cooldown

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error forcing spawn: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Apply a type boost effect to a player
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

        // Format duration for display
        int durationSeconds = durationTicks / 20;
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;

        // Notify the player
        player.sendMessage(
                new StringTextComponent(TextFormatting.GREEN +
                        capitalize(type) + " Type Attractor activated! " +
                        TextFormatting.YELLOW + "(" + minutes + "m " + seconds + "s)"),
                player.getUUID()
        );

        CustomConsumables.getLogger().info("Applied {} type boost to {} with {}x multiplier for {} ticks",
                type, player.getName().getString(), multiplier, durationTicks);
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

        // Notify the player
        player.sendMessage(
                new StringTextComponent(TextFormatting.GOLD +
                        "Legendary Lure activated! Duration: " + formatTime(durationTicks)),
                player.getUUID()
        );

        CustomConsumables.getLogger().info("Applied legendary boost to {} with {}% chance for {} ticks",
                player.getName().getString(), chance, durationTicks);
    }

    /**
     * Apply a shiny boost effect to a player
     */
    public static void applyShinyBoost(PlayerEntity player, int durationTicks, float chance) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getOrCreateModTag(player);
        CompoundNBT effectData = new CompoundNBT();

        effectData.putInt(DURATION_KEY, durationTicks);
        effectData.putFloat(CHANCE_KEY, chance);
        effectData.putBoolean(OVERRIDE_KEY, true);

        playerData.put(SHINY_BOOST_KEY, effectData);
        saveModTag(player, playerData);

        // Notify the player
        player.sendMessage(
                new StringTextComponent(TextFormatting.AQUA +
                        "Shiny Charm activated! " + chance + "% chance for " + formatTime(durationTicks)),
                player.getUUID()
        );

        CustomConsumables.getLogger().info("Applied shiny boost to {} with {}% chance for {} ticks",
                player.getName().getString(), chance, durationTicks);
    }

    /**
     * Check if a player has an active type boost
     */
    public static boolean hasTypeBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED) return false;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getInt(DURATION_KEY) > 0;
        }

        return false;
    }

    /**
     * Get the type that the player is currently boosting
     */
    public static String getBoostedType(PlayerEntity player) {
        if (!PIXELMON_LOADED) return "";

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getString(TYPE_VALUE_KEY);
        }

        return "";
    }

    /**
     * Get the multiplier for the player's type boost
     */
    public static float getTypeBoostMultiplier(PlayerEntity player, float defaultValue) {
        if (!PIXELMON_LOADED) return defaultValue;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getFloat(TYPE_MULTIPLIER_KEY);
        }

        return defaultValue;
    }

    /**
     * Get remaining duration of type boost in ticks
     */
    public static int getTypeBoostDuration(PlayerEntity player) {
        if (!PIXELMON_LOADED) return 0;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            return effect.getInt(DURATION_KEY);
        }

        return 0;
    }

    /**
     * Check if a player has an active legendary boost
     */
    public static boolean hasLegendaryBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED) return false;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(LEGENDARY_BOOST_KEY);
            return effect.getInt(DURATION_KEY) > 0;
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
            CompoundNBT effect = playerData.getCompound(LEGENDARY_BOOST_KEY);
            return effect.getFloat(CHANCE_KEY);
        }

        return defaultChance;
    }

    /**
     * Get remaining duration of legendary boost in ticks
     */
    public static int getLegendaryBoostDuration(PlayerEntity player) {
        if (!PIXELMON_LOADED) return 0;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(LEGENDARY_BOOST_KEY);
            return effect.getInt(DURATION_KEY);
        }

        return 0;
    }

    /**
     * Check if a player has an active shiny boost
     */
    public static boolean hasShinyBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED) return false;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            return effect.getInt(DURATION_KEY) > 0;
        }

        return false;
    }

    /**
     * Get the shiny spawn chance for a player
     */
    public static float getShinyChance(PlayerEntity player, float defaultChance) {
        if (!PIXELMON_LOADED) return defaultChance;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            return effect.getFloat(CHANCE_KEY);
        }

        return defaultChance;
    }

    /**
     * Get remaining duration of shiny boost in ticks
     */
    public static int getShinyBoostDuration(PlayerEntity player) {
        if (!PIXELMON_LOADED) return 0;

        CompoundNBT playerData = getModTag(player);
        if (playerData != null && playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            return effect.getInt(DURATION_KEY);
        }

        return 0;
    }

    /**
     * Update all effects for a player (reduce durations)
     */
    public static void updateEffects(PlayerEntity player) {
        if (!PIXELMON_LOADED) return;

        CompoundNBT playerData = getModTag(player);
        if (playerData == null) return;

        boolean updated = false;

        // Update type boost
        if (playerData.contains(TYPE_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(TYPE_BOOST_KEY);
            int duration = effect.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effect.putInt(DURATION_KEY, duration);
                playerData.put(TYPE_BOOST_KEY, effect);
                updated = true;

                // Notify when approaching expiration
                if (duration == 600) { // 30 seconds left
                    String type = effect.getString(TYPE_VALUE_KEY);
                    notifyPlayerWithCooldown(player,
                            TextFormatting.YELLOW + capitalize(type) +
                                    " Type Attractor effect has 30 seconds left!",
                            10000);
                }

                // Notify on expiration
                if (duration == 0) {
                    String type = effect.getString(TYPE_VALUE_KEY);
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.RED +
                                    capitalize(type) + " Type Attractor has expired!"),
                            player.getUUID()
                    );
                }
            }
        }

        // Update legendary boost
        if (playerData.contains(LEGENDARY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(LEGENDARY_BOOST_KEY);
            int duration = effect.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effect.putInt(DURATION_KEY, duration);
                playerData.put(LEGENDARY_BOOST_KEY, effect);
                updated = true;

                // Notify on expiration
                if (duration == 0) {
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.RED +
                                    "Legendary Lure effect has expired!"),
                            player.getUUID()
                    );
                }
            }
        }

        // Update shiny boost
        if (playerData.contains(SHINY_BOOST_KEY)) {
            CompoundNBT effect = playerData.getCompound(SHINY_BOOST_KEY);
            int duration = effect.getInt(DURATION_KEY);
            if (duration > 0) {
                duration--;
                effect.putInt(DURATION_KEY, duration);
                playerData.put(SHINY_BOOST_KEY, effect);
                updated = true;

                // Notify on expiration
                if (duration == 0) {
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.RED +
                                    "Shiny Charm effect has expired!"),
                            player.getUUID()
                    );
                }
            }
        }

        // Save if anything changed
        if (updated) {
            saveModTag(player, playerData);
        }
    }

    // Helper methods

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

    /**
     * Helper method to notify a player with cooldown to prevent spam
     */
    private static void notifyPlayerWithCooldown(PlayerEntity player, String message, long cooldownMs) {
        if (!(player instanceof ServerPlayerEntity)) return;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Check if we're in cooldown period
        if (notificationCooldowns.containsKey(playerId)) {
            long lastNotification = notificationCooldowns.get(playerId);
            if (currentTime - lastNotification < cooldownMs) {
                return; // Still in cooldown, don't send message
            }
        }

        // Send message and update cache
        player.sendMessage(new StringTextComponent(message), player.getUUID());
        notificationCooldowns.put(playerId, currentTime);
    }

    /**
     * Format ticks into a readable time string
     */
    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;

        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    /**
     * Capitalize the first letter of a string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}