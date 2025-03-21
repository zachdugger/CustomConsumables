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

import java.util.HashMap;
import java.util.Map;
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
    // Player UUID -> Last notification time
    private final ConcurrentHashMap<UUID, Long> lastNotificationTime = new ConcurrentHashMap<>();

    // Track when we last sent a minute notification to each player
    private static final Map<UUID, Integer> lastMinuteNotified = new HashMap<>();

    /**
     * Private constructor for singleton
     */
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
     * Spawn visual particles based on Pokémon type
     */
    public void spawnTypeBoostParticles(ServerPlayerEntity player, String type) {
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
        for (int i = 0; i < 20; i++) {
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
     * Register a type boost for a player without forcing spawns
     */
    public void registerTypeBoost(PlayerEntity player, String type, int duration, float multiplier) {
        // Let the PixelmonIntegration handle storing the effect data
        PixelmonIntegration.applyTypeBoost(player, type, duration, multiplier);

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
                UUID playerID = player.getUUID();

                // Show a periodic reminder, but make sure we don't spam
                if (remainingTicks % 1200 == 0) { // Every minute
                    // Make sure we only notify once at each minute threshold
                    int minutesLeft = remainingTicks / 1200;
                    Integer lastNotified = lastMinuteNotified.get(playerID);

                    if (lastNotified == null || lastNotified != minutesLeft) {
                        String typeName = capitalize(type);

                        if (player instanceof ServerPlayerEntity) {
                            ((ServerPlayerEntity) player).sendMessage(
                                    new StringTextComponent(TextFormatting.GREEN +
                                            typeName + " Type Attractor: " + minutesLeft + " minute(s) remaining"),
                                    player.getUUID()
                            );

                            // Add some particles as a visual reminder
                            manager.spawnTypeBoostParticles((ServerPlayerEntity) player, type);

                            // Update the last notification time
                            lastMinuteNotified.put(playerID, minutesLeft);
                        }
                    }
                }

                // Occasional visual particles to give feedback, but not too often
                if (remainingTicks % 400 == 0 && player instanceof ServerPlayerEntity) {
                    // Add subtle visual feedback
                    if (RANDOM.nextFloat() < 0.3f) {
                        manager.spawnTypeBoostParticles((ServerPlayerEntity) player, type);
                    }
                }

                // Occasional attempt to re-register boost with server
                if (remainingTicks % 1000 == 0) {
                    // Get the current multiplier
                    float currentMultiplier = PixelmonIntegration.getTypeBoostMultiplier(player, 10.0f);

                    // Re-register just to be sure it's still active - use the existing values
                    PixelmonIntegration.applyTypeBoost(player, type, remainingTicks, currentMultiplier);
                }
            } else {
                // Effect expired, remove from notification tracking
                lastMinuteNotified.remove(player.getUUID());
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