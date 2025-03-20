package com.blissy.customConsumables;

import com.blissy.customConsumables.init.ItemInit;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("customconsumables")
public class CustomConsumables {

    public static final String MOD_ID = "customconsumables";

    // Fix: Updated createIcon to makeIcon for Forge 1.16.5
    public static final ItemGroup CUSTOMCONSUMABLES_GROUP = new ItemGroup("customconsumables") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ItemInit.LEGENDARY_LURE.get());
        }
    };

    public CustomConsumables() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register items
        ItemInit.ITEMS.register(modEventBus);

        // Register setup method
        modEventBus.addListener(this::setup);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Register our event handler (we'll fix this too)
        MinecraftForge.EVENT_BUS.register(new com.blissy.customConsumables.events.ConsumableEvents());
    }
}