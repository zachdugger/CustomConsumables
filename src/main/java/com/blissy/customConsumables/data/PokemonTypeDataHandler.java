package com.blissy.customConsumables.data;

import com.blissy.customConsumables.CustomConsumables;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enhanced handler for Pokémon data that processes both type information and spawn data
 * to provide intelligent type filtering based on player context
 */
public class PokemonTypeDataHandler {
    // Singleton instance
    private static PokemonTypeDataHandler instance;

    // Data directories
    private static final Path POKEMON_DATA_DIR = FMLPaths.GAMEDIR.get().resolve("pixelmon_data");
    private static final Path SPAWN_DATA_DIR = FMLPaths.GAMEDIR.get().resolve("spawns");

    // Pokémon type mappings
    private Map<String, Set<String>> pokemonTypeMap = new HashMap<>();

    // Biome to Pokemon mappings (biome name -> list of eligible Pokémon)
    private Map<String, Set<String>> biomePokemonMap = new HashMap<>();

    // Type to Pokemon mappings (type name -> list of Pokémon of that type)
    private Map<String, Set<String>> typePokemonMap = new HashMap<>();

    // Spawn location types for Pokémon
    private Map<String, Set<String>> pokemonLocationMap = new HashMap<>();

    // Gson for JSON parsing
    private final Gson gson = new Gson();

    // Flag to indicate if data has been loaded
    private boolean dataLoaded = false;

    // Whether to prefer internal data over directory data
    private boolean useInternalData = false;

    // Private constructor for singleton
    private PokemonTypeDataHandler() {}

    /**
     * Get the singleton instance
     */
    public static synchronized PokemonTypeDataHandler getInstance() {
        if (instance == null) {
            instance = new PokemonTypeDataHandler();
        }
        return instance;
    }

    /**
     * Initialize the data handler, loading all necessary data
     */
    public synchronized void initialize() {
        if (dataLoaded) {
            return;
        }

        CustomConsumables.getLogger().info("Initializing PokemonTypeDataHandler...");

        // First try to load from external directories
        boolean externalDataLoaded = loadExternalData();

        // Fall back to internal data if external loading failed
        if (!externalDataLoaded) {
            loadInternalData();
            useInternalData = true;
        }

        // Process the loaded data to build additional mappings
        processPokemonTypeData();

        dataLoaded = true;

        // Log summary of loaded data
        CustomConsumables.getLogger().info("Loaded data for {} Pokémon with types", pokemonTypeMap.size());
        CustomConsumables.getLogger().info("Loaded spawn data for {} biomes", biomePokemonMap.size());
        CustomConsumables.getLogger().info("Mapped {} different Pokémon types", typePokemonMap.size());
    }

    /**
     * Load data from external directories
     * @return true if data was successfully loaded
     */
    private boolean loadExternalData() {
        boolean success = false;

        try {
            // Load Pokémon type data
            if (Files.exists(POKEMON_DATA_DIR)) {
                loadPokemonTypesFromDirectory(POKEMON_DATA_DIR.toFile());
                success = !pokemonTypeMap.isEmpty();
            }

            // Load spawn data
            if (Files.exists(SPAWN_DATA_DIR)) {
                loadSpawnDataFromDirectory(SPAWN_DATA_DIR.toFile());
                // Only count as successful if we have both type and spawn data
                success = success && !biomePokemonMap.isEmpty();
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error loading external Pokémon data", e);
            success = false;
        }

        return success;
    }

    /**
     * Load data from internal resources as a fallback
     */
    private void loadInternalData() {
        try {
            // For internal data, we'll use a built-in set of common mappings
            loadDefaultTypeMappings();
            loadDefaultBiomeMappings();
            CustomConsumables.getLogger().info("Using default internal data mappings");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error loading internal Pokémon data", e);
        }
    }

    /**
     * Process the loaded data to build additional mappings
     */
    private void processPokemonTypeData() {
        // Build reverse mapping of type -> list of Pokémon
        typePokemonMap.clear();

        for (Map.Entry<String, Set<String>> entry : pokemonTypeMap.entrySet()) {
            String pokemon = entry.getKey();
            Set<String> types = entry.getValue();

            for (String type : types) {
                typePokemonMap
                        .computeIfAbsent(type.toLowerCase(), k -> new HashSet<>())
                        .add(pokemon.toLowerCase());
            }
        }

        // Log the number of Pokémon for each type
        for (Map.Entry<String, Set<String>> entry : typePokemonMap.entrySet()) {
            CustomConsumables.getLogger().debug("Type {}: {} Pokémon",
                    entry.getKey(), entry.getValue().size());
        }
    }

    /**
     * Load Pokémon type data from the directory
     */
    private void loadPokemonTypesFromDirectory(File directory) {
        try {
            // Find all JSON files in the directory
            List<File> jsonFiles = findJsonFiles(directory);
            CustomConsumables.getLogger().info("Found {} JSON files in {}",
                    jsonFiles.size(), directory.getAbsolutePath());

            // Process each file
            for (File file : jsonFiles) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject root = gson.fromJson(reader, JsonObject.class);
                    processPokemonTypeFile(root);
                } catch (Exception e) {
                    CustomConsumables.getLogger().error("Error processing {}: {}",
                            file.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error loading Pokémon types", e);
        }
    }

    /**
     * Process a Pokémon type file
     */
    private void processPokemonTypeFile(JsonObject root) {
        try {
            // Get Pokémon name
            String pokemonName = root.get("name").getAsString().toLowerCase();

            // Get forms array
            JsonArray forms = root.getAsJsonArray("forms");
            if (forms == null || forms.size() == 0) {
                return;
            }

            // Get the types from the first form
            JsonObject firstForm = forms.get(0).getAsJsonObject();
            JsonArray typesArray = firstForm.getAsJsonArray("types");

            if (typesArray != null && typesArray.size() > 0) {
                Set<String> types = new HashSet<>();
                for (JsonElement typeElement : typesArray) {
                    String type = typeElement.getAsString().toLowerCase();
                    types.add(type);
                }

                // Add to type map
                pokemonTypeMap.put(pokemonName, types);

                // If there's spawn data, also record it
                if (firstForm.has("spawn")) {
                    JsonObject spawnData = firstForm.getAsJsonObject("spawn");
                    if (spawnData.has("spawnLocations")) {
                        JsonArray locationsArray = spawnData.getAsJsonArray("spawnLocations");
                        if (locationsArray != null && locationsArray.size() > 0) {
                            Set<String> locations = new HashSet<>();
                            for (JsonElement locElement : locationsArray) {
                                locations.add(locElement.getAsString().toLowerCase());
                            }
                            pokemonLocationMap.put(pokemonName, locations);
                        }
                    }
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error processing Pokémon data: {}", e.getMessage());
        }
    }

    /**
     * Load spawn data from the directory
     */
    private void loadSpawnDataFromDirectory(File directory) {
        try {
            // Find all JSON files in the directory
            List<File> jsonFiles = findJsonFiles(directory);
            CustomConsumables.getLogger().info("Found {} spawn JSON files in {}",
                    jsonFiles.size(), directory.getAbsolutePath());

            // Process each file
            for (File file : jsonFiles) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject root = gson.fromJson(reader, JsonObject.class);
                    processSpawnFile(root);
                } catch (Exception e) {
                    CustomConsumables.getLogger().error("Error processing spawn file {}: {}",
                            file.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error loading spawn data", e);
        }
    }

    /**
     * Process a spawn file
     */
    private void processSpawnFile(JsonObject root) {
        try {
            // Get Pokémon ID
            String pokemonId = root.get("id").getAsString().toLowerCase();

            // Get spawn infos
            JsonArray spawnInfos = root.getAsJsonArray("spawnInfos");
            if (spawnInfos == null || spawnInfos.size() == 0) {
                return;
            }

            // Process each spawn info
            for (JsonElement infoElement : spawnInfos) {
                JsonObject spawnInfo = infoElement.getAsJsonObject();

                // Get the spec (usually species:PokemonName)
                String spec = spawnInfo.get("spec").getAsString();
                String pokemonName = extractPokemonName(spec).toLowerCase();

                // Get location types
                JsonArray locationTypes = spawnInfo.getAsJsonArray("stringLocationTypes");
                if (locationTypes != null) {
                    Set<String> locations = new HashSet<>();
                    for (JsonElement locElement : locationTypes) {
                        locations.add(locElement.getAsString().toLowerCase());
                    }
                    pokemonLocationMap.put(pokemonName, locations);
                }

                // Get condition info if present
                if (spawnInfo.has("condition")) {
                    JsonObject condition = spawnInfo.getAsJsonObject("condition");

                    // Get biomes if present
                    if (condition.has("stringBiomes")) {
                        JsonArray biomes = condition.getAsJsonArray("stringBiomes");
                        if (biomes != null) {
                            for (JsonElement biomeElement : biomes) {
                                String biome = biomeElement.getAsString().toLowerCase();

                                // Add to biome -> Pokémon mapping
                                biomePokemonMap
                                        .computeIfAbsent(biome, k -> new HashSet<>())
                                        .add(pokemonName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error processing spawn data: {}", e.getMessage());
        }
    }

    /**
     * Extract Pokémon name from a spec string (e.g., "species:Pikachu" -> "Pikachu")
     */
    private String extractPokemonName(String spec) {
        if (spec.contains(":")) {
            return spec.substring(spec.indexOf(":") + 1);
        }
        return spec;
    }

    /**
     * Load default type mappings as a fallback
     */
    private void loadDefaultTypeMappings() {
        // A minimal set of common Pokémon and their types
        addDefaultTyping("bulbasaur", "grass", "poison");
        addDefaultTyping("charmander", "fire");
        addDefaultTyping("squirtle", "water");
        addDefaultTyping("pikachu", "electric");
        addDefaultTyping("eevee", "normal");
        addDefaultTyping("mewtwo", "psychic");
        addDefaultTyping("dragonite", "dragon", "flying");
        addDefaultTyping("machop", "fighting");
        addDefaultTyping("geodude", "rock", "ground");
        addDefaultTyping("gastly", "ghost", "poison");
        addDefaultTyping("magikarp", "water");
        addDefaultTyping("gyarados", "water", "flying");
        addDefaultTyping("snorlax", "normal");
        addDefaultTyping("gengar", "ghost", "poison");
        addDefaultTyping("alakazam", "psychic");
        addDefaultTyping("onix", "rock", "ground");
        addDefaultTyping("charizard", "fire", "flying");
        addDefaultTyping("blastoise", "water");
        addDefaultTyping("venusaur", "grass", "poison");
        addDefaultTyping("mew", "psychic");
    }

    /**
     * Helper method to add default type mappings
     */
    private void addDefaultTyping(String pokemon, String... types) {
        Set<String> typeSet = new HashSet<>(Arrays.asList(types));
        pokemonTypeMap.put(pokemon.toLowerCase(), typeSet);
    }

    /**
     * Load default biome mappings as a fallback
     */
    private void loadDefaultBiomeMappings() {
        // Add some default biome mappings
        addDefaultBiomeMapping("desert", "sandshrew", "cacnea", "diglett", "trapinch");
        addDefaultBiomeMapping("plains", "rattata", "pidgey", "caterpie", "bellsprout");
        addDefaultBiomeMapping("forest", "caterpie", "weedle", "oddish", "hoothoot");
        addDefaultBiomeMapping("jungle", "exeggcute", "tropius", "tangela", "aipom");
        addDefaultBiomeMapping("mountains", "geodude", "onix", "clefairy", "skarmory");
        addDefaultBiomeMapping("ocean", "tentacool", "magikarp", "staryu", "horsea");
        addDefaultBiomeMapping("beach", "krabby", "staryu", "slowpoke", "corsola");
        addDefaultBiomeMapping("taiga", "sneasel", "swinub", "snorunt", "snover");
        addDefaultBiomeMapping("tundra", "spheal", "snorunt", "delibird", "cubchoo");
        addDefaultBiomeMapping("swamp", "croagunk", "lotad", "wooper", "tympole");
    }

    /**
     * Helper method to add default biome mappings
     */
    private void addDefaultBiomeMapping(String biome, String... pokemon) {
        Set<String> pokemonSet = new HashSet<>(Arrays.asList(pokemon));
        biomePokemonMap.put(biome.toLowerCase(), pokemonSet);
    }

    /**
     * Find all JSON files in a directory and its subdirectories
     */
    private List<File> findJsonFiles(File directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get the types for a specific Pokémon
     * @param pokemonName The name of the Pokémon
     * @return Set of types or empty set if not found
     */
    public Set<String> getPokemonTypes(String pokemonName) {
        if (!dataLoaded) {
            initialize();
        }

        return pokemonTypeMap.getOrDefault(pokemonName.toLowerCase(), Collections.emptySet());
    }

    /**
     * Check if a Pokémon has a specific type
     * @param pokemonName The name of the Pokémon
     * @param type The type to check
     * @return true if the Pokémon has the type
     */
    public boolean hasPokemonType(String pokemonName, String type) {
        Set<String> types = getPokemonTypes(pokemonName);
        return types.contains(type.toLowerCase());
    }

    /**
     * Get all Pokémon of a specific type
     * @param type The type to search for
     * @return Set of Pokémon names
     */
    public Set<String> getPokemonOfType(String type) {
        if (!dataLoaded) {
            initialize();
        }

        return typePokemonMap.getOrDefault(type.toLowerCase(), Collections.emptySet());
    }

    /**
     * Get all Pokémon that can spawn in a specific biome
     * @param biomeName The biome name
     * @return Set of Pokémon names
     */
    public Set<String> getPokemonInBiome(String biomeName) {
        if (!dataLoaded) {
            initialize();
        }

        return biomePokemonMap.getOrDefault(biomeName.toLowerCase(), Collections.emptySet());
    }

    /**
     * Get available spawn location types for a Pokémon
     * @param pokemonName The name of the Pokémon
     * @return Set of location types
     */
    public Set<String> getPokemonLocations(String pokemonName) {
        if (!dataLoaded) {
            initialize();
        }

        return pokemonLocationMap.getOrDefault(pokemonName.toLowerCase(), Collections.emptySet());
    }

    /**
     * Check if a Pokémon can spawn in a specific location type
     * @param pokemonName The name of the Pokémon
     * @param locationType The location type to check
     * @return true if the Pokémon can spawn there
     */
    public boolean canSpawnIn(String pokemonName, String locationType) {
        Set<String> locations = getPokemonLocations(pokemonName);
        return locations.contains(locationType.toLowerCase());
    }

    /**
     * Filter Pokémon that can spawn in the player's current biome and are of the specified type
     * @param player The player entity
     * @param type The type to filter for
     * @return Set of eligible Pokémon names
     */
    public Set<String> getEligiblePokemonForPlayer(PlayerEntity player, String type) {
        if (!dataLoaded) {
            initialize();
        }

        // Get the player's current biome
        ResourceLocation biomeKey = player.level.getBiome(player.blockPosition()).getRegistryName();
        String biomeName = biomeKey != null ? biomeKey.getPath() : "plains";

        // Get all Pokémon of the specified type
        Set<String> typeMatches = getPokemonOfType(type);

        // Get all Pokémon that can spawn in this biome
        Set<String> biomeMatches = getPokemonInBiome(biomeName);

        // If we don't have specific biome data, try to use the biome category
        if (biomeMatches.isEmpty()) {
            Biome.Category biomeCategory = player.level.getBiome(player.blockPosition()).getBiomeCategory();
            String category = biomeCategory.getName().toLowerCase();
            biomeMatches = getPokemonInBiome(category);

            // If still empty, fallback to some defaults based on category
            if (biomeMatches.isEmpty()) {
                switch (category) {
                    case "desert":
                        biomeMatches = new HashSet<>(Arrays.asList("sandshrew", "cacnea", "trapinch"));
                        break;
                    case "forest":
                        biomeMatches = new HashSet<>(Arrays.asList("caterpie", "weedle", "oddish"));
                        break;
                    case "taiga":
                    case "extreme_hills":
                        biomeMatches = new HashSet<>(Arrays.asList("geodude", "clefairy", "swinub"));
                        break;
                    case "swamp":
                        biomeMatches = new HashSet<>(Arrays.asList("poliwag", "croagunk", "lotad"));
                        break;
                    case "ocean":
                        biomeMatches = new HashSet<>(Arrays.asList("tentacool", "magikarp", "horsea"));
                        break;
                    case "plains":
                    default:
                        biomeMatches = new HashSet<>(Arrays.asList("rattata", "pidgey", "eevee"));
                        break;
                }
            }
        }

        // Find the intersection of type matches and biome matches
        Set<String> eligiblePokemon = new HashSet<>(typeMatches);
        eligiblePokemon.retainAll(biomeMatches);

        // If we don't have any matches, fall back to just the type matches
        if (eligiblePokemon.isEmpty()) {
            return typeMatches;
        }

        return eligiblePokemon;
    }

    /**
     * Determine if a Pokémon should be allowed to spawn based on type filtering
     * @param player The player entity
     * @param pokemonName The name of the Pokémon trying to spawn
     * @param activeType The type currently being boosted
     * @return true if the Pokémon should be allowed to spawn
     */
    public boolean shouldAllowPokemonSpawn(PlayerEntity player, String pokemonName, String activeType) {
        if (!dataLoaded) {
            initialize();
        }

        // If no active type, allow all spawns
        if (activeType == null || activeType.isEmpty()) {
            return true;
        }

        // Check if this Pokémon is of the active type
        if (hasPokemonType(pokemonName, activeType)) {
            return true;
        }

        // If not matching the active type, it might still be allowed based on rarity
        // For now, we'll block most non-matching spawns
        return false;
    }

    /**
     * Get a list of all available Pokémon types
     * @return Set of all types
     */
    public Set<String> getAllTypes() {
        if (!dataLoaded) {
            initialize();
        }

        return typePokemonMap.keySet();
    }
}