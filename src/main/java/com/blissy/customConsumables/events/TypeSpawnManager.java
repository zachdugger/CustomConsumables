package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages type-based spawn filtering and boosting
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class TypeSpawnManager {
    private static TypeSpawnManager instance;
    private static final Random RANDOM = new Random();

    // Type boost tracking
    // Player UUID -> Last force spawn time
    private final ConcurrentHashMap<UUID, Long> lastForceSpawnTime = new ConcurrentHashMap<>();
    private static final long FORCE_SPAWN_COOLDOWN = 8000; // 8 second cooldown

    // Spawn rate control
    private static final int MIN_SPAWN_INTERVAL = 20 * 10; // 10 seconds
    private static final int MAX_SPAWN_INTERVAL = 20 * 30; // 30 seconds
    private int nextSpawnTick = MIN_SPAWN_INTERVAL;

    // Private constructor for singleton
    private TypeSpawnManager() { }

    /**
     * Get the singleton instance
     */
    public static TypeSpawnManager getInstance() {
        if (instance == null) {
            instance = new TypeSpawnManager();
        }
        return instance;
    }

    /**
     * Try to force spawn a Pokémon of the specified type
     */
    public boolean tryForceSpawn(PlayerEntity player, String type) {
        if (!PixelmonIntegration.isPixelmonLoaded()) return false;

        // Check cooldown
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (lastForceSpawnTime.containsKey(playerId)) {
            long lastSpawn = lastForceSpawnTime.get(playerId);
            if (currentTime - lastSpawn < FORCE_SPAWN_COOLDOWN) {
                return false; // Still in cooldown
            }
        }

        // Get the server
        MinecraftServer server = player instanceof ServerPlayerEntity
                ? ((ServerPlayerEntity) player).getServer()
                : ServerLifecycleHooks.getCurrentServer();

        if (server == null) return false;

        // Try to spawn
        boolean success = PixelmonIntegration.forceSpawnType(player, type);

        // If spawn successful, update cooldown
        if (success) {
            lastForceSpawnTime.put(playerId, currentTime);

            // Generate visual effect
            if (player instanceof ServerPlayerEntity) {
                spawnTypeBoostParticles((ServerPlayerEntity)player, type);
            }

            // Log the spawn attempt
            CustomConsumables.getLogger().debug(
                    "Forced spawn of type {} for player {}",
                    type, player.getName().getString()
            );
        }

        return success;
    }

    /**
     * Spawn visual particles based on Pokémon type
     */
    private void spawnTypeBoostParticles(ServerPlayerEntity player, String type) {
        if (player == null || player.level == null) return;

        ServerWorld world = player.getLevel();
        BasicParticleType particleType;

        // Choose particles based on type
        switch(type.toLowerCase()) {
            case "fire":
                particleType = ParticleTypes.FLAME;
                break;
            case "water":
                particleType = ParticleTypes.DRIPPING_WATER;
                break;
            case "electric":
                particleType = ParticleTypes.FIREWORK;
                break;
            case "grass":
                particleType = ParticleTypes.COMPOSTER;
                break;
            case "ice":
                particleType = ParticleTypes.ITEM_SNOWBALL;
                break;
            case "dragon":
                particleType = ParticleTypes.DRAGON_BREATH;
                break;
            default:
                particleType = ParticleTypes.WITCH;
                break;
        }

        // Create a spiral pattern
        for (int i = 0; i < 30; i++) {
            double angle = i * 0.5;
            double radius = 2.0 * Math.sin(i * 0.1);
            double height = i * 0.05;

            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            world.sendParticles(
                    particleType,
                    x, player.getY() + height, z,
                    1, 0, 0, 0, 0.01
            );
        }
    }

    /**
     * Register a type boost for a player and immediately try to spawn
     * FIX: Changed method signature to remove the eligiblePokemon parameter
     */
    public void registerTypeBoost(PlayerEntity player, String type, int durationTicks, float multiplier) {
        // Let the PixelmonIntegration handle storing the effect data
        PixelmonIntegration.applyTypeBoost(player, type, durationTicks, multiplier);

        // Try an immediate spawn
        tryForceSpawn(player, type);

        // Log the registration
        CustomConsumables.getLogger().info(
                "Registered {} type boost for player {} with {}x multiplier",
                type, player.getName().getString(), multiplier
        );
    }

    /**
     * Try to spawn a Pokémon for player periodically
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        PlayerEntity player = event.player;
        TypeSpawnManager manager = getInstance();

        // Check for active type boost
        if (PixelmonIntegration.hasTypeBoost(player)) {
            String type = PixelmonIntegration.getBoostedType(player);
            int remainingTicks = PixelmonIntegration.getTypeBoostDuration(player);

            // If effect is still active
            if (remainingTicks > 0) {
                // Show a periodic reminder
                if (remainingTicks % 1200 == 0) { // Every minute
                    int minutesLeft = remainingTicks / 1200;
                    String typeName = capitalize(type);

                    if (player instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) player).sendMessage(
                                new StringTextComponent(TextFormatting.GREEN +
                                        typeName + " Type Attractor: " + minutesLeft + " minute(s) remaining"),
                                player.getUUID()
                        );

                        // Add some particles as a visual reminder
                        manager.spawnTypeBoostParticles((ServerPlayerEntity) player, type);
                    }
                }

                // Periodic spawning
                if (remainingTicks % manager.nextSpawnTick == 0) {
                    boolean spawned = manager.tryForceSpawn(player, type);

                    // Adjust next spawn time based on success
                    if (spawned) {
                        // If we successfully spawned, wait a bit longer
                        manager.nextSpawnTick = MIN_SPAWN_INTERVAL + RANDOM.nextInt(MAX_SPAWN_INTERVAL - MIN_SPAWN_INTERVAL);
                    } else {
                        // If spawn failed, try again sooner
                        manager.nextSpawnTick = MIN_SPAWN_INTERVAL;
                    }
                }
            }
        }
    }

    /**
     * Capitalize the first letter of a string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}