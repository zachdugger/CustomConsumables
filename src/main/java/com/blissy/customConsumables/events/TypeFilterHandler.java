package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Handles type filtering effects for players
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class TypeFilterHandler {
    // Constants
    private static final String TYPE_FILTER_KEY = "typeFilter";
    private static final String TYPE_FILTER_DURATION_KEY = "typeFilterDuration";
    private static final String TYPE_FILTER_MULTIPLIER_KEY = "typeFilterMultiplier";
    private static final Random random = new Random();

    // Cache of active effects
    private static final Map<UUID, Long> activeTypeFilters = new HashMap<>();

    // Track spawn attempts to prevent spamming
    private static final Map<UUID, Long> lastSpawnAttempts = new HashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 5000; // 5 seconds

    // Tick counter for less frequent updates
    private static int tickCounter = 0;

    /**
     * Apply a type filter to a player
     */
    public static void applyTypeFilter(PlayerEntity player, String type, int durationTicks, float multiplier) {
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
        customData.putFloat(TYPE_FILTER_MULTIPLIER_KEY, multiplier);

        // Save back to player
        playerData.put(CustomConsumables.MOD_ID, customData);

        // Update cache
        activeTypeFilters.put(player.getUUID(), System.currentTimeMillis() + (durationTicks * 50L)); // 50ms per tick

        // Log application
        CustomConsumables.getLogger().info(
                "Applied {} type filter to player {} for {} ticks with {}x multiplier",
                type, player.getName().getString(), durationTicks, multiplier
        );

        // Add visual effect
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            ServerWorld world = serverPlayer.getLevel();

            // Create dramatic spiral effect
            double radius = 1.0;
            double height = 0.5;

            for (int i = 0; i < 60; i++) {
                double angle = i * 0.2;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;

                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.WITCH,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );

                radius += 0.05;
                height += 0.03;

                // Add some randomness to make it look more magical
                if (i % 5 == 0) {
                    world.sendParticles(
                            net.minecraft.particles.ParticleTypes.ENCHANT,
                            player.getX() + (random.nextFloat() * 2 - 1),
                            player.getY() + random.nextFloat() * 2,
                            player.getZ() + (random.nextFloat() * 2 - 1),
                            1, 0, 0, 0, 0.1
                    );
                }
            }

            // Play sound
            world.playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.util.SoundEvents.ENCHANTMENT_TABLE_USE,
                    net.minecraft.util.SoundCategory.PLAYERS,
                    1.0F, 1.0F
            );

            // Notification message
            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
            serverPlayer.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + "Type Attractor activated! " +
                            TextFormatting.AQUA + typeName + " type Pokémon " +
                            TextFormatting.GREEN + "will spawn more frequently for " +
                            (durationTicks / 20) + " seconds!"),
                    player.getUUID()
            );

            // Force an immediate spawn to show it's working
            forceSpawnTypePokemon(serverPlayer, type);
        }
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
     * Get the type filter multiplier for a player
     */
    public static float getTypeFilterMultiplier(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT customData = playerData.getCompound(CustomConsumables.MOD_ID);
            if (customData.contains(TYPE_FILTER_MULTIPLIER_KEY)) {
                return customData.getFloat(TYPE_FILTER_MULTIPLIER_KEY);
            }
        }

        return 1.0f; // Default multiplier
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

            if (customData.contains(TYPE_FILTER_KEY) && customData.contains(TYPE_FILTER_DURATION_KEY)) {
                int duration = customData.getInt(TYPE_FILTER_DURATION_KEY);
                String type = customData.getString(TYPE_FILTER_KEY);

                if (duration > 0) {
                    // Every 100 ticks (5 seconds), actively promote spawns of the desired type
                    if (duration % 100 == 0 && player instanceof ServerPlayerEntity) {
                        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                        ServerWorld world = serverPlayer.getLevel();

                        // Add particles to show it's working
                        world.sendParticles(
                                net.minecraft.particles.ParticleTypes.WITCH,
                                player.getX(), player.getY() + 0.5, player.getZ(),
                                10, 1.0, 0.5, 1.0, 0.05
                        );

                        // Try to force spawn the type directly
                        if (ModList.get().isLoaded("pixelmon")) {
                            // Check if we're not in cooldown
                            UUID playerId = player.getUUID();
                            long currentTime = System.currentTimeMillis();

                            if (!lastSpawnAttempts.containsKey(playerId) ||
                                    (currentTime - lastSpawnAttempts.get(playerId)) > SPAWN_COOLDOWN_MS) {

                                // Try spawning the specific type
                                forceSpawnTypePokemon(serverPlayer, type);

                                // Update cooldown
                                lastSpawnAttempts.put(playerId, currentTime);
                            }
                        }
                    }

                    // Notification reminders at decreasing intervals
                    if ((duration == 1200 || // 1 minute left
                            duration == 600 ||  // 30 seconds left
                            duration == 200) && // 10 seconds left
                            player instanceof ServerPlayerEntity) {

                        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                        int secondsLeft = duration / 20;
                        TextFormatting color = duration > 600 ? TextFormatting.GREEN :
                                (duration > 200 ? TextFormatting.YELLOW : TextFormatting.RED);

                        serverPlayer.sendMessage(
                                new StringTextComponent(color + typeName + " Type Attractor: " +
                                        secondsLeft + " seconds remaining"),
                                player.getUUID()
                        );
                    }

                    // Decrement duration
                    duration--;
                    customData.putInt(TYPE_FILTER_DURATION_KEY, duration);
                    playerData.put(CustomConsumables.MOD_ID, customData);

                    // Notify when effect expires
                    if (duration == 0) {
                        if (player instanceof ServerPlayerEntity) {
                            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

                            ((ServerPlayerEntity) player).sendMessage(
                                    new StringTextComponent(TextFormatting.RED +
                                            "Your " + typeName + " Type Attractor has expired!"),
                                    player.getUUID()
                            );

                            // Log expiration
                            CustomConsumables.getLogger().info(
                                    "{} type filter expired for player {}",
                                    typeName, player.getName().getString()
                            );

                            // Remove the filter from cache
                            activeTypeFilters.remove(player.getUUID());
                        }
                    }
                }
            }
        }
    }

    /**
     * Try to spawn specific examples of the given type
     * This helps in case the generic type spawn doesn't work
     */
    private static void trySpawnSpecificTypeExamples(MinecraftServer server, String type) {
        // Try to spawn common Pokémon of each type
        // This is a fallback in case the generic command doesn't work
        switch (type.toLowerCase()) {
            case "normal":
                tryCommand(server, "pokespawn pidgey");
                tryCommand(server, "pokespawn rattata");
                break;
            case "fire":
                tryCommand(server, "pokespawn charmander");
                tryCommand(server, "pokespawn growlithe");
                break;
            case "water":
                tryCommand(server, "pokespawn squirtle");
                tryCommand(server, "pokespawn magikarp");
                break;
            case "grass":
                tryCommand(server, "pokespawn bulbasaur");
                tryCommand(server, "pokespawn oddish");
                break;
            case "electric":
                tryCommand(server, "pokespawn pikachu");
                tryCommand(server, "pokespawn electabuzz");
                break;
            case "ice":
                tryCommand(server, "pokespawn seel");
                tryCommand(server, "pokespawn sneasel");
                break;
            case "fighting":
                tryCommand(server, "pokespawn machop");
                tryCommand(server, "pokespawn mankey");
                break;
            case "poison":
                tryCommand(server, "pokespawn ekans");
                tryCommand(server, "pokespawn grimer");
                break;
            case "ground":
                tryCommand(server, "pokespawn diglett");
                tryCommand(server, "pokespawn sandshrew");
                break;
            case "flying":
                tryCommand(server, "pokespawn spearow");
                tryCommand(server, "pokespawn zubat");
                break;
            case "psychic":
                tryCommand(server, "pokespawn abra");
                tryCommand(server, "pokespawn drowzee");
                break;
            case "bug":
                tryCommand(server, "pokespawn caterpie");
                tryCommand(server, "pokespawn weedle");
                break;
            case "rock":
                tryCommand(server, "pokespawn geodude");
                tryCommand(server, "pokespawn onix");
                break;
            case "ghost":
                tryCommand(server, "pokespawn gastly");
                tryCommand(server, "pokespawn misdreavus");
                break;
            case "dragon":
                tryCommand(server, "pokespawn dratini");
                tryCommand(server, "pokespawn bagon");
                break;
            case "dark":
                tryCommand(server, "pokespawn umbreon");
                tryCommand(server, "pokespawn houndour");
                break;
            case "steel":
                tryCommand(server, "pokespawn magnemite");
                tryCommand(server, "pokespawn skarmory");
                break;
            case "fairy":
                tryCommand(server, "pokespawn clefairy");
                tryCommand(server, "pokespawn togepi");
                break;
        }
    }

    /**
     * Try to force spawn a Pokémon of the specified type
     */
    private static void forceSpawnTypePokemon(ServerPlayerEntity player, String type) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        try {
            // Try multiple approaches to force a spawn

            // Approach 1: General type spawn command
            boolean success = tryCommand(server, "pokespawn " + type.toLowerCase());

            // Approach 2: If general command fails, try specific Pokémon of that type
            if (!success) {
                trySpawnSpecificTypeExamples(server, type);
            }

            // Show visual confirmation
            ServerWorld world = player.getLevel();

            // Create particle effect to show spawn attempt
            for (int i = 0; i < 3; i++) {
                double radius = 0.5 + (i * 0.5);
                for (int j = 0; j < 8; j++) {
                    double angle = j * Math.PI / 4;
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;

                    world.sendParticles(
                            net.minecraft.particles.ParticleTypes.WITCH,
                            x, player.getY() + 0.2, z,
                            1, 0, 0, 0, 0.01
                    );
                }
            }

            // Log the attempt
            CustomConsumables.getLogger().debug(
                    "Attempted to force spawn {} type Pokémon for player {}",
                    type, player.getName().getString()
            );

        } catch (Exception e) {
            // Just log and continue
            CustomConsumables.getLogger().debug("Error forcing spawn: " + e.getMessage());
        }
    }

    /**
     * Try to execute a command safely
     * @return true if the command executed without errors
     */
    private static boolean tryCommand(MinecraftServer server, String command) {
        try {
            server.getCommands().performCommand(
                    server.createCommandSourceStack().withPermission(4),
                    command
            );
            return true;
        } catch (Exception e) {
            // Silently ignore errors
            return false;
        }
    }
}