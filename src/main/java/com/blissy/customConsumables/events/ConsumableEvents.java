package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.Random;

/**
 * Main event handler for CustomConsumables mod
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class ConsumableEvents {
    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;
    private static final int VISUAL_EFFECT_INTERVAL = 200; // Every 10 seconds (200 ticks)

    /**
     * Process player tick events to update effects and show feedback
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        // Update effect durations
        PlayerEffectManager.tickEffects(event.player);

        // Occasionally show visual effects for active boosts
        if (event.player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.player;

            // Increment counter only once per tick (not per player)
            if (serverPlayer.level.getGameTime() % 20 == 0) {
                tickCounter++;
            }

            // Visual effects for type boost at regular intervals
            if (tickCounter % (VISUAL_EFFECT_INTERVAL / 20) == 0) { // Convert ticks to seconds
                if (PlayerEffectManager.hasTypeAttractorEffect(serverPlayer)) {
                    showTypeBoostVisuals(serverPlayer);
                }
            }
        }
    }

    /**
     * Show visual feedback for active type boost
     */
    private static void showTypeBoostVisuals(ServerPlayerEntity player) {
        if (RANDOM.nextFloat() < 0.3f) { // Only 30% chance to avoid spam
            String type = PlayerEffectManager.getTypeAttractorType(player);
            if (type.isEmpty()) return;

            int remainingTicks = PlayerEffectManager.getRemainingTypeBoostTime(player);

            // Only show if significant time remains
            if (remainingTicks > 200) {
                String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
                int remainingSeconds = remainingTicks / 20;

                player.sendMessage(
                        new StringTextComponent(TextFormatting.GREEN +
                                typeName + " Type Attractor: " +
                                TextFormatting.YELLOW + formatTime(remainingTicks) + " remaining"),
                        player.getUUID()
                );

                // Show particles
                showTypeParticles(player, type);

                // Periodically check if a spawn should be forced
                if (RANDOM.nextFloat() < 0.1f) { // 10% chance
                    forceTypeSpawn(player, type);
                }
            }
        }
    }

    /**
     * Force a Pokémon of the specified type to spawn
     */
    private static void forceTypeSpawn(ServerPlayerEntity player, String type) {
        try {
            // Use the PixelmonIntegration class to force a spawn
            boolean success = PixelmonIntegration.forceSpawnType(player, type);

            if (success && RANDOM.nextFloat() < 0.3f) {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.DARK_GREEN + "Your Type Attractor is working..."),
                        player.getUUID()
                );
            }
        } catch (Exception e) {
            // Just log silently - this is just a bonus feature
            CustomConsumables.getLogger().debug("Failed to force spawn: {}", e.getMessage());
        }
    }

    /**
     * Show particles based on type
     */
    private static void showTypeParticles(ServerPlayerEntity player, String type) {
        if (player.level instanceof net.minecraft.world.server.ServerWorld) {
            net.minecraft.world.server.ServerWorld world = (net.minecraft.world.server.ServerWorld) player.level;

            // Choose appropriate particles based on type
            switch(type.toLowerCase()) {
                case "fire":
                    showParticlesAround(world, player, ParticleTypes.FLAME);
                    break;
                case "water":
                    showParticlesAround(world, player, ParticleTypes.DRIPPING_WATER);
                    break;
                case "electric":
                    showParticlesAround(world, player, ParticleTypes.FIREWORK);
                    break;
                case "grass":
                    showParticlesAround(world, player, ParticleTypes.COMPOSTER);
                    break;
                case "dragon":
                    showParticlesAround(world, player, ParticleTypes.DRAGON_BREATH);
                    break;
                default:
                    showParticlesAround(world, player, ParticleTypes.WITCH);
                    break;
            }
        }
    }

    /**
     * Helper method to show particles around a player
     */
    private static void showParticlesAround(net.minecraft.world.server.ServerWorld world, ServerPlayerEntity player, net.minecraft.particles.IParticleData particleType) {
        // Spawn particles in a circle around player
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double radius = 2.0;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            world.sendParticles(
                    particleType,
                    x, player.getY() + 0.5, z,
                    3, 0, 0.1, 0, 0.01
            );
        }
    }

    /**
     * Process player login to show active effects
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

            // Check for active type boost
            if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
                String type = PlayerEffectManager.getTypeAttractorType(player);
                int remainingTicks = PlayerEffectManager.getRemainingTypeBoostTime(player);

                if (!type.isEmpty() && remainingTicks > 0) {
                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                    player.sendMessage(
                            new StringTextComponent(TextFormatting.GREEN +
                                    typeName + " Type Attractor is still active! " +
                                    TextFormatting.YELLOW + formatTime(remainingTicks) + " remaining"),
                            player.getUUID()
                    );

                    // Re-apply the boost via command when player logs in
                    try {
                        // Use PixelmonIntegration class
                        boolean success = PixelmonIntegration.applyTypeBoost(player, type);

                        if (success) {
                            CustomConsumables.getLogger().info("Re-applied {} type boost for player {} after login",
                                    type, player.getName().getString());
                        }
                    } catch (Exception e) {
                        CustomConsumables.getLogger().debug("Failed to re-apply type boost on login: {}", e.getMessage());
                    }
                }
            }

            // Check if Pixelmon is loaded and notify player
            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
            if (!pixelmonLoaded) {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.RED +
                                "Warning: Pixelmon mod is not installed! CustomConsumables items require Pixelmon."),
                        player.getUUID()
                );
            }
        }
    }

    /**
     * Format ticks into a readable time string
     */
    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;

        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }
}