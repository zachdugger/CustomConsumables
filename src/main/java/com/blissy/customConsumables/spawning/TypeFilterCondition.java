package com.blissy.customConsumables.spawning;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

/**
 * A custom spawn condition for Pixelmon that filters spawns based on type.
 * This implements Pixelmon's SpawnCondition interface via reflection to avoid
 * direct dependencies.
 */
public class TypeFilterCondition {

    private static final Random RANDOM = new Random();

    // Cache for reflection
    private static boolean initialized = false;
    private static Method getTypesMethod;

    /**
     * Initialize reflection for Pixelmon classes
     */
    private static void initialize() {
        if (initialized) return;

        try {
            // Get the Species class
            Class<?> speciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");

            // Get the getTypes method
            getTypesMethod = speciesClass.getMethod("getTypes");

            initialized = true;
            CustomConsumables.getLogger().info("TypeFilterCondition initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize TypeFilterCondition: {}", e.getMessage());
        }
    }

    /**
     * This method is called by Pixelmon to check if a spawn should be allowed.
     * It's invoked via reflection.
     *
     * @param world The world where the spawn is occurring
     * @param pos The position where the spawn is occurring
     * @param species The Pixelmon species being spawned
     * @return true if the spawn should be allowed, false to block it
     */
    public boolean shouldSpawn(World world, BlockPos pos, Object species) {
        if (!initialized) {
            initialize();
        }

        if (!initialized || species == null) {
            return true; // Allow spawn on initialization failure
        }

        try {
            // Find nearby players with active type filters
            PlayerEntity nearestPlayer = findNearbyPlayerWithTypeFilter(world, pos);
            if (nearestPlayer == null) {
                return true; // No players with type filters nearby, allow spawn
            }

            // Get the type filter
            String typeFilter = PlayerEffectManager.getTypeAttractorType(nearestPlayer);
            if (typeFilter == null || typeFilter.isEmpty()) {
                return true; // No filter, allow spawn
            }

            // Get the Pokémon's types
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
            float multiplier = PlayerEffectManager.getTypeAttractorChance(nearestPlayer, 0) / 100.0f;
            float roll = RANDOM.nextFloat();
            float blockChance = Math.min(0.9f, 1.0f - (1.0f / multiplier));

            // Allow based on probability - higher multiplier means higher chance to block
            return roll >= blockChance;

        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error in TypeFilterCondition: {}", e.getMessage());
            return true; // Allow spawn on error
        }
    }

    /**
     * Find the nearest player with an active type filter
     */
    private PlayerEntity findNearbyPlayerWithTypeFilter(World world, BlockPos pos) {
        PlayerEntity nearestPlayer = null;
        double closestDistance = 64 * 64; // 64 block range squared

        for (PlayerEntity player : world.players()) {
            if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
                double distance = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }

        return nearestPlayer;
    }

    /**
     * Getter for the priority.
     * Pixelmon calls this via reflection.
     * Higher values mean higher priority.
     */
    public int getPriority() {
        return 80; // High priority to ensure it runs before most conditions
    }
}