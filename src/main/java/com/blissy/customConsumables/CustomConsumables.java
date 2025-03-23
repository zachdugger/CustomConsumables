package com.blissy.customConsumables;

import com.blissy.customConsumables.init.ItemInit;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("customconsumables")
public class CustomConsumables {
    public static final String MOD_ID = "customconsumables";
    private static final Logger LOGGER = LogManager.getLogger();

    public CustomConsumables() {
        LOGGER.info("Initializing CustomConsumables mod");

        // Get the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register setup method for modloading
        modEventBus.addListener(this::setup);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("CustomConsumables mod initialization complete");
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("CustomConsumables setup starting");

        // Check for Pixelmon
        boolean pixelmonLoaded = checkPixelmonAvailability();
        LOGGER.info("Pixelmon detected: {}", pixelmonLoaded ? "Yes" : "No");

        if (pixelmonLoaded) {
            LOGGER.info("CustomConsumables is integrated with Pixelmon");
        } else {
            LOGGER.warn("Pixelmon not detected! CustomConsumables requires Pixelmon to function properly.");
        }

        LOGGER.info("CustomConsumables setup complete");
    }

    /**
     * Check for Pixelmon availability
     * @return true if Pixelmon is loaded
     */
    private boolean checkPixelmonAvailability() {
        // Check if the mod is loaded
        return ModList.get().isLoaded("pixelmon");
    }

    /**
     * Get the mod's logger instance
     * @return The logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Event handler to notify players when they join about the mod
     */
    @Mod.EventBusSubscriber(modid = MOD_ID)
    public static class PlayerEvents {
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // Only notify operators about the mod
            if (event.getPlayer().hasPermissions(2)) {
                event.getPlayer().sendMessage(
                        new StringTextComponent(TextFormatting.GREEN + "CustomConsumables mod is active!"),
                        event.getPlayer().getUUID()
                );

                // Let them know which items we've replaced
                if (ItemInit.LEGENDARY_EGG != null) {
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.GOLD + "Replaced " +
                                    ItemInit.LEGENDARY_EGG.getRegistryName() + " with Legendary Egg (0.5% chance to spawn legendary)"),
                            event.getPlayer().getUUID()
                    );
                }

                if (ItemInit.SHINY_EGG != null) {
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.AQUA + "Replaced " +
                                    ItemInit.SHINY_EGG.getRegistryName() + " with Shiny Egg (35% chance to spawn shiny)"),
                            event.getPlayer().getUUID()
                    );
                }

                if (ItemInit.XXL_EXP_CANDY != null) {
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.LIGHT_PURPLE + "Replaced " +
                                    ItemInit.XXL_EXP_CANDY.getRegistryName() + " with XXL Exp. Candy (100,000 exp points)"),
                            event.getPlayer().getUUID()
                    );
                }

                // Check for active Pixelmon
                if (ModList.get().isLoaded("pixelmon")) {
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.GREEN + "Pixelmon integration is enabled"),
                            event.getPlayer().getUUID()
                    );
                } else {
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.RED + "Warning: Pixelmon mod not detected!"),
                            event.getPlayer().getUUID()
                    );
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.RED + "CustomConsumables requires Pixelmon to function properly."),
                            event.getPlayer().getUUID()
                    );
                }
            }
        }
    }
}