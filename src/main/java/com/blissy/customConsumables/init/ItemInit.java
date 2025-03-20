package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.HiddenAbilityCapsuleItem;
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

    // Register the Legendary Lure item with a 100% chance for 1 minute
    public static final RegistryObject<Item> LEGENDARY_LURE = ITEMS.register("legendary_lure",
            () -> new LegendaryLureItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16), 100.0f, 60));

    // Register the Shiny Charm item with a 3x multiplier for 3 minutes
    public static final RegistryObject<Item> SHINY_CHARM = ITEMS.register("shiny_charm",
            () -> new ShinyCharmItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16), 3.0f, 180));

    // Register the Hidden Ability Capsule with a 100% chance for 2 minutes
    public static final RegistryObject<Item> HIDDEN_ABILITY_CAPSULE = ITEMS.register("hidden_ability_capsule",
            () -> new HiddenAbilityCapsuleItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16), 100.0f, 120));

    // Register the Type Attractor with a 100% chance for 2 minutes (default: fire type)
    public static final RegistryObject<Item> TYPE_ATTRACTOR = ITEMS.register("type_attractor",
            () -> new TypeAttractorItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16), 100.0f, 120, "fire"));
}