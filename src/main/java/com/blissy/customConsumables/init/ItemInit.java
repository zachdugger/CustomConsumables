package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.LegendaryEggItem;
import com.blissy.customConsumables.items.ShinyEggItem;
import com.blissy.customConsumables.items.XXLExpCandyItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemInit {
    // Store references to our custom items
    public static LegendaryEggItem LEGENDARY_EGG;
    public static ShinyEggItem SHINY_EGG;
    public static XXLExpCandyItem XXL_EXP_CANDY;

    // Store the original Pixelmon items for reference
    private static Map<ResourceLocation, Item> originalItems = new HashMap<>();

    // Define possible registry names for curry items based on your debug output
    private static final ResourceLocation[] LEGENDARY_OPTIONS = {
            new ResourceLocation("pixelmon", "dish_curry_seasoned"),
            new ResourceLocation("pixelmon", "curry_seasoned")
    };

    private static final ResourceLocation[] SHINY_OPTIONS = {
            new ResourceLocation("pixelmon", "dish_curry_rich"),
            new ResourceLocation("pixelmon", "curry_rich")
    };

    private static final ResourceLocation[] XP_OPTIONS = {
            new ResourceLocation("pixelmon", "dish_curry_plain"),
            new ResourceLocation("pixelmon", "curry_plain")
    };

    // Initialize at registration time
    @SubscribeEvent(priority = EventPriority.LOWEST) // Use LOWEST to run after Pixelmon has registered its items
    public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
        CustomConsumables.getLogger().info("Starting CustomConsumables item registration process");

        try {
            // Store references to the original items
            storeOriginalItems();

            // Initialize our items with appropriate registry names
            initializeItems();

            if (LEGENDARY_EGG != null && SHINY_EGG != null && XXL_EXP_CANDY != null) {
                IForgeRegistry<Item> registry = event.getRegistry();

                // Register our replacement items
                registry.register(LEGENDARY_EGG);
                registry.register(SHINY_EGG);
                registry.register(XXL_EXP_CANDY);

                CustomConsumables.getLogger().info("Successfully registered replacement items");

                // Verify registration was successful
                verifyRegistration();
            } else {
                CustomConsumables.getLogger().error("Failed to initialize items - could not find suitable curry items to replace");
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error during item registration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize our items with the appropriate registry names
     */
    private static void initializeItems() {
        ResourceLocation legendaryRL = null;
        ResourceLocation shinyRL = null;
        ResourceLocation xpRL = null;

        // Find suitable curry items to replace
        for (ResourceLocation rl : LEGENDARY_OPTIONS) {
            if (ForgeRegistries.ITEMS.getValue(rl) != null) {
                legendaryRL = rl;
                CustomConsumables.getLogger().info("Found Legendary Egg target: {}", rl);
                break;
            }
        }

        for (ResourceLocation rl : SHINY_OPTIONS) {
            if (ForgeRegistries.ITEMS.getValue(rl) != null) {
                shinyRL = rl;
                CustomConsumables.getLogger().info("Found Shiny Egg target: {}", rl);
                break;
            }
        }

        for (ResourceLocation rl : XP_OPTIONS) {
            if (ForgeRegistries.ITEMS.getValue(rl) != null) {
                xpRL = rl;
                CustomConsumables.getLogger().info("Found XXL Exp Candy target: {}", rl);
                break;
            }
        }

        // Initialize our items if we found suitable targets
        if (legendaryRL != null) {
            LEGENDARY_EGG = new LegendaryEggItem(new Item.Properties()
                    .tab(ItemGroup.TAB_FOOD)
                    .stacksTo(16));
            LEGENDARY_EGG.setRegistryName(legendaryRL);
        } else {
            CustomConsumables.getLogger().error("Failed to find suitable item for Legendary Egg");
        }

        if (shinyRL != null) {
            SHINY_EGG = new ShinyEggItem(new Item.Properties()
                    .tab(ItemGroup.TAB_FOOD)
                    .stacksTo(16));
            SHINY_EGG.setRegistryName(shinyRL);
        } else {
            CustomConsumables.getLogger().error("Failed to find suitable item for Shiny Egg");
        }

        if (xpRL != null) {
            XXL_EXP_CANDY = new XXLExpCandyItem(new Item.Properties()
                    .tab(ItemGroup.TAB_FOOD)
                    .stacksTo(64));
            XXL_EXP_CANDY.setRegistryName(xpRL);
        } else {
            CustomConsumables.getLogger().error("Failed to find suitable item for XXL Exp Candy");
        }
    }

    /**
     * Store the original Pixelmon curry items
     */
    private static void storeOriginalItems() {
        // Store all possible curry items
        for (ResourceLocation rl : LEGENDARY_OPTIONS) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) {
                originalItems.put(rl, item);
                CustomConsumables.getLogger().info("Stored original item: {}", rl);
            }
        }

        for (ResourceLocation rl : SHINY_OPTIONS) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) {
                originalItems.put(rl, item);
                CustomConsumables.getLogger().info("Stored original item: {}", rl);
            }
        }

        for (ResourceLocation rl : XP_OPTIONS) {
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) {
                originalItems.put(rl, item);
                CustomConsumables.getLogger().info("Stored original item: {}", rl);
            }
        }
    }

    /**
     * Verify that our items were successfully registered
     */
    private static void verifyRegistration() {
        if (LEGENDARY_EGG != null) {
            ResourceLocation rl = LEGENDARY_EGG.getRegistryName();
            Item registered = ForgeRegistries.ITEMS.getValue(rl);

            if (registered instanceof LegendaryEggItem) {
                CustomConsumables.getLogger().info("Verification success: Legendary Egg properly registered at {}", rl);
            } else {
                CustomConsumables.getLogger().error("Verification failed: Legendary Egg not properly registered. Found {} instead",
                        registered != null ? registered.getClass().getName() : "null");
            }
        }

        if (SHINY_EGG != null) {
            ResourceLocation rl = SHINY_EGG.getRegistryName();
            Item registered = ForgeRegistries.ITEMS.getValue(rl);

            if (registered instanceof ShinyEggItem) {
                CustomConsumables.getLogger().info("Verification success: Shiny Egg properly registered at {}", rl);
            } else {
                CustomConsumables.getLogger().error("Verification failed: Shiny Egg not properly registered. Found {} instead",
                        registered != null ? registered.getClass().getName() : "null");
            }
        }

        if (XXL_EXP_CANDY != null) {
            ResourceLocation rl = XXL_EXP_CANDY.getRegistryName();
            Item registered = ForgeRegistries.ITEMS.getValue(rl);

            if (registered instanceof XXLExpCandyItem) {
                CustomConsumables.getLogger().info("Verification success: XXL Exp Candy properly registered at {}", rl);
            } else {
                CustomConsumables.getLogger().error("Verification failed: XXL Exp Candy not properly registered. Found {} instead",
                        registered != null ? registered.getClass().getName() : "null");
            }
        }
    }

    /**
     * Get the original Pixelmon item for a given registry name
     */
    public static Item getOriginalItem(ResourceLocation registryName) {
        return originalItems.getOrDefault(registryName, null);
    }
}