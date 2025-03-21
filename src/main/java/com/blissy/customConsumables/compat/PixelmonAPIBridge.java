package com.blissy.customConsumables.compat;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Enhanced Pixelmon API integration that uses reflection to safely access
 * Pixelmon's API without direct compile-time dependencies.
 */
public class PixelmonAPIBridge {
    private static boolean initialized = false;
    private static boolean isPixelmonLoaded = false;

    // Cache for reflection lookups
    private static Class<?> speciesClass;
    private static Class<?> speciesRegistryClass;
    private static Class<?> enums;
    private static Class<?> pokemonTypeClass;
    private static Method getSpeciesMethod;
    private static Method getSpeciesByNameMethod;
    private static Method getTypesMethod;
    private static Method isLegendaryMethod;
    private static Method getCommandSpawnableSpec;
    private static Method getBaseStatsMethod;
    private static Field allSpeciesField;

    // Cache for Pokemon data
    private static Map<String, Set<String>> typeToSpeciesMap = new HashMap<>();
    private static boolean dataLoaded = false;

    /**
     * Initialize the API bridge
     */
    public static void initialize() {
        if (initialized) return;

        isPixelmonLoaded = ModList.get().isLoaded("pixelmon");
        if (!isPixelmonLoaded) {
            CustomConsumables.getLogger().info("Pixelmon not found, API bridge disabled");
            return;
        }

        try {
            // Load Pixelmon classes
            speciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");
            speciesRegistryClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.SpeciesRegistry");
            enums = Class.forName("com.pixelmonmod.pixelmon.enums.EnumSpecies");
            pokemonTypeClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Element");

            // Get methods for Species
            getTypesMethod = speciesClass.getMethod("getTypes");
            isLegendaryMethod = speciesClass.getMethod("isLegendary");
            getBaseStatsMethod = speciesClass.getMethod("getBaseStats");

            // Get methods for accessing registry
            getSpeciesMethod = speciesRegistryClass.getMethod("getSpecies", String.class);
            getSpeciesByNameMethod = speciesRegistryClass.getMethod("getByName", String.class);
            allSpeciesField = enums.getDeclaredField("values");
            allSpeciesField.setAccessible(true);

            // Get method for command spawning
            getCommandSpawnableSpec = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.stats.StatsRegistry")
                    .getMethod("getCommandSpawnableSpecies");

            initialized = true;
            CustomConsumables.getLogger().info("PixelmonAPIBridge initialized successfully");

            // Preload data
            loadTypeToSpeciesMap();

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize PixelmonAPIBridge: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Preload the mapping of types to species
     */
    private static void loadTypeToSpeciesMap() {
        if (dataLoaded || !initialized) return;

        try {
            // Get all species
            CustomConsumables.getLogger().info("Loading Pixelmon species data...");

            // Try to use the command spawnable method first as it's most reliable
            Object[] commandSpawnableSpecies = null;
            try {
                commandSpawnableSpecies = (Object[]) getCommandSpawnableSpec.invoke(null);
                CustomConsumables.getLogger().info("Got {} command spawnable species",
                        commandSpawnableSpecies != null ? commandSpawnableSpecies.length : 0);
            } catch (Exception e) {
                CustomConsumables.getLogger().error("Failed to get command spawnable species: {}", e.getMessage());
            }

            // If that didn't work, try the allSpecies field
            Object[] allSpecies;
            if (commandSpawnableSpecies != null && commandSpawnableSpecies.length > 0) {
                allSpecies = commandSpawnableSpecies;
            } else {
                allSpecies = (Object[]) allSpeciesField.get(null);
                CustomConsumables.getLogger().info("Falling back to all species: {}", allSpecies.length);
            }

            // Process each species
            int count = 0;
            for (Object speciesEnum : allSpecies) {
                try {
                    // Get the Species object
                    Object species = getSpeciesByNameMethod.invoke(null, speciesEnum.toString());
                    if (species == null) continue;

                    // Get the types
                    Object[] types = (Object[]) getTypesMethod.invoke(species);
                    if (types == null || types.length == 0) continue;

                    // Add to our mapping
                    String speciesName = speciesEnum.toString().toLowerCase();
                    for (Object type : types) {
                        String typeName = type.toString().toLowerCase();
                        typeToSpeciesMap.computeIfAbsent(typeName, k -> new HashSet<>()).add(speciesName);
                    }
                    count++;
                } catch (Exception e) {
                    // Just skip problematic species
                    CustomConsumables.getLogger().debug("Error processing species {}: {}",
                            speciesEnum, e.getMessage());
                }
            }

            dataLoaded = true;
            CustomConsumables.getLogger().info("Loaded type mapping for {} Pixelmon species", count);

            // Debug output for each type
            for (Map.Entry<String, Set<String>> entry : typeToSpeciesMap.entrySet()) {
                CustomConsumables.getLogger().debug("Type {}: {} Pokémon",
                        entry.getKey(), entry.getValue().size());
            }

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to load type-to-species mapping: {}", e.getMessage());
        }
    }

    /**
     * Check if Pixelmon is loaded
     */
    public static boolean isPixelmonLoaded() {
        return isPixelmonLoaded;
    }

    /**
     * Get all Pokémon of a specific type
     */
    public static Set<String> getPokemonOfType(String type) {
        if (!initialized) {
            initialize();
        }

        if (!dataLoaded) {
            loadTypeToSpeciesMap();
        }

        return typeToSpeciesMap.getOrDefault(type.toLowerCase(), Collections.emptySet());
    }

    /**
     * Get all available types
     */
    public static Set<String> getAllTypes() {
        if (!initialized) {
            initialize();
        }

        if (!dataLoaded) {
            loadTypeToSpeciesMap();
        }

        return typeToSpeciesMap.keySet();
    }

    /**
     * Force spawn a Pokémon of a specific type near a player
     */
    public static boolean forceSpawnPokemonOfType(PlayerEntity player, String type) {
        if (!initialized || !isPixelmonLoaded || !(player instanceof ServerPlayerEntity)) {
            return false;
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        try {
            // Get Pokémon of this type
            Set<String> pokemonOfType = getPokemonOfType(type);
            if (pokemonOfType.isEmpty()) {
                return false;
            }

            // Choose a random one
            List<String> pokemonList = new ArrayList<>(pokemonOfType);
            String chosenPokemon = pokemonList.get(new Random().nextInt(pokemonList.size()));

            // Spawn it using command
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn " + chosenPokemon
            );

            CustomConsumables.getLogger().info("Successfully spawned a {} for player {}",
                    chosenPokemon, player.getName().getString());

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error forcing spawn: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Register a type boost with Pixelmon's systems
     */
    public static boolean registerTypeBoost(PlayerEntity player, String type, int duration) {
        if (!initialized || !isPixelmonLoaded || !(player instanceof ServerPlayerEntity)) {
            return false;
        }

        try {
            // Get Pokémon of this type for messaging
            Set<String> pokemonOfType = getPokemonOfType(type);
            int count = pokemonOfType.size();

            // Try to use Pixelmon's command to boost types
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn boosttype " + type.toLowerCase() + " 10"
            );

            // Notify player
            serverPlayer.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + "Registered " +
                            type + " type boost with " + count + " eligible Pokémon"),
                    serverPlayer.getUUID()
            );

            CustomConsumables.getLogger().info("Registered {} type boost for player {} with {} eligible Pokémon",
                    type, player.getName().getString(), count);

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error registering type boost: {}", e.getMessage());
            return false;
        }
    }
}