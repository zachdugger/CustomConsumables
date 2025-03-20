package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * This handler manages the spawn rates for Pixelmon based on the consumable effects.
 * It handles:
 * - Legendary spawns (base 30% with lure boosting to 100%)
 * - Shiny spawns (base 1/4096 with charm boosting to 50%)
 * - Type-specific spawns (boosted by 1000% for specific types)
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

    // Track spawn attempts to prevent spamming
    private static final Map<UUID, Integer> typeSpawnCooldowns = new HashMap<>();
    private static final int TYPE_SPAWN_COOLDOWN = 100; // 5 seconds (100 ticks)

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

            // Update cooldowns
            updateCooldowns();
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

        // Handle active type boost effects
        PlayerEntity player = event.player;
        if (PixelmonIntegration.hasTypeBoost(player)) {
            // Get boost info
            String boostedType = PixelmonIntegration.getBoostedType(player);
            int remainingDuration = PixelmonIntegration.getTypeBoostDuration(player);

            // Every 100 ticks (5 seconds), try to force a spawn of the boosted type
            if (remainingDuration > 0 && tickCounter % 100 == 0) {
                UUID playerId = player.getUUID();

                // Check cooldown
                if (!typeSpawnCooldowns.containsKey(playerId) || typeSpawnCooldowns.get(playerId) <= 0) {
                    // Try to force spawn the boosted type
                    forceTypeSpawn(player, boostedType);

                    // Set cooldown
                    typeSpawnCooldowns.put(playerId, TYPE_SPAWN_COOLDOWN);
                }

                // Create visual indicator particles to show the effect is active
                if (player instanceof ServerPlayerEntity) {
                    ServerWorld world = ((ServerPlayerEntity)player).getLevel();

                    // Send particles in a spiral around the player
                    double radius = 1.5;
                    for (int i = 0; i < 16; i++) {
                        double angle = (i / 16.0) * Math.PI * 2;
                        double x = player.getX() + Math.cos(angle) * radius;
                        double z = player.getZ() + Math.sin(angle) * radius;

                        world.sendParticles(
                                net.minecraft.particles.ParticleTypes.WITCH,
                                x, player.getY() + 0.5, z,
                                1, 0, 0, 0, 0.05
                        );
                    }
                }
            }
        }
    }

    /**
     * Force a spawn of the specified type near the player
     */
    private static void forceTypeSpawn(PlayerEntity player, String type) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return;
        }

        MinecraftServer server = player instanceof ServerPlayerEntity
                ? ((ServerPlayerEntity)player).getServer()
                : ServerLifecycleHooks.getCurrentServer();

        if (server != null) {
            try {
                // Execute the command to spawn a Pokémon of the specified type
                server.getCommands().performCommand(
                        server.createCommandSourceStack().withPermission(4),
                        "pokespawn " + type.toLowerCase()
                );

                CustomConsumables.getLogger().debug(
                        "Forced spawn of {} type Pokémon near player {}",
                        type, player.getName().getString()
                );
            } catch (Exception e) {
                // Just log at debug level, this is a supplementary feature
                CustomConsumables.getLogger().debug(
                        "Failed to force spawn {} type: {}",
                        type, e.getMessage()
                );
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
     * Update cooldown timers
     */
    private static void updateCooldowns() {
        typeSpawnCooldowns.entrySet().removeIf(entry -> {
            int remaining = entry.getValue() - 5;
            if (remaining <= 0) {
                return true; // Remove from map
            } else {
                entry.setValue(remaining);
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

            // If this is the boosted type, dramatically increase its chance to spawn
            if (boostedType.equalsIgnoreCase(pokemonType)) {
                // Always allow this type to spawn
                CustomConsumables.getLogger().debug(
                        "Player {} type boost active. Prioritizing {} type to spawn",
                        player.getName().getString(), pokemonType
                );
                return true; // SPAWN THIS TYPE
            } else if (multiplier >= 5.0f) {
                // For very high multipliers (5x or more), block other types 75% of the time
                float roll = random.nextFloat();
                boolean shouldBlock = roll <= 0.85f;

                if (shouldBlock) {
                    CustomConsumables.getLogger().debug(
                            "Player {} type boost active. Blocking {} type (prioritizing {})",
                            player.getName().getString(), pokemonType, boostedType
                    );
                    return false; // BLOCK OTHER TYPES 85% OF THE TIME
                } else {
                    CustomConsumables.getLogger().debug(
                            "Player {} type boost active. Allowing non-boosted {} type (15% chance)",
                            player.getName().getString(), pokemonType
                    );
                }
            }
        }

        return null; // Default behavior
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
            float multiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 1.0f);

            // If this is the boosted type, allow it
            if (boostedType.equalsIgnoreCase(pokemonType)) {
                return true;
            }

            // With 1000% multiplier (10x), block most non-matching types
            float roll = random.nextFloat();
            boolean allowSpawn = roll > 0.85f; // Only allow 15% of non-matching types

            return allowSpawn;
        }

        return true; // No type filter active
    }
}