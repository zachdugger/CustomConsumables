package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * This handler manages the spawn rates for Pixelmon based on the consumable effects.
 * It handles:
 * - Legendary spawns (base 30% with lure boosting to 100%)
 * - Shiny spawns (base 1/4096 with charm boosting to 50%)
 * - Type-specific spawns (boosted by 500% for specific types)
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PixelmonSpawnHandler {
    private static final Random random = new Random();

    // Constants for base spawn rates
    private static final float BASE_LEGENDARY_CHANCE = 30.0f; // 30% base chance for legendaries in spawn checks
    private static final float BASE_SHINY_CHANCE = 0.024f;    // ~1/4096 for shiny

    // Track players that are in test mode
    private static final Map<UUID, Integer> testModeDuration = new HashMap<>();
    private static int tickCounter = 0;

    /**
     * Main tick event handler to process spawn rate modifications
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        tickCounter++;

        // Only perform spawn processing every 5 ticks to reduce overhead
        if (tickCounter % 5 == 0) {
            // Update test mode timers
            updateTestModes();
        }
    }

    /**
     * Handle player tick for effect updates
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        // Update effects using the Pixelmon integration
        if (PixelmonIntegration.isPixelmonLoaded()) {
            PixelmonIntegration.updateEffects(event.player);
        }

        // If player is in test mode, perform spawn checks frequently
        if (testModeDuration.containsKey(event.player.getUUID())) {
            // Every 20 ticks (1 second) for players in test mode
            if (tickCounter % 20 == 0) {
                forceSpawnTick(event.player);
            }
        }
    }

    /**
     * Update test mode timers for all players
     */
    private static void updateTestModes() {
        // Use an Iterator to avoid ConcurrentModificationException
        testModeDuration.entrySet().removeIf(entry -> {
            UUID playerID = entry.getKey();
            int remainingTime = entry.getValue();

            // Decrease timer
            remainingTime -= 5;

            if (remainingTime <= 0) {
                return true; // Remove from map
            } else {
                entry.setValue(remainingTime);
                return false;
            }
        });
    }

    /**
     * Force a spawn tick for testing purposes
     */
    public static boolean forceSpawnTick(PlayerEntity player) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return false;
        }

        // This simulates what happens during a normal Pixelmon spawn tick
        boolean result = checkForLegendarySpawn(player);

        return result;
    }

    /**
     * Check if a legendary should spawn based on the player's lure status
     */
    public static boolean checkForLegendarySpawn(PlayerEntity player) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return false;
        }

        // See if the player has an active legendary lure
        if (PixelmonIntegration.hasLegendaryBoost(player)) {
            float boostChance = PixelmonIntegration.getLegendaryChance(player, 100.0f);

            // Calculate final chance - for testing, we're going with the full boost value
            // In a real implementation, you might use: BASE_LEGENDARY_CHANCE * (boostChance / 100.0f)
            float finalChance = boostChance;

            // Roll for legendary spawn
            float roll = random.nextFloat() * 100.0f;
            boolean success = roll <= finalChance;

            CustomConsumables.getLogger().info(
                    "Player {} legendary spawn check with {}% chance. Roll: {}. Success: {}",
                    player.getName().getString(), finalChance, roll, success
            );

            // Show visual feedback if successful
            if (success && player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                serverPlayer.sendMessage(
                        new StringTextComponent(TextFormatting.GOLD +
                                "Legendary spawn check successful! (roll: " +
                                String.format("%.1f", roll) + "% vs chance: " +
                                String.format("%.1f", finalChance) + "%)"),
                        player.getUUID()
                );

                // Show particles
                VisualFeedbackSystem.registerLegendarySpawnCheck(serverPlayer, true);
            }

            return success;
        }

        // No legendary lure active, use base chance
        float roll = random.nextFloat() * 100.0f;
        boolean success = roll <= BASE_LEGENDARY_CHANCE;

        return success;
    }

    /**
     * Check if a spawn should be shiny based on player effects
     * This function supports OVERRIDE MODE (direct chance instead of multiplier)
     */
    public static boolean checkForShinySpawn(PlayerEntity player) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return false;
        }

        // See if the player has an active shiny charm
        if (PixelmonIntegration.hasShinyBoost(player)) {
            float shinyChance = PixelmonIntegration.getShinyChance(player, BASE_SHINY_CHANCE);

            // Direct percentage chance (should be 50% for testing)
            float roll = random.nextFloat() * 100.0f;
            boolean success = roll <= shinyChance;

            CustomConsumables.getLogger().info(
                    "Player {} shiny spawn check with {}% chance. Roll: {}. Success: {}",
                    player.getName().getString(), shinyChance, roll, success
            );

            if (success && player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                serverPlayer.sendMessage(
                        new StringTextComponent(TextFormatting.AQUA +
                                "★ Shiny boost active! Pokémon will be shiny! ★"),
                        player.getUUID()
                );
            }

            return success;
        }

        // Default shiny rate (~1/4096)
        float roll = random.nextFloat() * 100.0f;
        boolean success = roll <= BASE_SHINY_CHANCE;

        return success;
    }

    /**
     * Check if a type should spawn based on player's type booster
     * Returns:
     * - true if this is the boosted type and should SPAWN
     * - false if this is NOT the boosted type and should NOT spawn
     * - null if type boost isn't active (default logic applies)
     */
    public static Boolean shouldSpawnType(PlayerEntity player, String pokemonType) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return null; // No boost active
        }

        // Check if player has a type boost
        if (PixelmonIntegration.hasTypeBoost(player)) {
            String boostedType = PixelmonIntegration.getBoostedType(player);
            float multiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 1.0f);

            // If multiplier is very high (5x in this case), we should ONLY spawn the boosted type
            // and reject other types
            if (multiplier >= 5.0f) {
                // If this is the boosted type, always spawn it
                if (boostedType.equalsIgnoreCase(pokemonType)) {
                    CustomConsumables.getLogger().info(
                            "Player {} type boost active. Allowing {} type to spawn (exclusively)",
                            player.getName().getString(), pokemonType
                    );
                    return true; // SPAWN THIS TYPE
                } else {
                    CustomConsumables.getLogger().info(
                            "Player {} type boost active. Blocking {} type (only {} allowed)",
                            player.getName().getString(), pokemonType, boostedType
                    );
                    return false; // DON'T SPAWN OTHER TYPES
                }
            }
            // For lower multipliers, boost the chance of the selected type
            else {
                // If this is the boosted type, increase its chance
                if (boostedType.equalsIgnoreCase(pokemonType)) {
                    float roll = random.nextFloat();
                    boolean boost = roll <= 0.8f; // 80% chance to select the boosted type

                    if (boost) {
                        CustomConsumables.getLogger().info(
                                "Player {} type boost active. Boosting {} type spawn chance",
                                player.getName().getString(), pokemonType
                        );
                    }

                    return boost;
                }
            }
        }

        return null; // No boost active or not applicable
    }

    /**
     * Enable test mode for a player (more frequent spawn checks)
     */
    public static void startTestMode(PlayerEntity player, int durationSeconds) {
        if (player == null || !PixelmonIntegration.isPixelmonLoaded()) {
            return;
        }

        testModeDuration.put(player.getUUID(), durationSeconds * 20); // Convert to ticks

        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            serverPlayer.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN +
                            "Test mode active for " + durationSeconds + " seconds. " +
                            "Spawn checks will happen every second."),
                    player.getUUID()
            );
        }

        CustomConsumables.getLogger().info(
                "Started test mode for player {} for {} seconds",
                player.getName().getString(), durationSeconds
        );
    }

    /**
     * Test method for shiny chance - this should be called when a Pokémon spawns
     * to determine if it should be forced shiny
     */
    public static boolean testShinySpawn(PlayerEntity player) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return false;
        }

        if (PixelmonIntegration.hasShinyBoost(player)) {
            float shinyChance = PixelmonIntegration.getShinyChance(player, BASE_SHINY_CHANCE);
            float roll = random.nextFloat() * 100.0f;
            boolean success = roll <= shinyChance;

            CustomConsumables.getLogger().info(
                    "Shiny test for player {}: {}% chance. Roll: {}. Success: {}",
                    player.getName().getString(), shinyChance, roll, success
            );

            return success;
        }

        return false;
    }

    /**
     * Test method for type boost - this should be called when determining what Pokémon to spawn
     */
    public static boolean testTypeFilter(PlayerEntity player, String pokemonType) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return true; // By default, allow any type
        }

        if (PixelmonIntegration.hasTypeBoost(player)) {
            String boostedType = PixelmonIntegration.getBoostedType(player);

            // With 500% multiplier, ONLY spawn the boosted type
            return boostedType.equalsIgnoreCase(pokemonType);
        }

        return true; // No type filter active
    }
}