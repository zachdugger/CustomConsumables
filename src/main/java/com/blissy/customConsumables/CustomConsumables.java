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

    // Create a custom item group (creative tab) for our items
    public static final ItemGroup CUSTOMCONSUMABLES_GROUP = new ItemGroup("customconsumables") {
        @Override
        public ItemStack makeIcon() {
            // Return the legendary egg as the tab icon
            return new ItemStack(ItemInit.LEGENDARY_EGG.get());
        }
    };

    public CustomConsumables() {
        LOGGER.info("Initializing CustomConsumables mod");

        // Get the mod event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register our items with the game
        ItemInit.ITEMS.register(modEventBus);

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
    @Mod.EventBusSubscriber(modid = "customconsumables")
    public static class PlayerEvents {
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            // Only notify operators about the mod
            if (event.getPlayer().hasPermissions(2)) {
                event.getPlayer().sendMessage(
                        new StringTextComponent(TextFormatting.GREEN + "CustomConsumables mod is active!"),
                        event.getPlayer().getUUID()
                );

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