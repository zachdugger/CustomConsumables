package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * Handler for managing type boost effects
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class TypeBoostHandler {

    // Tick interval for visual effects
    private static final int EFFECT_INTERVAL = 200; // Every 10 seconds

    /**
     * Check if a player has an active type boost
     */
    public static boolean hasActiveBoost(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (modData.contains("boostDuration")) {
                int duration = modData.getInt("boostDuration");
                return duration > 0;
            }
        }
        return false;
    }

    /**
     * Get the type that is currently being boosted
     */
    public static String getBoostedType(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (modData.contains("boostedType")) {
                return modData.getString("boostedType");
            }
        }
        return "";
    }

    /**
     * Get the remaining duration of the boost in ticks
     */
    public static int getBoostDuration(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (modData.contains("boostDuration")) {
                return modData.getInt("boostDuration");
            }
        }
        return 0;
    }

    /**
     * Update type boost durations and provide visual feedback
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        PlayerEntity player = event.player;

        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);

            if (modData.contains("boostDuration") && modData.contains("boostedType")) {
                int duration = modData.getInt("boostDuration");
                String type = modData.getString("boostedType");

                if (duration > 0) {
                    // Decrement duration
                    duration--;
                    modData.putInt("boostDuration", duration);
                    playerData.put(CustomConsumables.MOD_ID, modData);

                    // Show visual effects occasionally
                    if (duration % EFFECT_INTERVAL == 0 && duration > 0) {
                        if (player instanceof ServerPlayerEntity) {
                            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                            ServerWorld serverWorld = serverPlayer.getLevel();

                            // Show particles
                            serverWorld.sendParticles(
                                    net.minecraft.particles.ParticleTypes.ENTITY_EFFECT,
                                    player.getX(), player.getY() + 1, player.getZ(),
                                    10, 0.5, 0.5, 0.5, 0.01
                            );

                            // Show message if sufficiently far into the effect
                            if (duration % (EFFECT_INTERVAL * 2) == 0) {
                                String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
                                float multiplier = modData.getFloat("boostMultiplier");

                                player.sendMessage(
                                        new StringTextComponent(TextFormatting.GREEN + typeName + " Type Attractor is active! " +
                                                TextFormatting.YELLOW + "(" + (duration / 20) + " seconds remaining)"),
                                        player.getUUID()
                                );
                            }
                        }
                    }

                    // Final notification when the effect expires
                    if (duration == 0 && player instanceof ServerPlayerEntity) {
                        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                        player.sendMessage(
                                new StringTextComponent(TextFormatting.RED + typeName + " Type Attractor has expired!"),
                                player.getUUID()
                        );

                        // Try to remove boost via command
                        if (((ServerPlayerEntity) player).getServer() != null) {
                            try {
                                ((ServerPlayerEntity) player).getServer().getCommands().performCommand(
                                        ((ServerPlayerEntity) player).getServer().createCommandSourceStack().withPermission(4),
                                        "pokespawn clearboost"
                                );
                            } catch (Exception e) {
                                // Just log and continue
                                CustomConsumables.getLogger().debug("Could not clear boost via command: {}", e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle player login to restore boosts
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();

        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);

            if (modData.contains("boostDuration") && modData.contains("boostedType")) {
                int duration = modData.getInt("boostDuration");
                String type = modData.getString("boostedType");
                float multiplier = modData.getFloat("boostMultiplier");

                if (duration > 0) {
                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                    player.sendMessage(
                            new StringTextComponent(TextFormatting.GREEN + typeName + " Type Attractor is still active! " +
                                    TextFormatting.YELLOW + "(" + (duration / 20) + " seconds remaining)"),
                            player.getUUID()
                    );

                    // Try to reapply the boost via command
                    if (player instanceof ServerPlayerEntity && ((ServerPlayerEntity) player).getServer() != null) {
                        try {
                            ((ServerPlayerEntity) player).getServer().getCommands().performCommand(
                                    ((ServerPlayerEntity) player).getServer().createCommandSourceStack().withPermission(4),
                                    "pokespawn boosttype " + type.toLowerCase() + " " + multiplier
                            );
                        } catch (Exception e) {
                            // Just log and continue
                            CustomConsumables.getLogger().debug("Could not reapply boost via command: {}", e.getMessage());
                        }
                    }

                    CustomConsumables.getLogger().info(
                            "Restored {} type boost for player {} with {} ticks remaining",
                            type, player.getName().getString(), duration
                    );
                }
            }
        }
    }
}