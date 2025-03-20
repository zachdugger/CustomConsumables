package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class handles loading and caching Pokémon type data from the Pixelmon data files.
 * It's used to determine if a Pokémon matches a specific type for the Type Attractor item.
 */
public class PokemonTypeDataHandler implements ISelectiveResourceReloadListener {
    // Singleton instance
    private static PokemonTypeDataHandler instance;

    // Cache of Pokémon name -> set of types
    private final Map<String, Set<String>> pokemonTypeCache = new HashMap<>();

    // Gson parser
    private final Gson gson = new Gson();

    // Flag to track if types are loaded
    public boolean typesLoaded = false;

    // Base directory for Pixelmon data
    private static final String PIXELMON_DATA_DIR = "pixelmon_data";

    private PokemonTypeDataHandler() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance
     */
    public static PokemonTypeDataHandler getInstance() {
        if (instance == null) {
            instance = new PokemonTypeDataHandler();
        }
        return instance;
    }

    /**
     * Initialize the type data from the pixelmon_data folder
     */
    public void initialize() {
        if (typesLoaded) {
            return; // Already loaded
        }

        CustomConsumables.getLogger().info("Loading Pokémon type data from pixelmon_data folder...");

        try {
            // Try to load from the pixelmon_data directory in the mod's root folder
            File dataDir = new File(PIXELMON_DATA_DIR);
            if (dataDir.exists() && dataDir.isDirectory()) {
                loadTypesFromDirectory(dataDir);
            } else {
                CustomConsumables.getLogger().warn("Couldn't find pixelmon_data directory, trying resource loading...");
                loadTypesFromResources();
            }

            CustomConsumables.getLogger().info("Loaded types for {} Pokémon", pokemonTypeCache.size());
            typesLoaded = true;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to load Pokémon type data", e);
        }
    }

    /**
     * Load Pokémon types from the directory
     */
    private void loadTypesFromDirectory(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            CustomConsumables.getLogger().warn("No JSON files found in pixelmon_data directory");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                parseAndCacheTypes(reader, file.getName());
                loaded++;

                // Provide some logging feedback every 100 files
                if (loaded % 100 == 0) {
                    CustomConsumables.getLogger().info("Processed {} Pokémon data files...", loaded);
                }
            } catch (Exception e) {
                CustomConsumables.getLogger().error("Error parsing {}: {}", file.getName(), e.getMessage());
            }
        }
    }

    /**
     * Load Pokémon types from resources
     */
    private void loadTypesFromResources() {
        try {
            IResourceManager resourceManager = ObfuscationReflectionHelper.getPrivateValue(
                    net.minecraft.client.Minecraft.class,
                    net.minecraft.client.Minecraft.getInstance(),
                    "field_110451_am" // resourceManager
            );

            // Try to find and load JSON files from resources
            // This is a fallback if the directory loading fails
            for (int i = 1; i <= 898; i++) {
                String paddedId = String.format("%03d", i);
                ResourceLocation location = new ResourceLocation(
                        CustomConsumables.MOD_ID,
                        "pixelmon_data/" + paddedId + ".json"
                );

                try {
                    IResource resource = resourceManager.getResource(location);
                    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                        parseAndCacheTypes(reader, paddedId);
                    }
                } catch (Exception e) {
                    // Just skip files that don't exist or can't be parsed
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to load from resources", e);
        }
    }

    /**
     * Parse the Pokémon JSON and cache the type data
     */
    private void parseAndCacheTypes(Reader reader, String filename) {
        try {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            // Get the Pokémon name
            String pokemonName = root.get("name").getAsString();

            // Process forms array
            JsonArray forms = root.getAsJsonArray("forms");
            if (forms == null || forms.size() == 0) {
                return;
            }

            // Look at the first form to get default types
            JsonObject firstForm = forms.get(0).getAsJsonObject();
            JsonArray typesArray = firstForm.getAsJsonArray("types");

            if (typesArray != null && typesArray.size() > 0) {
                Set<String> types = new HashSet<>();
                for (JsonElement typeElement : typesArray) {
                    String type = typeElement.getAsString().toUpperCase();
                    types.add(type);
                }

                // Add to cache
                pokemonTypeCache.put(pokemonName.toLowerCase(), types);
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error parsing Pokémon data from {}: {}", filename, e.getMessage());
        }
    }

    /**
     * Check if a Pokémon has a specific type
     * @param pokemonName The name of the Pokémon
     * @param type The type to check for
     * @return true if the Pokémon has the specified type
     */
    public boolean hasPokemonType(String pokemonName, String type) {
        if (!typesLoaded) {
            initialize();
        }

        // Normalize inputs
        pokemonName = pokemonName.toLowerCase();
        type = type.toUpperCase();

        // Check cache
        Set<String> types = pokemonTypeCache.get(pokemonName);
        if (types == null) {
            // Try to handle some common spelling variations or form differences
            if (pokemonName.contains("-")) {
                // Try without the form suffix
                String baseName = pokemonName.split("-")[0];
                types = pokemonTypeCache.get(baseName);
            }

            if (types == null) {
                CustomConsumables.getLogger().debug("No type data found for Pokémon: {}", pokemonName);
                return false;
            }
        }

        return types.contains(type);
    }

    /**
     * Get all types for a Pokémon
     * @param pokemonName The name of the Pokémon
     * @return A set of types, or null if the Pokémon isn't found
     */
    @Nullable
    public Set<String> getPokemonTypes(String pokemonName) {
        if (!typesLoaded) {
            initialize();
        }

        // Normalize inputs
        pokemonName = pokemonName.toLowerCase();

        // Check cache
        Set<String> types = pokemonTypeCache.get(pokemonName);
        if (types == null) {
            // Try to handle some common spelling variations or form differences
            if (pokemonName.contains("-")) {
                // Try without the form suffix
                String baseName = pokemonName.split("-")[0];
                types = pokemonTypeCache.get(baseName);
            }
        }

        return types;
    }

    /**
     * Resource reload listener implementation
     */
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        // In Minecraft 1.16.5, we check if it's a data reload in a different way
        // The resourcePredicate will tell us what type of reload is happening
        if (resourcePredicate.test(VanillaResourceType.TEXTURES)) {
            // We reload on any reload that affects data
            pokemonTypeCache.clear();
            typesLoaded = false;
            initialize();
        }
    }
}