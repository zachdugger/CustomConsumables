package com.blissy.customConsumables.data;

import com.blissy.customConsumables.CustomConsumables;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analysis tools for Pokémon spawn data to provide intelligent type filtering
 */
public class PokemonSpawnAnalyzer {
    private static PokemonSpawnAnalyzer instance;

    // Paths to data directories
    private final Path pokemonDataDir;
    private final Path spawnDataDir;

    // Data caches
    private final Map<String, Set<String>> biomeToSpawnsMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> biomeTypeDensityMap = new HashMap<>();
    private final Map<String, Set<String>> typeToEligibleSpawnsMap = new HashMap<>();

    // Gson for JSON parsing
    private final Gson gson = new Gson();

    // Flag to track if data has been loaded
    private boolean dataLoaded = false;

    /**
     * Private constructor for singleton pattern
     */
    private PokemonSpawnAnalyzer() {
        // Try to locate data directories
        Path gamePath = Paths.get("").toAbsolutePath();
        pokemonDataDir = gamePath.resolve("pixelmon_data");
        spawnDataDir = gamePath.resolve("spawns");
    }

    /**
     * Get the singleton instance
     */
    public static PokemonSpawnAnalyzer getInstance() {
        if (instance == null) {
            instance = new PokemonSpawnAnalyzer();
        }
        return instance;
    }

    /**
     * Initialize and load all data
     */
    public synchronized void initialize() {
        if (dataLoaded) return;

        CustomConsumables.getLogger().info("Initializing PokemonSpawnAnalyzer...");

        try {
            // Load Pokémon type data
            loadPokemonTypeData();

            // Load spawn data
            loadSpawnData();

            // Analyze biome type distributions
            analyzeTypeDistribution();

            dataLoaded = true;

            // Log summary
            CustomConsumables.getLogger().info("PokemonSpawnAnalyzer loaded data for {} biomes", biomeToSpawnsMap.size());
            CustomConsumables.getLogger().info("Analyzed type distributions for {} biomes", biomeTypeDensityMap.size());
            CustomConsumables.getLogger().info("Mapped eligible spawns for {} types", typeToEligibleSpawnsMap.size());

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error initializing PokemonSpawnAnalyzer", e);
        }
    }

    /**
     * Load Pokémon type data from JSON files
     */
    private void loadPokemonTypeData() throws Exception {
        if (!Files.exists(pokemonDataDir)) {
            CustomConsumables.getLogger().warn("Pokémon data directory not found: {}", pokemonDataDir);
            return;
        }

        // Find all JSON files in the Pokémon data directory
        List<File> jsonFiles;
        try (Stream<Path> paths = Files.walk(pokemonDataDir)) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }

        CustomConsumables.getLogger().info("Found {} Pokémon data files", jsonFiles.size());

        // Process each file
        for (File file : jsonFiles) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                processPokemonFile(root);
            } catch (Exception e) {
                CustomConsumables.getLogger().debug("Error processing {}: {}", file.getName(), e.getMessage());
            }
        }
    }

    /**
     * Process a single Pokémon data file
     */
    private void processPokemonFile(JsonObject root) {
        try {
            // Get the Pokémon's name
            String pokemonName = root.get("name").getAsString().toLowerCase();

            // Get the forms array
            JsonArray forms = root.getAsJsonArray("forms");
            if (forms == null || forms.size() == 0) return;

            // Get the first form
            JsonObject firstForm = forms.get(0).getAsJsonObject();

            // Get types from the first form
            JsonArray typesArray = firstForm.getAsJsonArray("types");
            if (typesArray == null || typesArray.size() == 0) return;

            // Process each type
            for (JsonElement typeElement : typesArray) {
                String type = typeElement.getAsString().toLowerCase();

                // Add this Pokémon to the type's eligible spawns
                typeToEligibleSpawnsMap
                        .computeIfAbsent(type, k -> new HashSet<>())
                        .add(pokemonName);
            }

        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error processing Pokémon data: {}", e.getMessage());
        }
    }

    /**
     * Load spawn data from JSON files
     */
    private void loadSpawnData() throws Exception {
        if (!Files.exists(spawnDataDir)) {
            CustomConsumables.getLogger().warn("Spawn data directory not found: {}", spawnDataDir);
            return;
        }

        // Find all JSON files in the spawn data directory
        List<File> jsonFiles;
        try (Stream<Path> paths = Files.walk(spawnDataDir)) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }

        CustomConsumables.getLogger().info("Found {} spawn data files", jsonFiles.size());

        // Process each file
        for (File file : jsonFiles) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                processSpawnFile(root);
            } catch (Exception e) {
                CustomConsumables.getLogger().debug("Error processing {}: {}", file.getName(), e.getMessage());
            }
        }
    }

    /**
     * Process a single spawn data file
     */
    private void processSpawnFile(JsonObject root) {
        try {
            // Get the Pokémon ID
            String pokemonId = root.get("id").getAsString().toLowerCase();

            // Get spawn infos array
            JsonArray spawnInfos = root.getAsJsonArray("spawnInfos");
            if (spawnInfos == null || spawnInfos.size() == 0) return;

            // Process each spawn info
            for (JsonElement infoElement : spawnInfos) {
                JsonObject spawnInfo = infoElement.getAsJsonObject();

                // Get the Pokémon spec (usually species:PokemonName)
                String spec = spawnInfo.get("spec").getAsString();
                String pokemonName = extractPokemonName(spec).toLowerCase();

                // Check for condition field (contains biome information)
                if (spawnInfo.has("condition")) {
                    JsonObject condition = spawnInfo.getAsJsonObject("condition");

                    // Process biome information if available
                    if (condition.has("stringBiomes")) {
                        JsonArray biomes = condition.getAsJsonArray("stringBiomes");

                        if (biomes != null) {
                            for (JsonElement biomeElement : biomes) {
                                String biomeName = biomeElement.getAsString().toLowerCase();

                                // Add to biome-to-spawns mapping
                                biomeToSpawnsMap
                                        .computeIfAbsent(biomeName, k -> new HashSet<>())
                                        .add(pokemonName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error processing spawn data: {}", e.getMessage());
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
     * Analyze type distribution in each biome
     */
    private void analyzeTypeDistribution() {
        // For each biome
        for (Map.Entry<String, Set<String>> entry : biomeToSpawnsMap.entrySet()) {
            String biomeName = entry.getKey();
            Set<String> spawnablePokemon = entry.getValue();

            Map<String, Integer> typeCounts = new HashMap<>();

            // Count Pokémon of each type in this biome
            for (String pokemon : spawnablePokemon) {
                // Find matching types for this Pokémon
                for (Map.Entry<String, Set<String>> typeEntry : typeToEligibleSpawnsMap.entrySet()) {
                    String type = typeEntry.getKey();
                    Set<String> pokemonOfType = typeEntry.getValue();

                    if (pokemonOfType.contains(pokemon)) {
                        // Increment count for this type
                        typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
                    }
                }
            }

            // Store type distribution for this biome
            biomeTypeDensityMap.put(biomeName, typeCounts);
        }
    }

    /**
     * Get Pokémon of a specific type that can spawn in a player's current biome
     * @param player The player entity
     * @param type The Pokémon type to filter for
     * @return Set of eligible Pokémon names
     */
    public Set<String> getEligiblePokemonForPlayer(PlayerEntity player, String type) {
        if (!dataLoaded) initialize();

        // Get the player's current biome
        World world = player.level;
        BlockPos pos = player.blockPosition();
        String biomeName = BiomeHelper.getCurrentBiomeName(player);

        // Find what can spawn in this biome
        Set<String> biomeSpawns = biomeToSpawnsMap.get(biomeName);

        // If no direct match, try similar biomes
        if (biomeSpawns == null || biomeSpawns.isEmpty()) {
            // Try the biome category as a fallback
            Biome.Category category = world.getBiome(pos).getBiomeCategory();
            String categoryName = category.getName().toLowerCase();
            biomeSpawns = biomeToSpawnsMap.get(categoryName);

            // If still no match, try similar biomes
            if (biomeSpawns == null || biomeSpawns.isEmpty()) {
                List<String> similarBiomes = BiomeHelper.getSimilarBiomes(biomeName);

                for (String similarBiome : similarBiomes) {
                    Set<String> similarBiomeSpawns = biomeToSpawnsMap.get(similarBiome);
                    if (similarBiomeSpawns != null && !similarBiomeSpawns.isEmpty()) {
                        biomeSpawns = similarBiomeSpawns;
                        break;
                    }
                }
            }
        }

        // Get all Pokémon of the requested type
        Set<String> pokemonOfType = typeToEligibleSpawnsMap.get(type.toLowerCase());

        // If we have both biome spawns and type data, find the intersection
        if (biomeSpawns != null && !biomeSpawns.isEmpty() &&
                pokemonOfType != null && !pokemonOfType.isEmpty()) {

            Set<String> eligiblePokemon = new HashSet<>(biomeSpawns);
            eligiblePokemon.retainAll(pokemonOfType);

            // If we found matches, return them
            if (!eligiblePokemon.isEmpty()) {
                return eligiblePokemon;
            }
        }

        // If no matches or missing data, fall back to all Pokémon of this type
        return pokemonOfType != null ? pokemonOfType : Collections.emptySet();
    }

    /**
     * Get the dominant Pokémon types in a specific biome
     * @param biomeName The biome name to analyze
     * @return Map of type -> count, sorted by frequency (descending)
     */
    public Map<String, Integer> getDominantTypes(String biomeName) {
        if (!dataLoaded) initialize();

        Map<String, Integer> typeCounts = biomeTypeDensityMap.get(biomeName.toLowerCase());

        if (typeCounts == null || typeCounts.isEmpty()) {
            // Try similar biomes
            List<String> similarBiomes = BiomeHelper.getSimilarBiomes(biomeName);

            for (String similarBiome : similarBiomes) {
                Map<String, Integer> similarTypeCounts = biomeTypeDensityMap.get(similarBiome);
                if (similarTypeCounts != null && !similarTypeCounts.isEmpty()) {
                    typeCounts = similarTypeCounts;
                    break;
                }
            }
        }

        // If still null, return empty map
        if (typeCounts == null) {
            return Collections.emptyMap();
        }

        // Sort by count (descending)
        return sortByValue(typeCounts);
    }

    /**
     * Sort a map by its values (descending order)
     */
    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.<K, V>comparingByValue().reversed());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Get the top N most common types in a biome
     * @param biomeName The biome name
     * @param n Number of types to return
     * @return List of type names, ordered by frequency
     */
    public List<String> getTopTypes(String biomeName, int n) {
        Map<String, Integer> typeCounts = getDominantTypes(biomeName);

        return typeCounts.entrySet().stream()
                .limit(n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Suggest the most appropriate type attractor for a player's current biome
     * @param player The player entity
     * @return The best type for this biome, or null if no data
     */
    public String suggestTypeForPlayer(PlayerEntity player) {
        String biomeName = BiomeHelper.getCurrentBiomeName(player);
        List<String> topTypes = getTopTypes(biomeName, 1);

        return topTypes.isEmpty() ? null : topTypes.get(0);
    }

    /**
     * Get all available Pokémon types
     */
    public Set<String> getAllTypes() {
        if (!dataLoaded) initialize();
        return typeToEligibleSpawnsMap.keySet();
    }

    /**
     * Check if a specific type is valid/loaded
     */
    public boolean isValidType(String type) {
        if (!dataLoaded) initialize();
        return typeToEligibleSpawnsMap.containsKey(type.toLowerCase());
    }
}