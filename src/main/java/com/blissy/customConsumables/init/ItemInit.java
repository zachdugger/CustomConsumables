package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.LegendaryEggItem;
import com.blissy.customConsumables.items.ShinyEggItem;
import com.blissy.customConsumables.items.XXLExpCandyItem;
import com.pixelmonmod.pixelmon.items.CurryDishItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemInit {
    // Store references to our custom items
    public static LegendaryEggItem LEGENDARY_EGG;
    public static ShinyEggItem SHINY_EGG;
    public static XXLExpCandyItem XXL_EXP_CANDY;

    // Create the items but don't register them yet
    static {
        LEGENDARY_EGG = new LegendaryEggItem(new Item.Properties()
                .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                .stacksTo(16));

        SHINY_EGG = new ShinyEggItem(new Item.Properties()
                .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                .stacksTo(16));

        XXL_EXP_CANDY = new XXLExpCandyItem(new Item.Properties()
                .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                .stacksTo(64));
    }

    @SubscribeEvent
    public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
        CustomConsumables.getLogger().info("Replacing Pixelmon curry items with custom consumables");

        // Find curry items by their registry names
        ResourceLocation sweetCurryRL = new ResourceLocation("pixelmon", "sweet_curry");
        ResourceLocation spicyCurryRL = new ResourceLocation("pixelmon", "spicy_curry");
        ResourceLocation sourCurryRL = new ResourceLocation("pixelmon", "sour_curry");

        // Get existing curry items from registry if they exist
        Item sweetCurry = ForgeRegistries.ITEMS.getValue(sweetCurryRL);
        Item spicyCurry = ForgeRegistries.ITEMS.getValue(spicyCurryRL);
        Item sourCurry = ForgeRegistries.ITEMS.getValue(sourCurryRL);

        // Register our replacements
        if (sweetCurry instanceof CurryDishItem) {
            event.getRegistry().register(LEGENDARY_EGG.setRegistryName(sweetCurryRL));
            CustomConsumables.getLogger().info("Replaced Sweet Curry with Legendary Egg");
        } else {
            CustomConsumables.getLogger().error("Cannot find Sweet Curry item to replace!");
        }

        if (spicyCurry instanceof CurryDishItem) {
            event.getRegistry().register(SHINY_EGG.setRegistryName(spicyCurryRL));
            CustomConsumables.getLogger().info("Replaced Spicy Curry with Shiny Egg");
        } else {
            CustomConsumables.getLogger().error("Cannot find Spicy Curry item to replace!");
        }

        if (sourCurry instanceof CurryDishItem) {
            event.getRegistry().register(XXL_EXP_CANDY.setRegistryName(sourCurryRL));
            CustomConsumables.getLogger().info("Replaced Sour Curry with XXL Exp Candy");
        } else {
            CustomConsumables.getLogger().error("Cannot find Sour Curry item to replace!");
        }
    }
}