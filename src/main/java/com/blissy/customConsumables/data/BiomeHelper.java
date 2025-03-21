package com.blissy.customConsumables.data;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Helper class for working with biomes and finding appropriate spawn matches
 */
public class BiomeHelper {
    // Cache of similar biomes (for fallback matching)
    private static final Map<String, List<String>> SIMILAR_BIOMES = new HashMap<>();

    // Cache of biome categories
    private static final Map<Biome.Category, List<String>> BIOME_CATEGORIES = new HashMap<>();

    // Initialize biome similarity map
    static {
        // Similar desert biomes
        addSimilarBiomes("desert", Arrays.asList("desert", "desert_hills", "desert_lakes", "badlands", "badlands_plateau", "wooded_badlands_plateau"));

        // Similar forest biomes
        addSimilarBiomes("forest", Arrays.asList("forest", "wooded_hills", "birch_forest", "birch_forest_hills",
                "dark_forest", "flower_forest", "tall_birch_forest"));

        // Similar mountain biomes
        addSimilarBiomes("mountains", Arrays.asList("mountains", "mountain_edge", "gravelly_mountains", "modified_gravelly_mountains",
                "snowy_mountains", "snowy_taiga_mountains"));

        // Similar plains biomes
        addSimilarBiomes("plains", Arrays.asList("plains", "sunflower_plains", "meadow", "savanna", "savanna_plateau"));

        // Similar taiga biomes
        addSimilarBiomes("taiga", Arrays.asList("taiga", "taiga_hills", "taiga_mountains", "snowy_taiga", "snowy_taiga_hills",
                "giant_tree_taiga", "giant_spruce_taiga"));

        // Similar jungle biomes
        addSimilarBiomes("jungle", Arrays.asList("jungle", "jungle_hills", "jungle_edge", "modified_jungle", "bamboo_jungle"));

        // Similar swamp biomes
        addSimilarBiomes("swamp", Arrays.asList("swamp", "swamp_hills"));

        // Similar ocean biomes
        addSimilarBiomes("ocean", Arrays.asList("ocean", "deep_ocean", "warm_ocean", "lukewarm_ocean", "cold_ocean",
                "deep_warm_ocean", "deep_lukewarm_ocean", "deep_cold_ocean", "deep_frozen_ocean"));

        // Similar beach biomes
        addSimilarBiomes("beach", Arrays.asList("beach", "snowy_beach", "stone_shore"));

        // Similar river biomes
        addSimilarBiomes("river", Arrays.asList("river", "frozen_river"));

        // Similar tundra biomes
        addSimilarBiomes("tundra", Arrays.asList("snowy_tundra", "ice_spikes", "snowy_beach", "frozen_river"));

        // Initialize biome categories
        for (Biome.Category category : Biome.Category.values()) {
            BIOME_CATEGORIES.put(category, new ArrayList<>());
        }

        // Map all registered biomes to their categories
        for (Map.Entry<RegistryKey<Biome>, Biome> entry : ForgeRegistries.BIOMES.getEntries()) {
            ResourceLocation biomeKey = entry.getKey().location();
            Biome biome = entry.getValue();
            String biomeName = biomeKey.getPath().toLowerCase();

            BIOME_CATEGORIES.computeIfAbsent(biome.getBiomeCategory(), k -> new ArrayList<>())
                    .add(biomeName);
        }
    }

    /**
     * Helper method to add similar biomes to the mapping
     */
    private static void addSimilarBiomes(String key, List<String> similarBiomes) {
        SIMILAR_BIOMES.put(key, similarBiomes);

        // Also add each individual biome as a key that maps to the same list
        for (String biome : similarBiomes) {
            if (!biome.equals(key)) {
                SIMILAR_BIOMES.put(biome, similarBiomes);
            }
        }
    }

    /**
     * Get the current biome name for a player
     * @param player The player entity
     * @return The biome name
     */
    public static String getCurrentBiomeName(PlayerEntity player) {
        World world = player.level;
        BlockPos pos = player.blockPosition();

        ResourceLocation biomeKey = world.getBiome(pos).getRegistryName();
        if (biomeKey != null) {
            return biomeKey.getPath().toLowerCase();
        }

        // Fallback to biome category if registry name isn't available
        Biome.Category category = world.getBiome(pos).getBiomeCategory();
        return category.getName().toLowerCase();
    }

    /**
     * Get the current biome category for a player
     * @param player The player entity
     * @return The biome category
     */
    public static Biome.Category getCurrentBiomeCategory(PlayerEntity player) {
        World world = player.level;
        BlockPos pos = player.blockPosition();

        return world.getBiome(pos).getBiomeCategory();
    }

    /**
     * Find similar biomes to the given biome name
     * @param biomeName The biome name to find similarities for
     * @return List of similar biome names
     */
    public static List<String> getSimilarBiomes(String biomeName) {
        String lowerName = biomeName.toLowerCase();

        // Check if we have direct similarity mapping
        if (SIMILAR_BIOMES.containsKey(lowerName)) {
            return SIMILAR_BIOMES.get(lowerName);
        }

        // Try to find the biome in the registry
        for (ResourceLocation key : ForgeRegistries.BIOMES.getKeys()) {
            if (key.getPath().equalsIgnoreCase(biomeName)) {
                Biome biome = ForgeRegistries.BIOMES.getValue(key);
                if (biome != null) {
                    // Return all biomes in the same category
                    Biome.Category category = biome.getBiomeCategory();
                    List<String> categoryBiomes = BIOME_CATEGORIES.get(category);
                    if (categoryBiomes != null && !categoryBiomes.isEmpty()) {
                        return categoryBiomes;
                    }
                }
            }
        }

        // Return a single-element list with just the input biome if no matches
        return Collections.singletonList(lowerName);
    }

    /**
     * Get all biomes in a specific category
     * @param category The biome category
     * @return List of biome names in that category
     */
    public static List<String> getBiomesInCategory(Biome.Category category) {
        return BIOME_CATEGORIES.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Check if a biome name might be a general category rather than a specific biome
     * @param biomeName The biome name to check
     * @return true if it appears to be a category
     */
    public static boolean isLikelyBiomeCategory(String biomeName) {
        String lowerName = biomeName.toLowerCase();

        // Check against known category-like names
        return lowerName.equals("desert") ||
                lowerName.equals("forest") ||
                lowerName.equals("taiga") ||
                lowerName.equals("mountains") ||
                lowerName.equals("jungle") ||
                lowerName.equals("ocean") ||
                lowerName.equals("plains") ||
                lowerName.equals("swamp") ||
                lowerName.equals("savanna") ||
                lowerName.equals("beach") ||
                lowerName.equals("river");
    }
}