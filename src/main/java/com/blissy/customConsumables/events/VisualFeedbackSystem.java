package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
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

/**
 * This class provides visual feedback for the player when effects are active
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class VisualFeedbackSystem {

    private static final Random random = new Random();
    private static final Map<UUID, Integer> activeLureEffects = new HashMap<>();
    private static int tickCounter = 0;

    /**
     * Register a player for visual feedback when a legendary lure is activated
     */
    public static void registerLegendaryLureEffect(PlayerEntity player, int durationTicks) {
        activeLureEffects.put(player.getUUID(), durationTicks);

        // Initial notification
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Play sound
            serverPlayer.level.playSound(null, player.blockPosition(),
                    SoundEvents.END_PORTAL_SPAWN, SoundCategory.PLAYERS,
                    0.5f, 1.5f);

            // Show initial particle burst
            showLegendaryParticles(serverPlayer, 30);

            serverPlayer.sendMessage(new StringTextComponent(
                    TextFormatting.GOLD + "** Legendary Lure activated! **"), player.getUUID());
            serverPlayer.sendMessage(new StringTextComponent(
                    TextFormatting.YELLOW + "Look for particles that show your lure is working!"), player.getUUID());
        }
    }

    /**
     * Register a spawn check intercept with visual feedback
     */
    public static void registerLegendarySpawnCheck(PlayerEntity player, boolean success) {
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            if (success) {
                // Spawn was successful - show special effect
                showLegendarySpawnEffect(serverPlayer);
            } else {
                // Just show normal checking effect
                showLegendaryCheckEffect(serverPlayer);
            }
        }
    }

    /**
     * Show special effect for a successful legendary spawn
     */
    public static void registerLegendarySpawnSuccess(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            showLegendarySpawnSuccessEffect(serverPlayer);
        }
    }

    /**
     * Show particles around a player to indicate lure effect
     */
    private static void showLegendaryParticles(ServerPlayerEntity player, int count) {
        ServerWorld world = player.getLevel();
        BlockPos pos = player.blockPosition();

        for (int i = 0; i < count; i++) {
            double x = pos.getX() + (random.nextDouble() * 2 - 1) * 2;
            double y = pos.getY() + random.nextDouble() * 2;
            double z = pos.getZ() + (random.nextDouble() * 2 - 1) * 2;

            world.sendParticles(ParticleTypes.END_ROD,
                    x, y, z, 1, 0, 0, 0, 0.05);
        }
    }

    /**
     * Show a special effect when a legendary spawn check is happening
     */
    private static void showLegendaryCheckEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getLevel();
        BlockPos pos = player.blockPosition();

        // Blue particles in a circle pattern
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double radius = 2.0;
            double x = pos.getX() + Math.cos(angle) * radius;
            double z = pos.getZ() + Math.sin(angle) * radius;

            world.sendParticles(ParticleTypes.DOLPHIN,
                    x, pos.getY() + 0.5, z, 3, 0, 0.2, 0, 0.1);
        }

        // Play sound
        world.playSound(null, pos,
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS,
                0.15f, 1.8f);

        player.sendMessage(new StringTextComponent(
                TextFormatting.AQUA + "Legendary check detected..."), player.getUUID());
    }

    /**
     * Show a successful legendary spawn effect
     */
    private static void showLegendarySpawnEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getLevel();
        BlockPos pos = player.blockPosition();

        // Golden particles swirling upward
        for (int i = 0; i < 40; i++) {
            double angle = i * 0.5;
            double radius = 3.0 * (1.0 - (i / 40.0));
            double height = i * 0.15;
            double x = pos.getX() + Math.cos(angle) * radius;
            double z = pos.getZ() + Math.sin(angle) * radius;

            // Use a different particle since GLOW doesn't exist in this version
            world.sendParticles(ParticleTypes.FLAME,
                    x, pos.getY() + height, z, 1, 0, 0, 0, 0.01);
        }

        // Play dramatic sound
        world.playSound(null, pos,
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS,
                0.5f, 1.2f);

        player.sendMessage(new StringTextComponent(
                TextFormatting.GOLD + "** LEGENDARY SPAWN ENGAGED! **"), player.getUUID());
    }

    /**
     * Show a super special effect when a legendary is actually spawned
     */
    private static void showLegendarySpawnSuccessEffect(ServerPlayerEntity player) {
        ServerWorld world = player.getLevel();
        BlockPos pos = player.blockPosition();

        // Create a dramatic pillar of particles
        for (int height = 0; height < 20; height++) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                double radius = 2.0;
                double x = pos.getX() + Math.cos(angle) * radius;
                double z = pos.getZ() + Math.sin(angle) * radius;

                // Different particles at different heights
                if (height < 5) {
                    world.sendParticles(ParticleTypes.FLAME,
                            x, pos.getY() + height, z, 1, 0, 0, 0, 0.01);
                } else if (height < 10) {
                    world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            x, pos.getY() + height, z, 1, 0, 0, 0, 0.01);
                } else if (height < 15) {
                    world.sendParticles(ParticleTypes.END_ROD,
                            x, pos.getY() + height, z, 1, 0, 0, 0, 0.01);
                } else {
                    world.sendParticles(ParticleTypes.FIREWORK,
                            x, pos.getY() + height, z, 1, 0, 0, 0, 0.05);
                }
            }
        }

        // Also do a burst around the player
        for (int i = 0; i < 50; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = Math.random() * 4;
            double x = pos.getX() + Math.cos(angle) * radius;
            double y = pos.getY() + Math.random() * 3;
            double z = pos.getZ() + Math.sin(angle) * radius;

            world.sendParticles(ParticleTypes.FIREWORK,
                    x, y, z, 1, 0, 0, 0, 0.1);
        }

        // Play dramatic sounds
        world.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 0.8f);
        world.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.3f, 1.2f);

        player.sendMessage(new StringTextComponent(
                TextFormatting.GOLD + "***** LEGENDARY POKEMON SPAWNED! *****"), player.getUUID());
        player.sendMessage(new StringTextComponent(
                TextFormatting.YELLOW + "Your lure helped attract this powerful Pokémon!"), player.getUUID());
    }

    /**
     * Tick handler for visual effects
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        tickCounter++;

        // Only process visual effects every 20 ticks (1 second)
        if (tickCounter % 20 == 0) {
            // Get the server instance
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            activeLureEffects.entrySet().removeIf(entry -> {
                UUID playerUUID = entry.getKey();
                int timeLeft = entry.getValue();

                ServerPlayerEntity player = server.getPlayerList().getPlayer(playerUUID);

                if (player != null && PlayerEffectManager.hasLegendaryLureEffect(player)) {
                    // Show ambient particles
                    if (random.nextInt(3) == 0) { // Only 1/3 chance to show particles each second
                        showLegendaryParticles(player, 5);
                    }

                    // Update time left
                    timeLeft -= 20;
                    if (timeLeft <= 0) {
                        return true; // Remove from map
                    } else {
                        entry.setValue(timeLeft);
                        return false;
                    }
                }

                return true; // Remove if player not found or effect not active
            });
        }
    }
}