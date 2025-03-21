package com.blissy.customConsumables.compat;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.ModList;

import java.util.Random;

/**
 * Simplified integration with Pixelmon for the Custom Consumables mod.
 */
public class PixelmonIntegration {
    private static final boolean PIXELMON_LOADED = ModList.get().isLoaded("pixelmon");
    private static final Random RANDOM = new Random();

    /**
     * Check if Pixelmon is loaded
     */
    public static boolean isPixelmonLoaded() {
        return PIXELMON_LOADED;
    }

    /**
     * Force spawn a Pokémon of a specific type near a player
     */
    public static boolean forceSpawnType(PlayerEntity player, String type) {
        if (!PIXELMON_LOADED || !(player instanceof ServerPlayerEntity)) return false;

        try {
            // Use Pixelmon's pokespawn command with the proper type parameter format
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Generate a random offset for more natural looking spawns
            int xOffset = RANDOM.nextInt(10) - 5;
            int zOffset = RANDOM.nextInt(10) - 5;

            // The correct format is "pokespawn random <type>"
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn random " + type.toLowerCase() + " ~" + xOffset + " ~ ~" + zOffset
            );

            CustomConsumables.getLogger().debug("Forcing spawn of {} type for player {}",
                    type, player.getName().getString());

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error forcing spawn: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Apply a type boost to the Pixelmon spawn system
     */
    public static boolean applyTypeBoost(PlayerEntity player, String type) {
        if (!PIXELMON_LOADED || !(player instanceof ServerPlayerEntity)) return false;

        try {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Execute the command to boost the type
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn boosttype " + type.toLowerCase() + " 10"
            );

            CustomConsumables.getLogger().info("Successfully applied {} type boost for player {}",
                    type, player.getName().getString());

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().warn("Error applying type boost: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Remove a type boost from the Pixelmon spawn system
     */
    public static boolean removeTypeBoost(PlayerEntity player) {
        if (!PIXELMON_LOADED || !(player instanceof ServerPlayerEntity)) return false;

        try {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

            // Execute the command to clear boost
            serverPlayer.getServer().getCommands().performCommand(
                    serverPlayer.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn clearboost"
            );

            CustomConsumables.getLogger().info("Removed type boost for player {}",
                    player.getName().getString());

            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().warn("Error removing type boost: {}", e.getMessage());
            return false;
        }
    }
}