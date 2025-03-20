package com.blissy.customConsumables;

import com.blissy.customConsumables.events.DynamicTypeSpawnHandler;
import com.blissy.customConsumables.events.PokemonTypeDataHandler;
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

@Mod(CustomConsumables.MOD_ID)
public class CustomConsumables {
    public static final String MOD_ID = "customconsumables";

    private static final Logger LOGGER = LogManager.getLogger();

    // Create a custom item group (creative tab) for our items
    public static final ItemGroup CUSTOMCONSUMABLES_GROUP = new ItemGroup("customconsumables") {
        @Override
        public ItemStack makeIcon() {
            // Return the first registered item as the tab icon
            return new ItemStack(ItemInit.LEGENDARY_LURE.get());
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
        boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
        LOGGER.info("Pixelmon detected: {}", pixelmonLoaded ? "Yes" : "No");

        if (pixelmonLoaded) {
            LOGGER.info("CustomConsumables is integrated with Pixelmon");
            LOGGER.info("Type Attractor now boosts spawn rates of specific types by 1000%");

            // Register data functions if server supports them
            event.enqueueWork(() -> {
                try {
                    LOGGER.info("Setting up Pixelmon integration");

                    // Initialize the type data handler
                    PokemonTypeDataHandler typeHandler = PokemonTypeDataHandler.getInstance();
                    typeHandler.initialize();

                    // Initialize the dynamic spawn handler
                    DynamicTypeSpawnHandler.initialize();

                    LOGGER.info("Pixelmon integration setup complete");
                } catch (Exception e) {
                    LOGGER.error("Error during Pixelmon integration: {}", e.getMessage());
                }
            });
        } else {
            LOGGER.info("CustomConsumables will work without Pixelmon features");
        }

        LOGGER.info("CustomConsumables setup complete");
        LOGGER.info("Available items:");
        LOGGER.info(" - Legendary Lure (1% chance to spawn a legendary Pokémon)");
        LOGGER.info(" - Shiny Charm (50% chance to spawn a shiny Pokémon)");
        LOGGER.info(" - Type Attractor (Boosts specified type spawn rates by 1000%)");
        LOGGER.info("Use /customitem debug to verify mod is working in-game");
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

                event.getPlayer().sendMessage(
                        new StringTextComponent(TextFormatting.YELLOW + "Use /customitem debug to verify integration"),
                        event.getPlayer().getUUID()
                );

                // Check for active Pixelmon
                if (ModList.get().isLoaded("pixelmon")) {
                    event.getPlayer().sendMessage(
                            new StringTextComponent(TextFormatting.GREEN + "Pixelmon integration is enabled"),
                            event.getPlayer().getUUID()
                    );
                }
            }
        }
    }
}