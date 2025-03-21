package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles type filtering for Pixelmon spawns
 * Uses event reflection to avoid direct dependencies on Pixelmon
 */
@Mod.EventBusSubscriber(modid = "customconsumables")
public class PixelmonTypeFilter {
    // Reflection cache for Pixelmon classes and methods
    private static boolean initialized = false;
    private static Class<?> spawnEvent = null;
    private static Class<?> pokemonSpeciesClass = null;
    private static Class<?> pokemonTypeClass = null;
    private static Method getSpeciesMethod = null;
    private static Method hasTypeMethod = null;
    private static Method getTypeEnumMethod = null;

    // Cache of player UUID to type filter
    private static final Map<UUID, String> playerTypeFilters = new HashMap<>();

    /**
     * Initialize reflection hooks for Pixelmon
     */
    public static void initialize() {
        if (initialized) return;

        try {
            // Load Pixelmon classes through reflection
            spawnEvent = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent");
            pokemonSpeciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");
            pokemonTypeClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Element");

            // Get methods through reflection
            getSpeciesMethod = spawnEvent.getMethod("getSpecies");
            hasTypeMethod = pokemonSpeciesClass.getMethod("hasType", pokemonTypeClass);
            getTypeEnumMethod = pokemonTypeClass.getMethod("valueOf", String.class);

            initialized = true;
            CustomConsumables.getLogger().info("PixelmonTypeFilter initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize PixelmonTypeFilter", e);
        }
    }

    /**
     * Event handler for Pixelmon's SpawnEvent
     * This won't directly subscribe but will be called through a "proxy" event
     */
    public static boolean onPokemonSpawn(Object event, PlayerEntity player) {
        // Initialize if not done already
        if (!initialized) {
            initialize();
            if (!initialized) return true; // Allow spawn if we failed to initialize
        }

        try {
            // Check if player has an active type filter
            if (!hasTypeFilter(player)) {
                return true; // Allow spawn if no filter
            }

            // Get the filter type
            String typeFilter = getTypeFilter(player);
            if (typeFilter.isEmpty()) {
                return true; // Allow spawn if filter is empty
            }

            // Convert type string to Pixelmon's Element enum
            Object typeEnum = getTypeEnumMethod.invoke(null, typeFilter.toUpperCase());
            if (typeEnum == null) {
                return true; // Allow spawn if type is invalid
            }

            // Get the species from the event
            Object species = getSpeciesMethod.invoke(event);
            if (species == null) {
                return true; // Allow spawn if species is null
            }

            // Check if the species has the filtered type
            Boolean hasType = (Boolean) hasTypeMethod.invoke(species, typeEnum);
            return hasType; // Only allow spawns of Pokémon with the filtered type

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error in onPokemonSpawn", e);
            return true; // Allow spawn on error
        }
    }

    /**
     * Register a player's type filter
     */
    public static void registerPlayerTypeFilter(PlayerEntity player, String type) {
        playerTypeFilters.put(player.getUUID(), type.toLowerCase());
    }

    /**
     * Remove a player's type filter
     */
    public static void removePlayerTypeFilter(PlayerEntity player) {
        playerTypeFilters.remove(player.getUUID());
    }

    /**
     * Get a player's type filter
     */
    public static String getPlayerTypeFilter(PlayerEntity player) {
        return playerTypeFilters.getOrDefault(player.getUUID(), "");
    }

    /**
     * Check if a Pokémon type matches a player's filter
     * This is called through reflection from PixelmonTypeFilterHook
     */
    public static boolean checkTypeMatch(PlayerEntity player, String pokemonType1, String pokemonType2) {
        if (!hasTypeFilter(player)) {
            return true; // No filter, allow spawn
        }

        String typeFilter = getTypeFilter(player).toLowerCase();
        if (typeFilter.isEmpty()) {
            return true; // Empty filter, allow spawn
        }

        // Check if either type matches the filter
        return pokemonType1.toLowerCase().equals(typeFilter) ||
                pokemonType2.toLowerCase().equals(typeFilter);
    }

    /**
     * Check if a player has an active type filter
     */
    public static boolean hasTypeFilter(PlayerEntity player) {
        if (player == null) {
            return false;
        }

        // First check our cache
        UUID playerUUID = player.getUUID();
        if (playerTypeFilters.containsKey(playerUUID)) {
            return !playerTypeFilters.get(playerUUID).isEmpty();
        }

        // Then check player effect manager
        if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
            String type = PlayerEffectManager.getTypeAttractorType(player);
            if (type != null && !type.isEmpty()) {
                // Cache for future use
                playerTypeFilters.put(playerUUID, type);
                return true;
            }
        }

        return false;
    }

    /**
     * Get the active type filter for a player
     */
    public static String getTypeFilter(PlayerEntity player) {
        if (player == null) {
            return "";
        }

        // First check our cache
        UUID playerUUID = player.getUUID();
        if (playerTypeFilters.containsKey(playerUUID)) {
            return playerTypeFilters.get(playerUUID);
        }

        // Then check player effect manager
        if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
            String type = PlayerEffectManager.getTypeAttractorType(player);
            if (type != null && !type.isEmpty()) {
                // Cache for future use
                playerTypeFilters.put(playerUUID, type);
                return type;
            }
        }

        return "";
    }
}