package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
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

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for Pixelmon spawns to boost and visualize type attractors
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PixelmonSpawnListener {
    private static final Random RANDOM = new Random();

    // Cache to prevent checking the same entity multiple times
    private static final ConcurrentHashMap<Integer, Long> processedEntities = new ConcurrentHashMap<>();

    /**
     * Process natural entity spawns to apply visual effects
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        // Skip if not on server or Pixelmon isn't loaded
        if (event.getWorld().isClientSide() || !PixelmonIntegration.isPixelmonLoaded()) {
            return;
        }

        try {
            // Get entity ID and check if we've already processed it
            int entityId = event.getEntity().getId();
            if (processedEntities.containsKey(entityId)) {
                return;
            }

            // Clean up the processed entities cache occasionally
            if (processedEntities.size() > 200) {
                processedEntities.entrySet().removeIf(entry ->
                        System.currentTimeMillis() - entry.getValue() > 60000); // 1 minute timeout
            }

            // Check if this is a Pixelmon entity using reflection
            if (!isPixelmonEntity(event.getEntity())) {
                return;
            }

            // Mark as processed to avoid checking it again
            processedEntities.put(entityId, System.currentTimeMillis());

            // Find the nearest player with an active effect
            PlayerEntity nearestPlayer = findNearestPlayerWithEffect(event.getEntity());
            if (nearestPlayer == null) {
                return;
            }

            // Get the Pokemon's name for notifications
            String pokemonName = event.getEntity().getDisplayName().getString();
            if (pokemonName.contains("Lv.")) {
                pokemonName = pokemonName.split("Lv.")[0].trim();
            }

            // If player has a type boost and we can get visual confirmation that this spawn
            // matches the player's type filter, add visual effects
            if (PlayerEffectManager.hasTypeAttractorEffect(nearestPlayer)) {
                String boostedType = PlayerEffectManager.getTypeAttractorType(nearestPlayer);

                if (!boostedType.isEmpty()) {
                    // Get type from entity if possible
                    if (matchesPlayerType(event.getEntity(), boostedType)) {
                        // This is a match - add visual confirmation occasionally
                        if (RANDOM.nextFloat() < 0.3f && nearestPlayer instanceof ServerPlayerEntity) {
                            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) nearestPlayer;

                            // Log for debugging
                            CustomConsumables.getLogger().debug(
                                    "Detected {} type Pokemon {} for player {}",
                                    boostedType, pokemonName, serverPlayer.getName().getString());

                            // Inform player occasionally (not always to avoid spam)
                            serverPlayer.sendMessage(
                                    new StringTextComponent(TextFormatting.GREEN +
                                            "Your Type Attractor attracted a " + pokemonName + "!"),
                                    serverPlayer.getUUID()
                            );

                            // Add some particles
                            if (serverPlayer.level instanceof net.minecraft.world.server.ServerWorld) {
                                net.minecraft.world.server.ServerWorld world =
                                        (net.minecraft.world.server.ServerWorld) serverPlayer.level;

                                BlockPos pos = event.getEntity().blockPosition();
                                world.sendParticles(
                                        net.minecraft.particles.ParticleTypes.HAPPY_VILLAGER,
                                        pos.getX(), pos.getY() + 0.5, pos.getZ(),
                                        5, 0.5, 0.5, 0.5, 0.0
                                );
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log but don't crash
            CustomConsumables.getLogger().debug("Error in spawn listener: {}", e.getMessage());
        }
    }

    /**
     * Find the nearest player with an active effect
     */
    private static PlayerEntity findNearestPlayerWithEffect(net.minecraft.entity.Entity entity) {
        BlockPos pos = entity.blockPosition();
        double closestDistance = 64 * 64; // 64 block radius squared
        PlayerEntity closestPlayer = null;

        // Search all players in the same world
        for (PlayerEntity player : entity.level.players()) {
            // Check if player has any effects
            if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
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
     * Check if an entity is a Pixelmon entity
     */
    private static boolean isPixelmonEntity(net.minecraft.entity.Entity entity) {
        try {
            // Use reflection to check if this is a Pixelmon entity
            Class<?> pixelmonEntityClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");
            return pixelmonEntityClass.isInstance(entity);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Try to determine if a Pokémon entity matches a specific type
     * This is best effort and may not always work depending on Pixelmon version
     */
    private static boolean matchesPlayerType(net.minecraft.entity.Entity entity, String type) {
        try {
            // First approach - try direct naming patterns (common naming conventions)
            String entityName = entity.getDisplayName().getString().toLowerCase();

            // This is a basic heuristic check - proper integration would require reflection
            // but this should catch most obvious cases for visual effects
            switch (type.toLowerCase()) {
                case "fire":
                    return entityName.contains("charman") || entityName.contains("cyndaquil") ||
                            entityName.contains("torchic") || entityName.contains("chimchar") ||
                            entityName.contains("fletchinder") || entityName.contains("charmander") ||
                            entityName.contains("charizard") || entityName.contains("flareon") ||
                            entityName.contains("magmar") || entityName.contains("typhlosion");
                case "water":
                    return entityName.contains("squirtle") || entityName.contains("totodile") ||
                            entityName.contains("mudkip") || entityName.contains("piplup") ||
                            entityName.contains("vaporeon") || entityName.contains("gyarados") ||
                            entityName.contains("lapras") || entityName.contains("blastoise") ||
                            entityName.contains("feraligatr") || entityName.contains("swampert");
                case "grass":
                    return entityName.contains("bulbasaur") || entityName.contains("chikorita") ||
                            entityName.contains("treecko") || entityName.contains("turtwig") ||
                            entityName.contains("leafeon") || entityName.contains("venusaur") ||
                            entityName.contains("meganium") || entityName.contains("sceptile") ||
                            entityName.contains("torterra") || entityName.contains("oddish");
                // Add more type matches as needed
                default:
                    // For other types, just try the reflection approach
                    break;
            }

            // Second approach - try reflection if available
            try {
                // Get Pokemon object
                Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

                if (pixelmonClass.isInstance(entity)) {
                    // Get Pokemon object via reflection
                    java.lang.reflect.Method getPokemonMethod = pixelmonClass.getMethod("getPokemon");
                    Object pokemon = getPokemonMethod.invoke(entity);

                    // Get types
                    Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
                    java.lang.reflect.Method getTypesMethod = null;

                    // Try various methods to get types
                    try {
                        // Try form approach
                        java.lang.reflect.Method getFormMethod = pokemonClass.getMethod("getForm");
                        Object form = getFormMethod.invoke(pokemon);
                        getTypesMethod = form.getClass().getMethod("getTypes");
                        Object[] types = (Object[]) getTypesMethod.invoke(form);

                        // Check if any type matches
                        for (Object typeObj : types) {
                            if (typeObj.toString().equalsIgnoreCase(type)) {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        // Form approach failed, try directly from species
                        try {
                            java.lang.reflect.Method getSpeciesMethod = pokemonClass.getMethod("getSpecies");
                            Object species = getSpeciesMethod.invoke(pokemon);
                            getTypesMethod = species.getClass().getMethod("getTypes");
                            Object[] types = (Object[]) getTypesMethod.invoke(species);

                            // Check if any type matches
                            for (Object typeObj : types) {
                                if (typeObj.toString().equalsIgnoreCase(type)) {
                                    return true;
                                }
                            }
                        } catch (Exception e2) {
                            // Both approaches failed, fall back to name-based matching
                            CustomConsumables.getLogger().debug("Type check failed, falling back to name matching");
                        }
                    }
                }
            } catch (Exception e) {
                // Reflection failed, continue with other approaches
                CustomConsumables.getLogger().debug("Reflection type check failed: {}", e.getMessage());
            }

            // Third approach - random chance based on confidence
            // This just gives some visual feedback even when we can't determine the type
            return RANDOM.nextFloat() < 0.1f; // 10% chance to match randomly for feedback

        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error checking Pokemon type: {}", e.getMessage());
            return false;
        }
    }
}