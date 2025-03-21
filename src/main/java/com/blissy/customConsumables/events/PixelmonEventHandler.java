package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * This class handles events from Pixelmon to implement our custom functionality.
 * Uses reflection to avoid direct dependencies on Pixelmon classes.
 */
@Mod.EventBusSubscriber(modid = "customconsumables")
public class PixelmonEventHandler {

    private static boolean initialized = false;
    private static Class<?> spawnEventClass;
    private static Class<?> pokemonClass;
    private static Class<?> pokemonSpeciesClass;
    private static Method getEntityMethod;
    private static Method getPokemonMethod;
    private static Method getSpeciesMethod;
    private static Method getTypesMethod;
    private static Method setShinyMethod;
    private static Method isLegendaryMethod;

    private static final Random random = new Random();

    // Cache for player notifications to prevent spam
    private static final Map<UUID, Long> notificationCache = new HashMap<>();

    /**
     * Initialize reflection to access Pixelmon classes and methods
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Load classes
            spawnEventClass = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent");
            pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
            pokemonSpeciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");

            // Get methods
            getEntityMethod = spawnEventClass.getMethod("getEntity");
            getPokemonMethod = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity").getMethod("getPokemon");
            getSpeciesMethod = pokemonClass.getMethod("getSpecies");
            getTypesMethod = pokemonSpeciesClass.getMethod("getTypes");
            setShinyMethod = pokemonClass.getMethod("setShiny", boolean.class);
            isLegendaryMethod = pokemonSpeciesClass.getMethod("isLegendary");

            initialized = true;
            CustomConsumables.getLogger().info("PixelmonEventHandler initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize PixelmonEventHandler: {}", e.getMessage());
        }
    }

    /**
     * Handler for Pixelmon's SpawnEvent
     * This method is called via reflection from the ASM transformer
     */
    public static void onPixelmonSpawn(Object event) {
        if (!initialized) {
            initialize();
        }

        if (!initialized || event == null) return;

        try {
            // Make sure this is a Pixelmon SpawnEvent
            if (!spawnEventClass.isInstance(event)) return;

            // Get the entity being spawned
            Object pixelmonEntity = getEntityMethod.invoke(event);
            if (pixelmonEntity == null) return;

            // Get the Pokemon object
            Object pokemon = getPokemonMethod.invoke(pixelmonEntity);
            if (pokemon == null) return;

            // Get the species
            Object species = getSpeciesMethod.invoke(pokemon);
            if (species == null) return;

            // Find the nearest player
            PlayerEntity nearestPlayer = findNearestPlayerWithEffect(pixelmonEntity);
            if (nearestPlayer == null) return;

            // Check legendary effect
            if (PlayerEffectManager.hasLegendaryLureEffect(nearestPlayer)) {
                applyLegendaryEffect(nearestPlayer, pokemon, species);
            }

            // Check shiny effect
            if (PlayerEffectManager.hasShinyBoostEffect(nearestPlayer)) {
                applyShinyEffect(nearestPlayer, pokemon);
            }

            // Check type filter effect
            if (PlayerEffectManager.hasTypeAttractorEffect(nearestPlayer)) {
                if (!applyTypeEffect(nearestPlayer, pokemon, species)) {
                    // Cancel spawn if type doesn't match
                    suppressSpawn(event);
                }
            }

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error in PixelmonEventHandler: {}", e.getMessage());
        }
    }

    /**
     * Find the nearest player with an active effect
     */
    private static PlayerEntity findNearestPlayerWithEffect(Object entity) {
        try {
            // Get position from entity
            net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) entity;
            BlockPos pos = mcEntity.blockPosition();

            PlayerEntity closestPlayer = null;
            double closestDistance = 64 * 64; // 64 block range squared

            // Search all players in same world
            for (PlayerEntity player : mcEntity.level.players()) {
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
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error finding nearest player: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Apply legendary effect to a spawn
     */
    private static void applyLegendaryEffect(PlayerEntity player, Object pokemon, Object species) {
        try {
            // If it's already a legendary, no need to do anything
            Boolean isLegendary = (Boolean) isLegendaryMethod.invoke(species);
            if (isLegendary) return;

            // Get the legendary chance from the player's effect
            float chance = PlayerEffectManager.getLegendaryLureChance(player, 1.0f);

            // Roll for legendary spawn
            if (random.nextFloat() * 100f <= chance) {
                // TODO: Can't directly make a Pokémon legendary, but we could:
                // 1. Cancel this spawn and spawn a legendary instead
                // 2. Notify the player that their lure has triggered

                notifyPlayer(player, TextFormatting.GOLD + "Your Legendary Lure has activated! A legendary Pokémon will spawn soon!");

                // Try to spawn a legendary using command
                if (player instanceof ServerPlayerEntity) {
                    player.getServer().getCommands().performCommand(
                            player.getServer().createCommandSourceStack().withPermission(4),
                            "pokespawn legendary near " + player.getName().getString()
                    );
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error applying legendary effect: {}", e.getMessage());
        }
    }

    /**
     * Apply shiny effect to a spawn
     */
    private static void applyShinyEffect(PlayerEntity player, Object pokemon) {
        try {
            // Check if should be shiny
            if (PlayerEffectManager.shouldBeShiny(player)) {
                // Make the Pokémon shiny
                setShinyMethod.invoke(pokemon, true);

                // Notify player
                notifyPlayer(player, TextFormatting.AQUA + "Your Shiny Charm activated! A shiny Pokémon has spawned!");
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error applying shiny effect: {}", e.getMessage());
        }
    }

    /**
     * Apply type effect to a spawn
     * @return true if spawn should be allowed, false to cancel
     */
    private static boolean applyTypeEffect(PlayerEntity player, Object pokemon, Object species) {
        try {
            // Get the type the player is filtering for
            String typeFilter = PlayerEffectManager.getTypeAttractorType(player);
            if (typeFilter == null || typeFilter.isEmpty()) {
                return true; // No filter, allow spawn
            }

            // Get this Pokémon's types
            Object[] types = (Object[]) getTypesMethod.invoke(species);
            if (types == null || types.length == 0) {
                return true; // No types, allow spawn
            }

            // Check if any type matches
            for (Object type : types) {
                if (type.toString().equalsIgnoreCase(typeFilter)) {
                    return true; // Type matches, allow spawn
                }
            }

            // Get multiplier to determine if we should block this spawn
            float multiplier = PlayerEffectManager.getTypeAttractorChance(player, 0) / 100.0f;
            float roll = random.nextFloat();
            float blockChance = Math.min(0.9f, 1.0f - (1.0f / multiplier));

            return roll >= blockChance; // Only block based on probability

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error applying type effect: {}", e.getMessage());
            return true; // Allow spawn on error
        }
    }

    /**
     * Suppress this spawn
     */
    private static void suppressSpawn(Object event) {
        try {
            // Use reflection to call setCanceled(true)
            Method setCanceledMethod = event.getClass().getMethod("setCanceled", boolean.class);
            setCanceledMethod.invoke(event, true);
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error suppressing spawn: {}", e.getMessage());
        }
    }

    /**
     * Notify a player with a message, with rate limiting
     */
    private static void notifyPlayer(PlayerEntity player, String message) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Check if we've recently sent a notification
        if (notificationCache.containsKey(playerId)) {
            long lastNotification = notificationCache.get(playerId);
            if (currentTime - lastNotification < 5000) { // 5 second cooldown
                return;
            }
        }

        // Send message and update cache
        player.sendMessage(new StringTextComponent(message), player.getUUID());
        notificationCache.put(playerId, currentTime);
    }

    /**
     * Manual hook method that can be called by ASM transformer
     */
    public static void onPixelmonSpawnHook(Object event) {
        onPixelmonSpawn(event);
    }
}