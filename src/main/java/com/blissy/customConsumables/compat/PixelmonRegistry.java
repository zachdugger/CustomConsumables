package com.blissy.customConsumables.compat;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.reflect.Method;

/**
 * This class registers our event handlers with Pixelmon's event system
 * using reflection to avoid direct dependencies.
 */
public class PixelmonRegistry {

    private static boolean registered = false;

    /**
     * Register our listeners with Pixelmon
     */
    public static void register() {
        if (registered) return;

        try {
            // Register our spawn event listener
            registerSpawnEvents();

            // Register our spawn hooks
            registerSpawnHooks();

            registered = true;
            CustomConsumables.getLogger().info("Successfully registered with Pixelmon's systems");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to register with Pixelmon: {}", e.getMessage());
        }
    }

    /**
     * Register for Pixelmon's events
     */
    private static void registerSpawnEvents() throws Exception {
        // Try to access Pixelmon's event bus
        Class<?> pixelmonEventsClass = Class.forName("com.pixelmonmod.pixelmon.api.events.PixelmonEvents");
        Object spawnEvents = pixelmonEventsClass.getField("SPAWN").get(null);

        // Get the register method
        Method registerMethod = spawnEvents.getClass().getMethod("register", Object.class);

        // Create and register our listener
        Object spawnListener = Class.forName("com.blissy.customConsumables.events.PixelmonSpawnListener")
                .getDeclaredConstructor().newInstance();

        registerMethod.invoke(spawnEvents, spawnListener);

        CustomConsumables.getLogger().info("Registered with Pixelmon's spawn events");
    }

    /**
     * Register our spawn hooks with Pixelmon
     */
    private static void registerSpawnHooks() throws Exception {
        // Register our spawn conditions
        Class<?> spawnConditionClass = Class.forName("com.pixelmonmod.pixelmon.api.spawning.SpawnCondition");
        Class<?> typeFilterClass = Class.forName("com.blissy.customConsumables.spawning.TypeFilterCondition");

        // Get the register method
        Method registerMethod = spawnConditionClass.getMethod("register", Class.class);

        // Register our condition
        registerMethod.invoke(null, typeFilterClass);

        CustomConsumables.getLogger().info("Registered spawn hooks with Pixelmon");
    }

    /**
     * Register with Pixelmon's event system directly
     */
    public static void registerWithEventSystem() {
        try {
            // Try to access Pixelmon's ForgeEventFactory
            Class<?> forgeEventFactoryClass = Class.forName("com.pixelmonmod.pixelmon.api.events.ForgeEventFactory");

            // Get and invoke the registerHandlers method
            Method registerMethod = forgeEventFactoryClass.getMethod("registerHandlers");
            registerMethod.invoke(null);

            CustomConsumables.getLogger().info("Registered with Pixelmon's ForgeEventFactory");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to register with ForgeEventFactory: {}", e.getMessage());
        }
    }
}