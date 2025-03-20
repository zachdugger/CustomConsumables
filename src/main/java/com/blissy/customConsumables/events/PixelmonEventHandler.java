package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Event handler for Pixelmon-specific events.
 * This class will only attempt to handle Pixelmon events if Pixelmon is loaded.
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class PixelmonEventHandler {
    // Constants
    private static final String PIXELMON_MOD_ID = "pixelmon";
    private static final float DEFAULT_LEGENDARY_CHANCE = 100.0f; // 100% chance for testing
    private static final Random random = new Random();

    // Track initialization state
    private static boolean initialized = false;

    // Reflection cache
    private static Class<?> legendarySpawnEventClass;
    private static Class<?> legendaryChooseLocationClass;
    private static Class<?> legendarySpawnClass;
    private static Class<?> spawnEventClass;
    private static Class<?> pixelmonEntityClass;
    private static Class<?> playerTrackingSpawnerClass;

    /**
     * Check if Pixelmon is loaded.
     */
    public static boolean isPixelmonLoaded() {
        return ModList.get().isLoaded(PIXELMON_MOD_ID);
    }

    /**
     * Initialize the Pixelmon integration.
     * Called during mod startup to prepare the event handlers.
     */
    public static void initialize() {
        if (!isPixelmonLoaded()) {
            CustomConsumables.getLogger().info("Pixelmon not detected, skipping Pixelmon event handler initialization");
            return;
        }

        try {
            // Load all necessary Pixelmon classes for reflection
            legendarySpawnEventClass = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent");
            legendaryChooseLocationClass = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent$ChooseLocation");
            legendarySpawnClass = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent$Spawn");
            spawnEventClass = Class.forName("com.pixelmonmod.pixelmon.api.events.spawning.SpawnEvent");
            pixelmonEntityClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");
            playerTrackingSpawnerClass = Class.forName("com.pixelmonmod.pixelmon.spawning.PlayerTrackingSpawner");

            initialized = true;
            CustomConsumables.getLogger().info("Successfully initialized Pixelmon event handler");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to initialize Pixelmon integration", e);
        }
    }

    /**
     * Use a standard Forge event as a "hook" to check for and process Pixelmon events
     * This avoids the need to directly reference Pixelmon classes in event method parameters
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        // Only process on server side and if initialized
        if (event.getEntityLiving().level.isClientSide || !initialized || !isPixelmonLoaded()) {
            return;
        }

        // If the entity is a player, tick their effects
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            PlayerEffectManager.updatePlayerEffects(player);
        }
    }

    // Method to handle legendary spawn events from the Pixelmon mod
    // This will be called by a special event listener that's registered only if Pixelmon is present
    // See PixelmonHooks class for implementation
}