package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.LegendaryLureItem;
import com.blissy.customConsumables.items.ShinyCharmItem;
import com.blissy.customConsumables.items.TypeAttractorItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemInit {
    // Create a deferred register for items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CustomConsumables.MOD_ID);

    // Register the Legendary Lure item with a 1% chance to spawn a legendary
    public static final RegistryObject<Item> LEGENDARY_LURE = ITEMS.register("legendary_lure",
            () -> new LegendaryLureItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));

    // Register the Shiny Charm item with a 50% chance to spawn a shiny
    public static final RegistryObject<Item> SHINY_CHARM = ITEMS.register("shiny_charm",
            () -> new ShinyCharmItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));

    // Register the Type Attractor (default: fire type)
    public static final RegistryObject<Item> TYPE_ATTRACTOR = ITEMS.register("type_attractor",
            () -> new TypeAttractorItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16), "fire"));
}