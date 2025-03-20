package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.*;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemInit {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CustomConsumables.MOD_ID);

    // Fix: use tab() instead of group() for Forge 1.16.5
    public static final RegistryObject<Item> LEGENDARY_LURE = ITEMS.register("legendary_lure",
            () -> new LegendaryLureItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));

    public static final RegistryObject<Item> SHINY_CHARM = ITEMS.register("shiny_charm",
            () -> new ShinyCharmItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(1)));

    public static final RegistryObject<Item> HIDDEN_ABILITY_CAPSULE = ITEMS.register("hidden_ability_capsule",
            () -> new HiddenAbilityCapsuleItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));

    public static final RegistryObject<Item> TYPE_ATTRACTOR = ITEMS.register("type_attractor",
            () -> new TypeAttractorItem(new Item.Properties()
                    .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                    .stacksTo(16)));
}