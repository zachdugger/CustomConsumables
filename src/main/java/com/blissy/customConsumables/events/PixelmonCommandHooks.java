package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * This class integrates with Pixelmon through commands instead of reflection.
 * It intercepts Pixelmon commands for legendary spawns and modifies them.
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PixelmonCommandHooks {
    private static final String PIXELMON_MOD_ID = "pixelmon";
    private static final Random random = new Random();
    private static final float DEFAULT_LEGENDARY_CHANCE = 100.0f;

    // Track test mode for each player
    private static final Map<UUID, Integer> testModeCountdown = new HashMap<>();
    private static int tickCounter = 0;

    // Whether we successfully intercepted a spawn command
    private static boolean lastCommandIntercepted = false;

    /**
     * Check if Pixelmon is loaded
     */
    public static boolean isPixelmonLoaded() {
        return ModList.get().isLoaded(PIXELMON_MOD_ID);
    }

    /**
     * Track commands to intercept Pixelmon legendary spawn attempts
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommand(CommandEvent event) {
        if (!isPixelmonLoaded()) {
            return;
        }

        String command = event.getParseResults().getReader().getString();

        // If this is a Pixelmon legendary spawn command, we can intercept it
        if (command.startsWith("pokespawn legendary") ||
                command.equals("pokespawn legend") ||
                command.startsWith("pokespawn legends") ||
                command.startsWith("/pokespawn legendary") ||
                command.equals("/pokespawn legend") ||
                command.startsWith("/pokespawn legends")) {

            try {
                // Get the command source
                CommandSource source = event.getParseResults().getContext().getSource();
                if (source.getEntity() instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) source.getEntity();

                    // Check if the player has an active legendary lure
                    if (PlayerEffectManager.hasLegendaryLureEffect(player)) {
                        float chance = PlayerEffectManager.getLegendaryLureChance(player, DEFAULT_LEGENDARY_CHANCE);

                        // Roll for success based on the lure chance
                        float roll = random.nextFloat() * 100.0f;
                        boolean success = roll <= chance;

                        CustomConsumables.getLogger().info("Legendary spawn command intercepted from player {} with {}% chance. Roll: {}. Success: {}",
                                player.getName().getString(), chance, roll, success);

                        // Show visual feedback
                        VisualFeedbackSystem.registerLegendarySpawnCheck(player, success);

                        lastCommandIntercepted = true;

                        // If we failed the roll, cancel the command
                        if (!success) {
                            event.setCanceled(true);
                            player.sendMessage(
                                    new StringTextComponent(TextFormatting.RED + "Legendary spawn attempt failed (roll: " +
                                            String.format("%.1f", roll) + "% vs chance: " + String.format("%.1f", chance) + "%)"),
                                    player.getUUID());
                        } else {
                            player.sendMessage(
                                    new StringTextComponent(TextFormatting.GREEN + "Legendary lure is helping your spawn attempt! (roll: " +
                                            String.format("%.1f", roll) + "% vs chance: " + String.format("%.1f", chance) + "%)"),
                                    player.getUUID());
                        }
                    }
                }
            } catch (Exception e) {
                CustomConsumables.getLogger().error("Error processing Pixelmon command", e);
            }
        }

        // Auto-confirm successful spawns
        if (lastCommandIntercepted &&
                (command.startsWith("yes") || command.startsWith("/yes") ||
                        command.equals("y") || command.equals("/y"))) {

            try {
                // Get the command source
                CommandSource source = event.getParseResults().getContext().getSource();
                if (source.getEntity() instanceof PlayerEntity) {
                    PlayerEntity player = (PlayerEntity) source.getEntity();

                    // Show special effect for successful spawn
                    if (PlayerEffectManager.hasLegendaryLureEffect(player)) {
                        VisualFeedbackSystem.registerLegendarySpawnSuccess(player);
                        player.sendMessage(
                                new StringTextComponent(TextFormatting.GOLD + "** Your lure attracted a LEGENDARY Pokémon! **"),
                                player.getUUID());
                    }
                }
            } catch (Exception e) {
                CustomConsumables.getLogger().error("Error processing confirmation command", e);
            }

            lastCommandIntercepted = false;
        }
    }

    /**
     * Server tick handler for the test mode
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        tickCounter++;

        // Process test mode - trigger legendary spawn checks at accelerated rate
        if (!testModeCountdown.isEmpty() && tickCounter % 20 == 0) { // Check every second
            testModeCountdown.entrySet().removeIf(entry -> {
                UUID playerId = entry.getKey();
                int remainingTicks = entry.getValue();

                // Get the server and player
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null) return true;

                ServerPlayerEntity player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    return true; // Remove if player not found
                }

                if (remainingTicks <= 0) {
                    player.sendMessage(
                            new StringTextComponent(TextFormatting.YELLOW + "Legendary test mode has expired."),
                            player.getUUID());
                    return true; // Remove from map
                }

                // Every 5 seconds (roughly), do a legendary check
                if (remainingTicks % 100 == 0) {
                    forceSpawnCommand(player);
                }

                // Update countdown
                entry.setValue(remainingTicks - 20);
                return false;
            });
        }
    }

    /**
     * Force a legendary spawn command for a player
     */
    private static void forceSpawnCommand(ServerPlayerEntity player) {
        if (!isPixelmonLoaded()) {
            return;
        }

        try {
            // Execute the pokespawn command as the player
            String command = "pokespawn legendary";
            MinecraftServer server = player.getServer();

            if (server != null) {
                // Show the attempt message
                player.sendMessage(
                        new StringTextComponent(TextFormatting.YELLOW + "Attempting legendary spawn..."),
                        player.getUUID());

                // Tell the player to check if they get a legendary
                player.sendMessage(
                        new StringTextComponent(TextFormatting.YELLOW + "Watch for special particles and success messages!"),
                        player.getUUID());

                // Run the command
                server.getCommands().performCommand(player.createCommandSourceStack(), command);
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error forcing legendary spawn", e);
        }
    }

    /**
     * Starts test mode for a player, which attempts legendary spawns every few seconds
     */
    public static void startTestMode(PlayerEntity player, int durationSeconds) {
        if (!isPixelmonLoaded()) {
            player.sendMessage(
                    new StringTextComponent(TextFormatting.RED + "Pixelmon not detected!"),
                    player.getUUID());
            return;
        }

        // Make sure they have a lure effect
        if (!PlayerEffectManager.hasLegendaryLureEffect(player)) {
            player.sendMessage(
                    new StringTextComponent(TextFormatting.RED + "You need an active legendary lure effect first!"),
                    player.getUUID());
            player.sendMessage(
                    new StringTextComponent(TextFormatting.YELLOW + "Use /applylegendary or /customitem legendary to get one."),
                    player.getUUID());
            return;
        }

        testModeCountdown.put(player.getUUID(), durationSeconds * 20); // Convert to ticks

        player.sendMessage(
                new StringTextComponent(TextFormatting.GREEN + "Legendary test mode activated for " +
                        durationSeconds + " seconds."),
                player.getUUID());
        player.sendMessage(
                new StringTextComponent(TextFormatting.YELLOW +
                        "The system will attempt legendary spawns every few seconds."),
                player.getUUID());
    }

    /**
     * Force a legendary spawn check for a player.
     * This is used for testing purposes.
     */
    public static boolean forceTestLegendaryCheck(PlayerEntity player) {
        if (!isPixelmonLoaded()) {
            player.sendMessage(
                    new StringTextComponent(TextFormatting.RED + "Pixelmon not detected!"),
                    player.getUUID());
            return false;
        }

        // Make sure they have a lure effect
        if (!PlayerEffectManager.hasLegendaryLureEffect(player)) {
            player.sendMessage(
                    new StringTextComponent(TextFormatting.RED + "You need an active legendary lure effect first!"),
                    player.getUUID());
            player.sendMessage(
                    new StringTextComponent(TextFormatting.YELLOW + "Use /applylegendary or /customitem legendary to get one."),
                    player.getUUID());
            return false;
        }

        try {
            if (player instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                forceSpawnCommand(serverPlayer);
                return true;
            } else {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.RED + "You must be on a server to test this!"),
                        player.getUUID());
                return false;
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error forcing legendary spawn check", e);
            player.sendMessage(
                    new StringTextComponent(TextFormatting.RED + "Error: " + e.getMessage()),
                    player.getUUID());
            return false;
        }
    }
}