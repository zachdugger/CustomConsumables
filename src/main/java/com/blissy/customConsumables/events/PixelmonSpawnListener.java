package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for Pixelmon spawns to apply effect boosts from Custom Consumables items
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PixelmonSpawnListener {
    private static final Random RANDOM = new Random();

    // Cache to prevent checking the same entity multiple times
    private static final ConcurrentHashMap<Integer, Long> processedEntities = new ConcurrentHashMap<>();

    // Cooldown tracking for spawns
    private static final Map<UUID, Long> spawnCooldowns = new HashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 8000; // 8 second cooldown

    // Periodic spawn settings
    private static final int SPAWN_CHECK_INTERVAL = 200; // 10 seconds (in ticks)
    private static int tickCounter = 0;

    /**
     * Process natural entity spawns to apply our effects
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
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
            if (processedEntities.size() > 500) {
                cleanupProcessedEntities();
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

            // Handle the Pokémon spawn based on player effects
            handlePokemonSpawn(event, nearestPlayer);

        } catch (Exception e) {
            // Log but don't crash
            CustomConsumables.getLogger().error("Error in onEntitySpawn: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Periodic tick to manage effects and try to spawn Pokémon
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !PixelmonIntegration.isPixelmonLoaded()) {
            return;
        }

        tickCounter++;

        // Check every SPAWN_CHECK_INTERVAL ticks (10 seconds)
        if (tickCounter % SPAWN_CHECK_INTERVAL == 0) {
            // Get the server instance using ServerLifecycleHooks
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            // Every 10 minutes (12000 ticks), print debug info about active effects
            if (tickCounter % 12000 == 0) {
                printDebugInfo(server);
            }
        }
    }

    /**
     * Print debug information about active effects
     */
    private static void printDebugInfo(net.minecraft.server.MinecraftServer server) {
        CustomConsumables.getLogger().info("=== TYPE ATTRACTOR DEBUG INFO ===");

        int playersWithTypeBoost = 0;

        for (PlayerEntity player : server.getPlayerList().getPlayers()) {
            if (PixelmonIntegration.hasTypeBoost(player)) {
                playersWithTypeBoost++;

                String type = PixelmonIntegration.getBoostedType(player);
                int timeLeft = PixelmonIntegration.getTypeBoostDuration(player);
                float multiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 1.0f);

                CustomConsumables.getLogger().info("Player {} has {} type boost with {}x multiplier ({} seconds remaining)",
                        player.getName().getString(),
                        type,
                        multiplier,
                        timeLeft / 20);
            }
        }

        CustomConsumables.getLogger().info("Total players with active type boost: {}", playersWithTypeBoost);
        CustomConsumables.getLogger().info("===================================");
    }

    /**
     * Process a Pokémon spawn based on player effects
     */
    private static void handlePokemonSpawn(EntityJoinWorldEvent event, PlayerEntity player) {
        // Get the Pokémon's name if possible
        String pokemonName = getPokemonName(event.getEntity());

        // Handle type boost effect if active
        if (PixelmonIntegration.hasTypeBoost(player)) {
            String boostedType = PixelmonIntegration.getBoostedType(player);

            // Attempt to check the Pokémon's type (might fail due to reflection)
            boolean matchesType = false;
            boolean typeCheckSuccess = false;

            // First try our own type checking with detailed debugging
            try {
                CustomConsumables.getLogger().debug("Checking if {} matches type {}", pokemonName, boostedType);

                // Get entity class and available methods for debugging
                Class<?> entityClass = event.getEntity().getClass();
                CustomConsumables.getLogger().debug("Entity class: {}", entityClass.getName());

                matchesType = doesPokemonMatchType(event.getEntity(), boostedType);
                typeCheckSuccess = true;

                CustomConsumables.getLogger().debug("Type check result: {} matches {}: {}",
                        pokemonName, boostedType, matchesType);
            } catch (Exception e) {
                // Log detailed error information
                CustomConsumables.getLogger().error("Type check failed for {}: {}", pokemonName, e.getMessage());
                CustomConsumables.getLogger().error("Entity class: {}", event.getEntity().getClass().getName());
                e.printStackTrace();

                // We don't want to block spawns due to technical errors
                // Let Pixelmon's natural spawn system handle filtering
                typeCheckSuccess = false;
            }

            // Only modify spawns if we were able to successfully check the type
            if (typeCheckSuccess && !matchesType) {
                // This Pokémon doesn't match our type filter, let Pixelmon handle it
                // Don't cancel the event - the internal Pixelmon system will already be boosting the correct types
                CustomConsumables.getLogger().debug(
                        "Detected non-matching type: {} (not a {} type) for player {}. " +
                                "Pixelmon's natural spawn system should be handling the boost.",
                        pokemonName, boostedType, player.getName().getString()
                );
            } else if (matchesType) {
                // This Pokémon matches our boosted type!
                // Add visual feedback only
                if (player instanceof ServerPlayerEntity) {
                    // Add some particles
                    if (event.getWorld() instanceof ServerWorld && RANDOM.nextFloat() < 0.4f) {
                        BlockPos pos = event.getEntity().blockPosition();
                        ((ServerWorld) event.getWorld()).sendParticles(
                                net.minecraft.particles.ParticleTypes.HAPPY_VILLAGER,
                                pos.getX(), pos.getY() + 0.5, pos.getZ(),
                                5, 0.5, 0.5, 0.5, 0.0
                        );
                    }

                    // Notify the player occasionally
                    if (RANDOM.nextFloat() < 0.3f) {
                        player.sendMessage(
                                new StringTextComponent(TextFormatting.GREEN +
                                        "Your Type Attractor attracted a " + pokemonName + "!"),
                                player.getUUID()
                        );
                    }
                }
            }
        }

        // Handle shiny boost if active
        if (PixelmonIntegration.hasShinyBoost(player)) {
            float shinyChance = PixelmonIntegration.getShinyChance(player, 0);

            // Roll for shiny
            if (RANDOM.nextFloat() * 100 < shinyChance) {
                // Try to make the Pokémon shiny
                makeShiny(event.getEntity());

                // Notify the player
                if (player instanceof ServerPlayerEntity) {
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.AQUA +
                                    "★ Your Shiny Charm activated! A shiny " +
                                    pokemonName + " has spawned! ★"),
                            player.getUUID()
                    );
                }
            }
        }
    }

    /**
     * Enhanced method to check if a Pokémon matches a type with detailed debugging
     */
    private static boolean doesPokemonMatchType(net.minecraft.entity.Entity entity, String targetType) {
        try {
            // Try each approach with detailed logging
            CustomConsumables.getLogger().debug("Attempting to check if entity {} matches type {}",
                    entity.getDisplayName().getString(), targetType);

            // Method 1: Try using Form approach (Pixelmon 9.1.x)
            try {
                Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

                if (pixelmonClass.isInstance(entity)) {
                    // Get Pokemon object
                    Method getPokemonMethod = pixelmonClass.getMethod("getPokemon");
                    Object pokemon = getPokemonMethod.invoke(entity);

                    // Try to get the form
                    Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");

                    // Debug the Pokemon object methods
                    Method[] pokemonMethods = pokemonClass.getMethods();
                    CustomConsumables.getLogger().debug("Pokemon object methods:");
                    for (Method method : pokemonMethods) {
                        if (method.getName().contains("Form") ||
                                method.getName().contains("Type") ||
                                method.getName().contains("Species")) {
                            CustomConsumables.getLogger().debug("  - {}", method.getName());
                        }
                    }

                    Method getFormMethod = pokemonClass.getMethod("getForm");
                    Object form = getFormMethod.invoke(pokemon);

                    // Debug form object methods
                    Class<?> formClass = form.getClass();
                    Method[] formMethods = formClass.getMethods();
                    CustomConsumables.getLogger().debug("Form object methods:");
                    for (Method method : formMethods) {
                        if (method.getName().contains("Type")) {
                            CustomConsumables.getLogger().debug("  - {}", method.getName());
                        }
                    }

                    // Get the types from the form
                    Method getTypesMethod = formClass.getMethod("getTypes");
                    Object[] types = (Object[]) getTypesMethod.invoke(form);

                    // Debug the types
                    CustomConsumables.getLogger().debug("Found {} types: {}", types.length, java.util.Arrays.toString(types));

                    // Check if any type matches our target
                    for (Object type : types) {
                        CustomConsumables.getLogger().debug("Comparing {} with {}", type.toString(), targetType);
                        if (type.toString().equalsIgnoreCase(targetType)) {
                            return true;
                        }
                    }

                    return false;
                }
            } catch (Exception e) {
                CustomConsumables.getLogger().warn("Method 1 (Form approach) failed: {}", e.getMessage());
            }

            // Method 2: Try Species approach
            try {
                Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

                if (pixelmonClass.isInstance(entity)) {
                    // Get Pokemon object
                    Method getPokemonMethod = pixelmonClass.getMethod("getPokemon");
                    Object pokemon = getPokemonMethod.invoke(entity);

                    // Get species
                    Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
                    Method getSpeciesMethod = pokemonClass.getMethod("getSpecies");
                    Object species = getSpeciesMethod.invoke(pokemon);

                    // Debug species object
                    CustomConsumables.getLogger().debug("Species: {}", species);

                    // Try to get types from species
                    Class<?> speciesClass = species.getClass();
                    Method[] speciesMethods = speciesClass.getMethods();

                    // Find methods related to types
                    Method typeMethod = null;
                    for (Method method : speciesMethods) {
                        if (method.getName().contains("Type") && method.getParameterCount() == 0) {
                            CustomConsumables.getLogger().debug("Found potential type method: {}", method.getName());
                            typeMethod = method;

                            // Prefer getTypes if available
                            if (method.getName().equals("getTypes")) {
                                break;
                            }
                        }
                    }

                    if (typeMethod != null) {
                        Object result = typeMethod.invoke(species);

                        if (result instanceof Object[]) {
                            Object[] types = (Object[]) result;
                            CustomConsumables.getLogger().debug("Found types: {}", java.util.Arrays.toString(types));

                            for (Object type : types) {
                                if (type.toString().equalsIgnoreCase(targetType)) {
                                    return true;
                                }
                            }
                        } else {
                            CustomConsumables.getLogger().debug("Type method returned: {} ({})",
                                    result, result.getClass().getName());
                        }
                    }
                }
            } catch (Exception e) {
                CustomConsumables.getLogger().warn("Method 2 (Species approach) failed: {}", e.getMessage());
            }

            // If all approaches fail, just check the name
            // Let Pixelmon's natural spawn system handle the actual filtering
            CustomConsumables.getLogger().warn("All type checking methods failed for {}", entity.getDisplayName().getString());
            return false;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Critical error checking Pokémon type: {}", e.getMessage());
            e.printStackTrace();
            return false;
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
            if (PixelmonIntegration.hasTypeBoost(player) ||
                    PixelmonIntegration.hasShinyBoost(player) ||
                    PixelmonIntegration.hasLegendaryBoost(player)) {

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
     * Get the name of a Pokémon entity
     */
    private static String getPokemonName(net.minecraft.entity.Entity entity) {
        String displayName = entity.getDisplayName().getString();

        // Simple sanitization to get just the Pokémon name
        if (displayName != null && !displayName.isEmpty()) {
            // Remove any level indicators or other text
            if (displayName.contains("Lv.")) {
                displayName = displayName.split("Lv.")[0].trim();
            }
            return displayName;
        }

        return "Pokémon";
    }

    /**
     * Try to make a Pokémon entity shiny
     */
    private static void makeShiny(net.minecraft.entity.Entity entity) {
        try {
            Class<?> pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

            if (pixelmonClass.isInstance(entity)) {
                // Get the Pokemon object
                Method getPokemonMethod = pixelmonClass.getMethod("getPokemon");
                Object pokemon = getPokemonMethod.invoke(entity);

                // Set shiny
                Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
                Method setShinyMethod = pokemonClass.getMethod("setShiny", boolean.class);
                setShinyMethod.invoke(pokemon, true);

                CustomConsumables.getLogger().info("Successfully made Pokémon shiny");
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error making Pokémon shiny: {}", e.getMessage());
        }
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
     * Clean up the processed entities cache
     */
    private static void cleanupProcessedEntities() {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - 60000; // 1 minute

        processedEntities.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }
}