package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles Pixelmon spawns by listening to EntityJoinWorldEvent
 * and checking if the entity is a Pixelmon.
 */
@Mod.EventBusSubscriber(modid = "customconsumables")
public class PixelmonSpawnListener {

    private static final Random RANDOM = new Random();

    // Cache for reflection lookups
    private static boolean initialized = false;
    private static Class<?> pixelmonClass;
    private static Method getPokemonMethod;
    private static Method getSpeciesMethod;
    private static Method getTypesMethod;
    private static Method setShinyMethod;
    private static Method isLegendaryMethod;

    // Cache to avoid checking entities we've already checked
    private static final ConcurrentHashMap<Integer, Boolean> checkedEntities = new ConcurrentHashMap<>();

    /**
     * Initialize reflection for Pixelmon
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Get Pixelmon entity class
            pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

            // Get methods
            getPokemonMethod = pixelmonClass.getMethod("getPokemon");

            // Get Pokemon class methods
            Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
            getSpeciesMethod = pokemonClass.getMethod("getSpecies");
            setShinyMethod = pokemonClass.getMethod("setShiny", boolean.class);

            // Get Species class methods
            Class<?> speciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");
            getTypesMethod = speciesClass.getMethod("getTypes");
            isLegendaryMethod = speciesClass.getMethod("isLegendary");

            initialized = true;
            CustomConsumables.getLogger().info("PixelmonSpawnListener initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize PixelmonSpawnListener: {}", e.getMessage());
        }
    }

    /**
     * Listen for entity spawn events
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide()) return;

        if (!initialized) {
            initialize();
        }

        if (!initialized) return;

        try {
            // Check if this is a Pixelmon entity
            if (!pixelmonClass.isInstance(event.getEntity())) {
                return;
            }

            // Check if we've already processed this entity
            int entityId = event.getEntity().getId();
            if (checkedEntities.containsKey(entityId)) {
                return;
            }

            // Mark as checked
            checkedEntities.put(entityId, true);

            // Clean up cache if it gets too large
            if (checkedEntities.size() > 1000) {
                checkedEntities.clear();
            }

            // Get the Pokemon object
            Object pixelmonEntity = event.getEntity();
            Object pokemon = getPokemonMethod.invoke(pixelmonEntity);

            if (pokemon == null) return;

            // Get the species
            Object species = getSpeciesMethod.invoke(pokemon);

            if (species == null) return;

            // Find nearest player with an effect
            PlayerEntity nearestPlayer = findNearestPlayerWithEffect(event.getEntity());

            if (nearestPlayer == null) return;

            // Process effects
            processEffects(nearestPlayer, pokemon, species, pixelmonEntity);

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error in PixelmonSpawnListener: {}", e.getMessage());
        }
    }

    /**
     * Find the nearest player with an active effect
     */
    private static PlayerEntity findNearestPlayerWithEffect(net.minecraft.entity.Entity entity) {
        BlockPos pos = entity.blockPosition();

        PlayerEntity closestPlayer = null;
        double closestDistance = 64 * 64; // 64 block range squared

        // Search all players in same world
        for (PlayerEntity player : entity.level.players()) {
            // Check if player has any effects
            if (PlayerEffectManager.hasLegendaryLureEffect(player) ||
                    PlayerEffectManager.hasShinyBoostEffect(player) ||
                    PlayerEffectManager.hasTypeAttractorEffect(player)) {

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
     * Process effects for a spawn
     */
    private static void processEffects(PlayerEntity player, Object pokemon, Object species, Object pixelmonEntity) {
        try {
            // Check for shiny effect
            if (PlayerEffectManager.hasShinyBoostEffect(player)) {
                applyShinyEffect(player, pokemon);
            }

            // Check for type effect - this can cancel the spawn
            if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
                if (!shouldAllowTypeSpawn(player, species)) {
                    // This is a Pokémon of a different type - remove it
                    if (pixelmonEntity instanceof net.minecraft.entity.Entity) {
                        ((net.minecraft.entity.Entity) pixelmonEntity).remove();

                        // Notify player (with rate limiting)
                        if (player instanceof ServerPlayerEntity && RANDOM.nextFloat() < 0.1f) {
                            ((ServerPlayerEntity) player).sendMessage(
                                    new StringTextComponent(TextFormatting.RED + "A Pokémon of the wrong type tried to spawn! It was blocked."),
                                    player.getUUID()
                            );
                        }

                        // Try to force spawn a Pokémon of the correct type
                        if (RANDOM.nextFloat() < 0.3f) {
                            forceCorrectTypeSpawn(player);
                        }
                    }
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error processing effects: {}", e.getMessage());
        }
    }

    /**
     * Apply shiny effect to a spawn
     */
    private static void applyShinyEffect(PlayerEntity player, Object pokemon) {
        try {
            // Check if should be shiny based on player effect
            if (PlayerEffectManager.shouldBeShiny(player)) {
                // Make the Pokemon shiny
                setShinyMethod.invoke(pokemon, true);

                // Notify player
                if (player instanceof ServerPlayerEntity) {
                    ((ServerPlayerEntity) player).sendMessage(
                            new StringTextComponent(TextFormatting.AQUA + "Your Shiny Charm activated! A shiny Pokémon has spawned near you!"),
                            player.getUUID()
                    );
                }

                // Log success
                CustomConsumables.getLogger().info(
                        "Applied shiny effect for player {}",
                        player.getName().getString()
                );
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error applying shiny effect: {}", e.getMessage());
        }
    }

    /**
     * Check if a spawn should be allowed based on type
     */
    private static boolean shouldAllowTypeSpawn(PlayerEntity player, Object species) {
        try {
            // Get the type filter
            String typeFilter = PlayerEffectManager.getTypeAttractorType(player);
            if (typeFilter == null || typeFilter.isEmpty()) {
                return true; // No filter, allow spawn
            }

            // Get the Pokemon's types
            Object[] types = (Object[]) getTypesMethod.invoke(species);
            if (types == null || types.length == 0) {
                return true; // No types, allow spawn
            }

            // Check if any type matches
            for (Object type : types) {
                if (type.toString().equalsIgnoreCase(typeFilter)) {
                    // Type matches, definitely allow
                    return true;
                }
            }

            // Type doesn't match - decide based on probability
            float multiplier = PlayerEffectManager.getTypeAttractorChance(player, 0) / 100.0f;
            float roll = RANDOM.nextFloat();
            float blockChance = Math.min(0.9f, 1.0f - (1.0f / multiplier));

            // Allow based on probability - higher multiplier means higher chance to block
            return roll >= blockChance;

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error checking type spawn: {}", e.getMessage());
            return true; // Allow on error
        }
    }

    /**
     * Force spawn a Pokémon of the correct type
     */
    private static void forceCorrectTypeSpawn(PlayerEntity player) {
        try {
            // Get player's desired type
            String type = PlayerEffectManager.getTypeAttractorType(player);
            if (type == null || type.isEmpty()) return;

            // Get server
            if (!(player instanceof ServerPlayerEntity)) return;
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Try to spawn using command
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn " + type.toLowerCase()
            );

            // Log the attempt
            CustomConsumables.getLogger().debug(
                    "Forced spawn of type {} for player {}",
                    type, player.getName().getString()
            );

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error forcing type spawn: {}", e.getMessage());
        }
    }
}