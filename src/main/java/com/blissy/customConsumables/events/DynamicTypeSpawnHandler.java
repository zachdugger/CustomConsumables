package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class dynamically handles Pokémon spawns to boost the chances of specific types
 * when a player has an active Type Attractor effect.
 *
 * It works by:
 * 1. Loading and monitoring the PokemonTypeDataHandler for type information
 * 2. Intercepting Pixelmon spawns to determine their type
 * 3. Allowing or blocking spawns based on the player's active type boost
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class DynamicTypeSpawnHandler {
    // Reflection cache for accessing Pixelmon internals
    private static boolean initialized = false;
    private static Class<?> pixelmonEntityClass = null;
    private static Method getPokemonMethod = null;
    private static Method getSpeciesMethod = null;
    private static Method getTypesMethod = null;

    // Reference to the type data handler
    private static PokemonTypeDataHandler typeDataHandler = null;

    // Random for chance calculations
    private static final Random random = new Random();

    /**
     * Initialize reflection for accessing Pixelmon classes
     */
    public static void initialize() {
        if (initialized) return;

        try {
            // Load type data handler
            typeDataHandler = PokemonTypeDataHandler.getInstance();
            typeDataHandler.initialize();

            // Try to get Pixelmon classes via reflection
            pixelmonEntityClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

            // Get the method to retrieve the Pokémon from the entity
            getPokemonMethod = pixelmonEntityClass.getMethod("getPokemon");

            // Get the method to retrieve the Species
            Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
            getSpeciesMethod = pokemonClass.getMethod("getSpecies");

            // Get the method to retrieve types
            Class<?> speciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");
            getTypesMethod = speciesClass.getMethod("getTypes");

            initialized = true;
            CustomConsumables.getLogger().info("DynamicTypeSpawnHandler initialized successfully");
        }
        catch (ClassNotFoundException e) {
            CustomConsumables.getLogger().info("Pixelmon classes not found, spawn handling disabled: {}", e.getMessage());
        }
        catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize DynamicTypeSpawnHandler: {}", e.getMessage());
        }
    }

    /**
     * This event handler intercepts entities as they join the world
     * and filters Pokémon spawns based on the type attractor effect
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        // Skip client side
        if (event.getWorld().isClientSide()) return;

        // Initialize if needed
        if (!initialized) {
            initialize();
            if (!initialized) return;
        }

        try {
            // Check if this is a Pixelmon entity
            if (!pixelmonEntityClass.isInstance(event.getEntity())) {
                return;
            }

            // Get the nearest player with a type attractor effect
            PlayerEntity activePlayer = null;
            String boostedType = null;

            // Check all players within range
            for (PlayerEntity player : event.getWorld().players()) {
                if (PixelmonIntegration.hasTypeBoost(player)) {
                    // Found a player with a type boost
                    activePlayer = player;
                    boostedType = PixelmonIntegration.getBoostedType(player).toUpperCase();
                    break;
                }
            }

            // If no player has a type boost, allow the spawn
            if (activePlayer == null || boostedType == null || boostedType.isEmpty()) {
                return;
            }

            // Get the Pixelmon entity
            Object pixelmon = event.getEntity();

            // Get the Pokémon object
            Object pokemon = getPokemonMethod.invoke(pixelmon);

            // Get the species
            Object species = getSpeciesMethod.invoke(pokemon);

            // Get the types
            Object[] types = (Object[]) getTypesMethod.invoke(species);

            // Check if any of the Pokémon's types match the boosted type
            boolean typeMatches = false;
            for (Object typeObj : types) {
                String pokemonType = typeObj.toString();
                if (pokemonType.equalsIgnoreCase(boostedType)) {
                    typeMatches = true;
                    break;
                }
            }

            // If type matches, allow the spawn
            if (typeMatches) {
                // This is a Pokémon of the boosted type, always allow it
                return;
            }

            // For non-matching types, get the multiplier
            float multiplier = PixelmonIntegration.getTypeBoostMultiplier(activePlayer, 1.0f);

            // For high multipliers (5x+), block most non-matching types
            if (multiplier >= 5.0f) {
                float roll = random.nextFloat();
                boolean shouldBlock = roll <= 0.85f; // Block 85% of non-matching types

                if (shouldBlock) {
                    // Cancel the spawn
                    event.setCanceled(true);

                    // Log at debug level
                    CustomConsumables.getLogger().debug(
                            "Blocked non-{} type Pokémon spawn near player {} with Type Attractor",
                            boostedType, activePlayer.getName().getString()
                    );
                }
            }
            else {
                // For lower multipliers, use a less aggressive filtering approach
                // Block non-matching types based on the inverse of the multiplier
                float roll = random.nextFloat();
                boolean shouldBlock = roll <= (1.0f - (1.0f / multiplier));

                if (shouldBlock) {
                    event.setCanceled(true);
                }
            }
        }
        catch (Exception e) {
            // Just log at debug level and continue
            CustomConsumables.getLogger().debug("Error in Pokémon spawn filter: {}", e.getMessage());
        }
    }

    /**
     * Tick event handler to update type boost effects
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only run on server side and at the end phase
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        PlayerEntity player = event.player;

        // Every 100 ticks (5 seconds), if we have an active effect
        // Try to force a spawn of a Pokémon with the correct type
        if (player.tickCount % 100 == 0 && player instanceof ServerPlayerEntity) {
            if (PixelmonIntegration.hasTypeBoost(player)) {
                // Make sure we're initialized
                if (!initialized) {
                    initialize();
                }

                // Attempt a forced spawn (this uses Pixelmon's normal spawning rules)
                tryForceTypeSpawn((ServerPlayerEntity) player);
            }
        }
    }

    /**
     * Try to force a spawn of a Pokémon with the correct type
     */
    private static void tryForceTypeSpawn(ServerPlayerEntity player) {
        try {
            // Get the type being boosted
            String boostedType = PixelmonIntegration.getBoostedType(player);
            if (boostedType.isEmpty()) return;

            // Execute the command to try to spawn a Pokémon
            if (player.getServer() != null) {
                // The 'pokespawnall' command spawns using Pixelmon's normal spawn rules
                // but with increased chance, which is what we want
                player.getServer().getCommands().performCommand(
                        player.getServer().createCommandSourceStack().withPermission(4),
                        "pokespawnall"
                );

                // Add some subtle particles so the player knows something happened
                if (random.nextFloat() < 0.3f) { // Only 30% of the time to avoid spam
                    player.getLevel().sendParticles(
                            net.minecraft.particles.ParticleTypes.HAPPY_VILLAGER,
                            player.getX(), player.getY() + 0.5, player.getZ(),
                            5, 0.5, 0.5, 0.5, 0.05
                    );
                }
            }
        }
        catch (Exception e) {
            CustomConsumables.getLogger().debug("Error forcing type spawn: {}", e.getMessage());
        }
    }
}