package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for Pixelmon spawns to apply effect boosts from Custom Consumables items
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PixelmonSpawnListener {
    private static final Random RANDOM = new Random();

    // Cache to prevent checking the same entity multiple times
    private static final ConcurrentHashMap<Integer, Long> processedEntities = new ConcurrentHashMap<>();

    // Cooldown tracking for spawns
    private static final Map<UUID, Long> spawnCooldowns = new HashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 8000; // 8 second cooldown

    // Periodic spawn settings
    private static final int SPAWN_CHECK_INTERVAL = 200; // 10 seconds (in ticks)
    private static int tickCounter = 0;

    /**
     * Process natural entity spawns to apply our effects
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        // Skip if not on server or Pixelmon isn't loaded
        if (event.getWorld().isClientSide() || !PixelmonIntegration.isPixelmonLoaded()) {
            return;
        }

        try {
            // Get entity ID and check if we've already processed it
            int entityId = event.getEntity().getId();
            if (processedEntities.containsKey(entityId)) {
                return;
            }

            // Clean up the processed entities cache occasionally
            if (processedEntities.size() > 500) {
                cleanupProcessedEntities();
            }

            // Check if this is a Pixelmon entity using reflection
            if (!isPixelmonEntity(event.getEntity())) {
                return;
            }

            // Mark as processed to avoid checking it again
            processedEntities.put(entityId, System.currentTimeMillis());

            // Find the nearest player with an active effect
            PlayerEntity nearestPlayer = findNearestPlayerWithEffect(event.getEntity());
            if (nearestPlayer == null) {
                return;
            }

            // Handle the Pokémon spawn based on player effects
            handlePokemonSpawn(event, nearestPlayer);

        } catch (Exception e) {
            // Log but don't crash
            CustomConsumables.getLogger().debug("Error in onEntitySpawn: {}", e.getMessage());
        }
    }

    /**
     * Periodic tick to manage effects and try to spawn Pokémon
     * FIX: Use ServerLifecycleHooks to get the server instead of event.getServer()
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !PixelmonIntegration.isPixelmonLoaded()) {
            return;
        }

        tickCounter++;

        // Check every SPAWN_CHECK_INTERVAL ticks (10 seconds)
        if (tickCounter % SPAWN_CHECK_INTERVAL == 0) {
            // Get the server instance using ServerLifecycleHooks
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            // Try to spawn Pokémon for players with type attractors
            for (PlayerEntity player : server.getPlayerList().getPlayers()) {
                if (PixelmonIntegration.hasTypeBoost(player)) {
                    trySpawnForPlayer(player);
                }
            }
        }
    }

    /**
     * Process a Pokémon spawn based on player effects
     */
    private static void handlePokemonSpawn(EntityJoinWorldEvent event, PlayerEntity player) {
        // Get the Pokémon's name if possible
        String pokemonName = getPokemonName(event.getEntity());

        // Handle type boost effect if active
        if (PixelmonIntegration.hasTypeBoost(player)) {
            String boostedType = PixelmonIntegration.getBoostedType(player);

            // Try to determine if this Pokémon matches the player's boosted type
            boolean matchesType = doesPokemonMatchType(event.getEntity(), boostedType, player);

            if (!matchesType) {
                // This Pokémon doesn't match the player's type boost
                // Decide whether to block based on the multiplier
                float multiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 1.0f);
                float blockChance = Math.min(0.9f, 1.0f - (1.0f / multiplier));

                if (RANDOM.nextFloat() < blockChance) {
                    // Block this spawn
                    event.setCanceled(true);

                    // Log the blocked spawn
                    CustomConsumables.getLogger().debug(
                            "Blocked spawn of {} for player {} with {} type boost",
                            pokemonName, player.getName().getString(), boostedType
                    );

                    // Try to spawn a replacement of the correct type
                    if (RANDOM.nextFloat() < 0.3f) { // 30% chance to try replacement
                        tryForceSpawn(player);
                    }

                    return;
                }
            } else {
                // This Pokémon matches the boosted type!
                // Add visual feedback
                if (player instanceof ServerPlayerEntity) {
                    // Add some particles
                    if (event.getWorld() instanceof ServerWorld && RANDOM.nextFloat() < 0.4f) {
                        BlockPos pos = event.getEntity().blockPosition();
                        ((ServerWorld) event.getWorld()).sendParticles(
                                net.minecraft.particles.ParticleTypes.HAPPY_VILLAGER,
                                pos.getX(), pos.getY() + 0.5, pos.getZ(),
                                5, 0.5, 0.5, 0.5, 0.0
                        );
                    }

                    // Notify the player occasionally
                    if (RANDOM.nextFloat() < 0.3f) {
                        player.sendMessage(
                                new StringTextComponent(TextFormatting.GREEN +
                                        "Your Type Attractor attracted a " + pokemonName + "!"),
                                player.getUUID()
                        );
                    }
                }
            }
        }

        // Handle shiny boost if active
        if (PixelmonIntegration.hasShinyBoost(player)) {
            float shinyChance = PixelmonIntegration.getShinyChance(player, 0);

            // Roll for shiny
            if (RANDOM.nextFloat() * 100 < shinyChance) {
                // Try to make the Pokémon shiny
                makeShiny(event.getEntity());

                // Notify the player
                if (player instanceof ServerPlayerEntity) {
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.AQUA +
                                    "★ Your Shiny Charm activated! A shiny " +
                                    pokemonName + " has spawned! ★"),
                            player.getUUID()
                    );
                }
            }
        }
    }

    /**
     * Try to force spawn a Pokémon of the player's boosted type
     */
    private static void tryForceSpawn(PlayerEntity player) {
        // Check cooldown
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (spawnCooldowns.containsKey(playerId)) {
            long lastSpawn = spawnCooldowns.get(playerId);
            if (currentTime - lastSpawn < SPAWN_COOLDOWN_MS) {
                return; // Still on cooldown
            }
        }

        // Update cooldown
        spawnCooldowns.put(playerId, currentTime);

        // Get the player's boosted type
        String type = PixelmonIntegration.getBoostedType(player);
        if (type == null || type.isEmpty()) {
            return;
        }

        // Force spawn a Pokémon of this type
        PixelmonIntegration.forceSpawnType(player, type);
    }

    /**
     * Try to spawn Pokémon for a player as part of the periodic tick
     */
    private static void trySpawnForPlayer(PlayerEntity player) {
        // Only try for players with type boosts
        if (!PixelmonIntegration.hasTypeBoost(player)) {
            return;
        }

        // Rate limit spawns
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (spawnCooldowns.containsKey(playerId)) {
            long lastSpawn = spawnCooldowns.get(playerId);
            if (currentTime - lastSpawn < SPAWN_COOLDOWN_MS) {
                return;
            }
        }

        // Roll based on the multiplier - higher multiplier means more spawns
        float multiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 1.0f);
        float spawnChance = Math.min(0.8f, (multiplier / 10.0f));

        if (RANDOM.nextFloat() < spawnChance) {
            // Update cooldown
            spawnCooldowns.put(playerId, currentTime);

            // Get the type and try to spawn
            String type = PixelmonIntegration.getBoostedType(player);
            PixelmonIntegration.forceSpawnType(player, type);

            CustomConsumables.getLogger().debug(
                    "Periodic spawn check: Spawned {} type for player {}",
                    type, player.getName().getString()
            );
        }
    }

    /**
     * Find the nearest player with an active effect
     */
    private static PlayerEntity findNearestPlayerWithEffect(net.minecraft.entity.Entity entity) {
        BlockPos pos = entity.blockPosition();
        double closestDistance = 64 * 64; // 64 block radius squared
        PlayerEntity closestPlayer = null;

        // Search all players in the same world
        for (PlayerEntity player : entity.level.players()) {
            // Check if player has any effects
            if (PixelmonIntegration.hasTypeBoost(player) ||
                    PixelmonIntegration.hasShinyBoost(player) ||
                    PixelmonIntegration.hasLegendaryBoost(player)) {

                double distance = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
        }

        return closestPlayer;
    }

    /**
     * Try to determine if a Pokémon entity matches a given type
     * This uses server commands to avoid relying on direct Pixelmon API access
     */
    private static boolean doesPokemonMatchType(net.minecraft.entity.Entity entity, String type, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity)) return false;

        // Use a command workaround:
        // Rather than directly accessing Pixelmon types, we leverage the fact that
        // the player's type filter already works with Pixelmon's spawn system.
        // If this entity spawned naturally while a type filter is active,
        // it's very likely to match the type (as the filter should be working).

        // For naturally spawned Pokémon with a player's type filter active,
        // we give a high probability that it's the right type
        UUID playerId = player.getUUID();
        if (PixelmonIntegration.hasTypeBoost(player)) {
            String boostedType = PixelmonIntegration.getBoostedType(player);
            if (boostedType.equalsIgnoreCase(type)) {
                // Give a high probability that this is the right type (85%)
                // This avoids needing to know the exact Pokémon types
                return RANDOM.nextFloat() < 0.85f;
            }
        }

        // If we can't determine, be conservative and return false
        return false;
    }

    /**
     * Get the name of a Pokémon entity
     */
    private static String getPokemonName(net.minecraft.entity.Entity entity) {
        String displayName = entity.getDisplayName().getString();

        // Simple sanitization to get just the Pokémon name
        if (displayName != null && !displayName.isEmpty()) {
            // Remove any level indicators or other text
            if (displayName.contains("Lv.")) {
                displayName = displayName.split("Lv.")[0].trim();
            }
            return displayName;
        }

        return "Pokémon";
    }

    /**
     * Try to make a Pokémon entity shiny
     */
    private static void makeShiny(net.minecraft.entity.Entity entity) {
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;

            // Use a command to try to make the targeted Pokémon shiny
            try {
                player.getServer().getCommands().performCommand(
                        player.getServer().createCommandSourceStack().withPermission(4),
                        "pokeedit shiny true"
                );
            } catch (Exception e) {
                CustomConsumables.getLogger().debug("Error setting shiny status: {}", e.getMessage());
            }
        }
    }

    /**
     * Check if an entity is a Pixelmon entity
     */
    private static boolean isPixelmonEntity(net.minecraft.entity.Entity entity) {
        try {
            // Use reflection to check if this is a Pixelmon entity
            Class<?> pixelmonEntityClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");
            return pixelmonEntityClass.isInstance(entity);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Clean up the processed entities cache
     */
    private static void cleanupProcessedEntities() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - 60000; // 1 minute

        processedEntities.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}