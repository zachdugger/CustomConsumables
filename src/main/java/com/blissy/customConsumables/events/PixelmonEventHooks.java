package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * This class hooks into Pixelmon events using reflection and event handlers
 * to enforce type attractor effects.
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PixelmonEventHooks {
    private static final Random random = new Random();
    private static boolean initialized = false;
    private static Class<?> pixelmonClass = null;
    private static Method getSpeciesMethod = null;
    private static Method getTypesMethod = null;

    /**
     * Initialize the Pixelmon hooks on startup
     */
    public static void initialize() {
        if (initialized || !PixelmonIntegration.isPixelmonLoaded()) return;

        try {
            // Try to find Pixelmon's Pokémon class
            pixelmonClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");
            Class<?> speciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");

            // Get required methods
            getSpeciesMethod = pixelmonClass.getMethod("getSpecies");
            getTypesMethod = speciesClass.getMethod("getTypes");

            // Register our event listener
            CustomConsumables.getLogger().info("Registering Pixelmon entity spawn listener");
            MinecraftForge.EVENT_BUS.register(PixelmonEventHooks.class);

            initialized = true;
            CustomConsumables.getLogger().info("PixelmonEventHooks initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize PixelmonEventHooks", e);
        }
    }

    /**
     * Event handler that catches Pokémon entities when they spawn
     * and applies our type filtering logic
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!initialized || !PixelmonIntegration.isPixelmonLoaded()) return;

        // Skip client side
        if (event.getWorld().isClientSide()) return;

        // Check if this is a Pixelmon entity
        if (pixelmonClass.isInstance(event.getEntity())) {
            try {
                // Get the entity
                Object pixelmon = event.getEntity();

                // Find the nearest player with a type boost
                ServerWorld world = (ServerWorld) event.getWorld();
                PlayerEntity nearestPlayer = null;
                double nearestDistance = Double.MAX_VALUE;

                for (PlayerEntity player : world.players()) {
                    if (PixelmonIntegration.hasTypeBoost(player)) {
                        double distance = player.distanceToSqr(event.getEntity());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestPlayer = player;
                        }
                    }
                }

                // If no player with a type boost is nearby, allow spawn
                if (nearestPlayer == null || nearestDistance > 50*50) {
                    return;
                }

                // Get the boosted type
                String boostedType = PixelmonIntegration.getBoostedType(nearestPlayer);
                if (boostedType.isEmpty()) return;

                // Get the Pokémon's species and types
                Object species = getSpeciesMethod.invoke(pixelmon);
                Object[] types = (Object[]) getTypesMethod.invoke(species);

                // Check if any of the Pokémon's types match the boosted type
                boolean typeMatches = false;
                for (Object type : types) {
                    if (type.toString().equalsIgnoreCase(boostedType)) {
                        typeMatches = true;
                        break;
                    }
                }

                // Get the multiplier for the type boost
                float multiplier = PixelmonIntegration.getTypeBoostMultiplier(nearestPlayer, 1.0f);

                // Always allow boosted types to spawn
                if (typeMatches) {
                    // Send a notification if the player is close enough
                    if (nearestDistance < 20*20 && nearestPlayer instanceof ServerPlayerEntity) {
                        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) nearestPlayer;
                        String typeName = boostedType.substring(0, 1).toUpperCase() + boostedType.substring(1).toLowerCase();

                        // Only send a message occasionally to avoid spam
                        if (random.nextInt(5) == 0) {
                            serverPlayer.sendMessage(
                                    new StringTextComponent(TextFormatting.GREEN + "Your " + typeName +
                                            " Type Attractor helped spawn a Pokémon!"),
                                    serverPlayer.getUUID()
                            );
                        }
                    }

                    // Always allow the spawn
                    return;
                }

                // For non-boosted types with a high multiplier (5x or greater),
                // block most spawns to make room for the boosted type
                if (multiplier >= 5.0f) {
                    float roll = random.nextFloat();
                    boolean shouldBlock = roll <= 0.85f; // Block 85% of non-boosted types

                    if (shouldBlock) {
                        // Cancel the spawn event
                        event.setCanceled(true);

                        // Debug log
                        CustomConsumables.getLogger().debug(
                                "Blocked non-{} type Pokémon spawn near player {}",
                                boostedType, nearestPlayer.getName().getString()
                        );

                        // Try to spawn a Pokémon of the correct type instead
                        if (random.nextFloat() < 0.3f) { // 30% chance to replace
                            trySpawnReplacementPokemon(nearestPlayer, boostedType);
                        }
                    }
                } else {
                    // For lower multipliers, use a less aggressive approach
                    // Block non-boosted types based on the inverse of the multiplier
                    float roll = random.nextFloat();
                    boolean shouldBlock = roll <= (1.0f - (1.0f / multiplier));

                    if (shouldBlock) {
                        event.setCanceled(true);
                    }
                }

            } catch (Exception e) {
                CustomConsumables.getLogger().debug("Error processing Pixelmon entity spawn: {}", e.getMessage());
            }
        }
    }

    /**
     * Try to spawn a replacement Pokémon of the correct type
     */
    private static void trySpawnReplacementPokemon(PlayerEntity player, String type) {
        if (!(player instanceof ServerPlayerEntity)) return;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (serverPlayer.getServer() == null) return;

        try {
            // Try to spawn a Pokémon of the desired type
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn " + type.toLowerCase()
            );

            // Show visual feedback
            ServerWorld world = serverPlayer.getLevel();
            world.sendParticles(
                    net.minecraft.particles.ParticleTypes.ENTITY_EFFECT,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    5, 0.5, 0.5, 0.5, 0.05
            );

            CustomConsumables.getLogger().debug(
                    "Spawned replacement {} type Pokémon for player {}",
                    type, player.getName().getString()
            );
        } catch (Exception e) {
            // Just log at debug level
            CustomConsumables.getLogger().debug(
                    "Failed to spawn replacement Pokémon: {}", e.getMessage()
            );
        }
    }
}