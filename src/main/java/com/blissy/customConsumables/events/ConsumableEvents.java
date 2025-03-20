package com.blissy.customConsumables.events;

import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.compat.PixelmonCompat;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class ConsumableEvents {

    // Tick all player effects
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            PlayerEffectManager.tickEffects(event.player);
        }
    }

    // In a real Pixelmon environment, this will be replaced by actual event handlers
    // But for development, we just need this basic class to compile
}