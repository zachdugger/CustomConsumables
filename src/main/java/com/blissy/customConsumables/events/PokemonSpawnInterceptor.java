package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * This class directly intercepts Pixelmon spawn events through reflection
 * to enforce type attractor effects at a lower level in the spawning system.
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PokemonSpawnInterceptor {
    // Random number generator for chance calculations
    private static final Random random = new Random();

    // Cache for reflection lookups
    private static boolean initialized = false;
    private static Class<?> spawnEventClass = null;
    private static Class<?> pixelmonSpeciesClass = null;
    private static Method getSpeciesMethod = null;
    private static Method getTypesMethod = null;

    // Tracking variables for spawn attempts
    private static final Map<UUID, Long> lastForceSpawnTime = new HashMap<>();
    private static final long FORCE_SPAWN_COOLDOWN = 5000; // 5 seconds

    /**
     * Initialize reflection hooks to access Pixelmon's internals
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Try to load Pixelmon classes
            spawnEventClass = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent");
            pixelmonSpeciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");

            // Get the methods we need
            getSpeciesMethod = spawnEventClass.getMethod("getSpecies");
            getTypesMethod = pixelmonSpeciesClass.getMethod("getTypes");

            initialized = true;
            CustomConsumables.getLogger().info("PokemonSpawnInterceptor initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize PokemonSpawnInterceptor", e);
        }
    }

    /**
     * Hook method that gets called from within Pixelmon's SpawnEvent
     * This will be called by a proxy event or transformer
     */
    public static boolean onPokemonSpawnDecision(Object event, PlayerEntity player) {
        if (!PixelmonIntegration.isPixelmonLoaded() || player == null) {
            return true; // Allow spawn by default
        }

        // Initialize if needed
        if (!initialized) {
            initialize();
            if (!initialized) return true; // Allow spawn if initialization failed
        }

        try {
            // Check if player has an active type boost
            if (!PixelmonIntegration.hasTypeBoost(player)) {
                return true; // No type effect, allow spawn
            }

            // Get the boosted type
            String boostedType = PixelmonIntegration.getBoostedType(player);
            if (boostedType == null || boostedType.isEmpty()) {
                return true; // No specific type boost, allow spawn
            }

            // Get the Pokémon species from the event
            Object species = getSpeciesMethod.invoke(event);
            if (species == null) {
                return true; // No species, allow spawn
            }

            // Get the types of the Pokémon
            Object[] types = (Object[]) getTypesMethod.invoke(species);
            if (types == null || types.length == 0) {
                return true; // No types, allow spawn
            }

            // Check if any of the Pokémon's types match the boosted type
            boolean typeMatches = false;
            for (Object type : types) {
                if (type.toString().equalsIgnoreCase(boostedType)) {
                    typeMatches = true;
                    break;
                }
            }

            // Always allow spawns of the boosted type
            if (typeMatches) {
                CustomConsumables.getLogger().debug(
                        "Allowing spawn of {} type Pokémon for player {}",
                        boostedType, player.getName().getString()
                );
                return true;
            }

            // For other types, decide based on multiplier
            float multiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 1.0f);
            if (multiplier >= 5.0f) {
                // With high multiplier, block most non-matching types
                float roll = random.nextFloat();
                boolean allowSpawn = roll > 0.85f; // Only allow 15% of non-matching types

                if (!allowSpawn) {
                    // Since we're blocking this spawn, try to force a spawn of the correct type
                    // but with a cooldown to prevent spamming
                    UUID playerId = player.getUUID();
                    long currentTime = System.currentTimeMillis();

                    if (!lastForceSpawnTime.containsKey(playerId) ||
                            (currentTime - lastForceSpawnTime.get(playerId)) > FORCE_SPAWN_COOLDOWN) {

                        // Attempt to force spawn a Pokémon of the correct type
                        tryForceSpawn(player, boostedType);

                        // Update cooldown
                        lastForceSpawnTime.put(playerId, currentTime);
                    }
                }

                return allowSpawn;
            }

            // For lower multipliers, use inverse chance
            float roll = random.nextFloat();
            boolean allowSpawn = roll <= (1.0f / multiplier);

            return allowSpawn;

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error in onPokemonSpawnDecision", e);
            return true; // Allow spawn on error
        }
    }

    /**
     * Try to force spawn a Pokémon of the specified type
     */
    private static void tryForceSpawn(PlayerEntity player, String type) {
        if (!(player instanceof ServerPlayerEntity)) return;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        MinecraftServer server = serverPlayer.getServer();

        if (server != null) {
            try {
                // Execute the command to spawn a Pokémon of the specified type
                server.getCommands().performCommand(
                        server.createCommandSourceStack().withPermission(4),
                        "pokespawn " + type.toLowerCase()
                );

                CustomConsumables.getLogger().debug(
                        "Forced spawn of {} type Pokémon for player {}",
                        type, player.getName().getString()
                );

                // Add visual feedback
                ServerWorld world = serverPlayer.getLevel();
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.WITCH,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        5, 0.5, 0.5, 0.5, 0.05
                );
            } catch (Exception e) {
                // Just log at debug level
                CustomConsumables.getLogger().debug(
                        "Failed to force spawn {} type: {}",
                        type, e.getMessage()
                );
            }
        }
    }

    /**
     * Ticking handler to ensure our type boost logic runs
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != LogicalSide.SERVER) {
            return;
        }

        // Every 100 ticks (5 seconds), check for players with active type boosts
        if (event.world.getGameTime() % 100 == 0) {
            if (!PixelmonIntegration.isPixelmonLoaded()) return;

            // Process all players with type boosts
            for (PlayerEntity player : event.world.players()) {
                if (PixelmonIntegration.hasTypeBoost(player)) {
                    String type = PixelmonIntegration.getBoostedType(player);
                    if (!type.isEmpty()) {
                        // If more than 30 seconds left on the boost, try to force spawns
                        int timeLeft = PixelmonIntegration.getTypeBoostDuration(player);
                        if (timeLeft > 30 * 20) {
                            tryForceSpawn(player, type);
                        }
                    }
                }
            }
        }
    }
}