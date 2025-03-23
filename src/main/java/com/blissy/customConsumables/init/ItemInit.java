package com.blissy.customConsumables.init;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.LegendaryPotionItem;
import com.blissy.customConsumables.items.ShinyPotionItem;
import com.blissy.customConsumables.items.XXLExpCandyItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemInit {
    // Store references to our custom items
    public static LegendaryPotionItem LEGENDARY_POTION;
    public static ShinyPotionItem SHINY_POTION;
    public static XXLExpCandyItem XXL_EXP_CANDY;

    // Create the items but don't register them yet
    static {
        LEGENDARY_POTION = new LegendaryPotionItem(new Item.Properties()
                .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                .stacksTo(16));

        SHINY_POTION = new ShinyPotionItem(new Item.Properties()
                .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                .stacksTo(16));

        XXL_EXP_CANDY = new XXLExpCandyItem(new Item.Properties()
                .tab(CustomConsumables.CUSTOMCONSUMABLES_GROUP)
                .stacksTo(64));
    }

    @SubscribeEvent
    public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
        CustomConsumables.getLogger().info("Registering custom consumables using Minecraft items");

        // Register our custom items with their own unique registry names
        // (but using Minecraft item textures)
        event.getRegistry().register(
                LEGENDARY_POTION.setRegistryName(new ResourceLocation(CustomConsumables.MOD_ID, "legendary_potion"))
        );
        CustomConsumables.getLogger().info("Registered Legendary Potion (uses Potion texture)");

        event.getRegistry().register(
                SHINY_POTION.setRegistryName(new ResourceLocation(CustomConsumables.MOD_ID, "shiny_potion"))
        );
        CustomConsumables.getLogger().info("Registered Shiny Potion (uses Strength Potion texture)");

        event.getRegistry().register(
                XXL_EXP_CANDY.setRegistryName(new ResourceLocation(CustomConsumables.MOD_ID, "xxl_exp_candy"))
        );
        CustomConsumables.getLogger().info("Registered XXL Exp Candy (uses Diamond Horse Armor texture)");
    }
}