package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles type filtering effects for players
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class TypeFilterHandler {
    // Constants
    private static final String TYPE_FILTER_KEY = "typeFilter";
    private static final String TYPE_FILTER_DURATION_KEY = "typeFilterDuration";

    // Cache of active effects
    private static final Map<UUID, Long> activeTypeFilters = new HashMap<>();

    // Tick counter for less frequent updates
    private static int tickCounter = 0;

    /**
     * Apply a type filter to a player
     */
    public static void applyTypeFilter(PlayerEntity player, String type, int durationTicks) {
        CompoundNBT playerData = player.getPersistentData();
        CompoundNBT customData;

        if (playerData.contains(CustomConsumables.MOD_ID)) {
            customData = playerData.getCompound(CustomConsumables.MOD_ID);
        } else {
            customData = new CompoundNBT();
        }

        // Store type filter data
        customData.putString(TYPE_FILTER_KEY, type.toLowerCase());
        customData.putInt(TYPE_FILTER_DURATION_KEY, durationTicks);

        // Save back to player
        playerData.put(CustomConsumables.MOD_ID, customData);

        // Update cache
        activeTypeFilters.put(player.getUUID(), System.currentTimeMillis() + (durationTicks * 50L)); // 50ms per tick

        // Log application
        CustomConsumables.getLogger().info(
                "Applied {} type filter to player {} for {} ticks",
                type, player.getName().getString(), durationTicks
        );
    }

    /**
     * Check if a player has an active type filter
     */
    public static boolean hasTypeFilter(PlayerEntity player) {
        UUID playerId = player.getUUID();

        // Check cache first for quick response
        if (activeTypeFilters.containsKey(playerId)) {
            long expiryTime = activeTypeFilters.get(playerId);

            if (System.currentTimeMillis() > expiryTime) {
                // Filter expired, remove from cache
                activeTypeFilters.remove(playerId);
                return false;
            }

            return true;
        }

        // Check NBT data as fallback
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT customData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (customData.contains(TYPE_FILTER_DURATION_KEY)) {
                int duration = customData.getInt(TYPE_FILTER_DURATION_KEY);
                if (duration > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get the type filter for a player
     */
    public static String getTypeFilter(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT customData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (customData.contains(TYPE_FILTER_KEY)) {
                return customData.getString(TYPE_FILTER_KEY);
            }
        }

        return "";
    }

    /**
     * Get the remaining duration of a type filter in ticks
     */
    public static int getTypeFilterDuration(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT customData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (customData.contains(TYPE_FILTER_DURATION_KEY)) {
                return customData.getInt(TYPE_FILTER_DURATION_KEY);
            }
        }

        return 0;
    }

    /**
     * Update type filter durations and remove expired filters
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only process on server side at the end of a tick
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        PlayerEntity player = event.player;
        CompoundNBT playerData = player.getPersistentData();

        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT customData = playerData.getCompound(CustomConsumables.MOD_ID);

            if (customData.contains(TYPE_FILTER_DURATION_KEY)) {
                int duration = customData.getInt(TYPE_FILTER_DURATION_KEY);

                if (duration > 0) {
                    // Decrement duration
                    duration--;
                    customData.putInt(TYPE_FILTER_DURATION_KEY, duration);
                    playerData.put(CustomConsumables.MOD_ID, customData);

                    // Show effect visually every 10 seconds
                    if (duration % 200 == 0) {
                        String type = customData.getString(TYPE_FILTER_KEY);
                        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                        if (player instanceof ServerPlayerEntity) {
                            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                            // Add visual particles
                            serverPlayer.getLevel().sendParticles(
                                    net.minecraft.particles.ParticleTypes.ENTITY_EFFECT,
                                    player.getX(), player.getY() + 0.8, player.getZ(),
                                    5, 0.2, 0.2, 0.2, 0.02
                            );
                        }
                    }

                    // Notify when effect is about to expire (20 seconds left)
                    if (duration == 400) {
                        if (player instanceof ServerPlayerEntity) {
                            String type = customData.getString(TYPE_FILTER_KEY);
                            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                            ((ServerPlayerEntity) player).displayClientMessage(
                                    new StringTextComponent(TextFormatting.YELLOW +
                                            "Your " + typeName + " Type Attractor is about to expire!"),
                                    true
                            );
                        }
                    }

                    // Notify when effect expires
                    if (duration == 0) {
                        if (player instanceof ServerPlayerEntity) {
                            String type = customData.getString(TYPE_FILTER_KEY);
                            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                            ((ServerPlayerEntity) player).displayClientMessage(
                                    new StringTextComponent(TextFormatting.RED +
                                            "Your " + typeName + " Type Attractor has expired!"),
                                    true
                            );

                            // Log expiration
                            CustomConsumables.getLogger().info(
                                    "{} type filter expired for player {}",
                                    typeName, player.getName().getString()
                            );

                            // Remove the filter from cache
                            activeTypeFilters.remove(player.getUUID());

                            // Remove the filter from NBT
                            customData.remove(TYPE_FILTER_KEY);
                            customData.remove(TYPE_FILTER_DURATION_KEY);
                            playerData.put(CustomConsumables.MOD_ID, customData);
                        }
                    }
                }
            }
        }
    }
}