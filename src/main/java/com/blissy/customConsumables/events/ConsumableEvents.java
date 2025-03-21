package com.blissy.customConsumables.events;

import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumableEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG_MODE = true;
    private static final int NOTIFICATION_INTERVAL = 200; // Notify every 10 seconds (200 ticks)
    private int tickCounter = 0;

    // Tick all player effects
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            PlayerEffectManager.tickEffects(event.player);

            // Debug: Show active effects periodically
            if (DEBUG_MODE && event.player instanceof ServerPlayerEntity) {
                tickCounter++;
                if (tickCounter >= NOTIFICATION_INTERVAL) {
                    tickCounter = 0;
                    showActiveEffects((ServerPlayerEntity) event.player);
                }
            }
        }
    }

    // Show debug info when player joins
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (DEBUG_MODE && event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            player.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + "CustomConsumables loaded! Use /customitem to spawn test items."),
                    player.getUUID()
            );

            // Check if Pixelmon is loaded
            if (PixelmonIntegration.isPixelmonLoaded()) {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.GREEN + "Pixelmon detected! Full integration enabled."),
                        player.getUUID()
                );
            } else {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.YELLOW + "Pixelmon not detected. Limited functionality available."),
                        player.getUUID()
                );
            }
        }
    }

    // Helper method to show active effects to a player
    private void showActiveEffects(ServerPlayerEntity player) {
        boolean hasAnyEffect = false;

        // Check for legendary boost
        int legendaryTime = PlayerEffectManager.getRemainingLegendaryBoostTime(player);
        if (legendaryTime > 0) {
            hasAnyEffect = true;
            player.sendMessage(
                    new StringTextComponent(TextFormatting.GOLD + "[Debug] Legendary boost active: " +
                            formatTime(legendaryTime) + " remaining"),
                    player.getUUID()
            );
        }

        // Check for shiny boost
        int shinyTime = PlayerEffectManager.getRemainingShinyBoostTime(player);
        if (shinyTime > 0) {
            hasAnyEffect = true;
            player.sendMessage(
                    new StringTextComponent(TextFormatting.AQUA + "[Debug] Shiny boost active: " +
                            formatTime(shinyTime) + " remaining"),
                    player.getUUID()
            );
        }

        // Check for type boost
        int typeTime = PlayerEffectManager.getRemainingTypeBoostTime(player);
        String boostType = PlayerEffectManager.getTypeBoost(player);
        if (typeTime > 0 && boostType != null) {
            hasAnyEffect = true;
            player.sendMessage(
                    new StringTextComponent(TextFormatting.GREEN + "[Debug] " + boostType + " type boost active: " +
                            formatTime(typeTime) + " remaining"),
                    player.getUUID()
            );
        }

        // If debug mode is enabled but no effects are active, let the player know
        if (DEBUG_MODE && !hasAnyEffect && tickCounter == 0) {
            player.sendMessage(
                    new StringTextComponent(TextFormatting.GRAY + "[Debug] No active effects"),
                    player.getUUID()
            );
        }
    }

    // Format ticks into mm:ss format
    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}