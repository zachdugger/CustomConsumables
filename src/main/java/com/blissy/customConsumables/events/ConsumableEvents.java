package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

/**
 * Main event handler for CustomConsumables mod
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class ConsumableEvents {

    /**
     * Process player login to show welcome message
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

            // Check if Pixelmon is loaded and notify player
            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
            if (!pixelmonLoaded) {
                player.sendMessage(
                        new StringTextComponent(TextFormatting.RED +
                                "Warning: Pixelmon mod is not installed! CustomConsumables items require Pixelmon."),
                        player.getUUID()
                );
            } else if (player.hasPermissions(2)) { // Only notify operators
                player.sendMessage(
                        new StringTextComponent(TextFormatting.GREEN +
                                "CustomConsumables mod is ready! Use /customitem to get special items."),
                        player.getUUID()
                );

                player.sendMessage(
                        new StringTextComponent(TextFormatting.YELLOW +
                                "Available items: Legendary Potion, Shiny Potion, XXL Exp. Candy"),
                        player.getUUID()
                );
            }
        }
    }
}