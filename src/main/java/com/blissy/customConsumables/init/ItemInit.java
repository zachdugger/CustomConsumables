package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.LegendaryEggItem;
import com.blissy.customConsumables.items.ShinyEggItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemInit {
    // Create a deferred register for items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CustomConsumables.MOD_ID);

    // Register the Legendary Egg item with a 0.5% chance to spawn a legendary
    public static final RegistryObject<Item> LEGENDARY_LURE = ITEMS.register("legendary_egg",
            () -> new LegendaryEggItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));

    // Register the Shiny Egg item with a 35% chance to spawn a shiny
    public static final RegistryObject<Item> SHINY_CHARM = ITEMS.register("shiny_egg",
            () -> new ShinyEggItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));
}