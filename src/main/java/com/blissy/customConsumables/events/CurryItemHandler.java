package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;
import java.util.UUID;

/**
 * Event handler that intercepts Pixelmon curry item usage and applies custom effects
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class CurryItemHandler {
    private static final Random RANDOM = new Random();

    // Chance configurations
    private static final float LEGENDARY_SPAWN_CHANCE = 0.5f; // 0.5% chance for legendary spawn
    private static final float SHINY_SPAWN_CHANCE = 35.0f;    // 35% chance for shiny spawn
    private static final int EXP_AMOUNT = 100000;             // Experience points to add

    // Pixelmon curry item registry names
    private static final ResourceLocation SWEET_CURRY_RL = new ResourceLocation("pixelmon", "sweet_curry");
    private static final ResourceLocation SPICY_CURRY_RL = new ResourceLocation("pixelmon", "spicy_curry");
    private static final ResourceLocation SOUR_CURRY_RL = new ResourceLocation("pixelmon", "sour_curry");

    /**
     * Handle curry item finishing usage (eating)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
        ItemStack itemStack = event.getItem();

        if (itemStack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = itemStack.getItem().getRegistryName();
        if (itemId == null) {
            return;
        }

        // Handle Sweet Curry as Legendary Egg
        if (itemId.equals(SWEET_CURRY_RL)) {
            event.setCanceled(true); // Cancel the original eating effect
            handleLegendaryEgg(player, itemStack);
        }
        // Handle Spicy Curry as Shiny Egg
        else if (itemId.equals(SPICY_CURRY_RL)) {
            event.setCanceled(true); // Cancel the original eating effect
            handleShinyEgg(player, itemStack);
        }
        // Handle Sour Curry differently - should be right-clicked on a Pokémon
        // This is handled by the other event handler
    }

    /**
     * Handle curry right-click on Pokémon entity (for XXL Exp Candy)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        ItemStack itemStack = event.getItemStack();

        if (itemStack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = itemStack.getItem().getRegistryName();
        if (itemId == null) {
            return;
        }

        // Handle Sour Curry as XXL Exp Candy when right-clicked on a Pokémon
        if (itemId.equals(SOUR_CURRY_RL) && event.getTarget() instanceof PixelmonEntity) {
            PixelmonEntity pixelmon = (PixelmonEntity) event.getTarget();

            // Make sure the Pokémon belongs to the player
            UUID pixelmonOwner = pixelmon.getOwnerUUID();
            if (pixelmonOwner != null && pixelmonOwner.equals(player.getUUID())) {
                event.setCanceled(true); // Cancel the original interaction

                // Apply the XXL Exp Candy effect
                if (applyExperience(player, pixelmon)) {
                    // Consume the item if successful
                    if (!player.abilities.instabuild) {
                        itemStack.shrink(1);
                    }
                }
            } else {
                // Not the player's Pokémon
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "You can only use this on your own Pokémon!"),
                        true
                );
            }
        }
    }

    /**
     * Handle Legendary Egg effect (replaces Sweet Curry)
     */
    private static void handleLegendaryEgg(ServerPlayerEntity player, ItemStack stack) {
        // Roll for legendary spawn (0.5% chance)
        float roll = RANDOM.nextFloat() * 100.0f;
        boolean success = roll <= LEGENDARY_SPAWN_CHANCE;

        if (success) {
            // Attempt to spawn a legendary
            MinecraftServer server = player.getServer();
            if (server != null) {
                // Run the command to spawn a legendary
                server.getCommands().performCommand(
                        player.createCommandSourceStack().withPermission(4), // Admin level permission
                        "pokespawn legendary"
                );

                // Notify player of success
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.GOLD + "The Legendary Egg hatched! A legendary Pokémon is spawning!"),
                        true
                );

                // Log the successful spawn
                CustomConsumables.getLogger().info(
                        "Player {} used Legendary Egg (Sweet Curry) successfully! Spawning a legendary.",
                        player.getName().getString()
                );
            }
        } else {
            // Notify player of failure
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.YELLOW + "You used a Legendary Egg, but no legendary appeared this time..."),
                    true
            );

            // Log the failed attempt
            CustomConsumables.getLogger().info(
                    "Player {} used Legendary Egg (Sweet Curry) but failed the roll ({}% vs {}% chance).",
                    player.getName().getString(),
                    String.format("%.2f", roll),
                    LEGENDARY_SPAWN_CHANCE
            );
        }

        // Consume the item unless in creative mode
        if (!player.abilities.instabuild) {
            stack.shrink(1);
        }
    }

    /**
     * Handle Shiny Egg effect (replaces Spicy Curry)
     */
    private static void handleShinyEgg(ServerPlayerEntity player, ItemStack stack) {
        // Roll for shiny spawn
        float roll = RANDOM.nextFloat() * 100.0f;
        boolean success = roll <= SHINY_SPAWN_CHANCE;

        if (success) {
            // Attempt to spawn a shiny Pokémon
            MinecraftServer server = player.getServer();
            if (server != null) {
                // Run the command to spawn a random shiny Pokémon near the player
                server.getCommands().performCommand(
                        player.createCommandSourceStack().withPermission(4), // Admin level permission
                        "pokespawn random shiny"
                );

                // Notify player of success
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.AQUA + "The Shiny Egg hatched! A shiny Pokémon is spawning!"),
                        true
                );

                // Log the successful spawn
                CustomConsumables.getLogger().info(
                        "Player {} used Shiny Egg (Spicy Curry) successfully! Spawning a shiny Pokémon.",
                        player.getName().getString()
                );

                // Play special sound and particles for dramatic effect
                player.getLevel().playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Spawn sparkling particles
                player.getLevel().sendParticles(
                        net.minecraft.particles.ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        50, 0.5, 0.5, 0.5, 0.1
                );
            }
        } else {
            // Notify player of failure
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.YELLOW + "You used a Shiny Egg, but no shiny Pokémon appeared this time..."),
                    true
            );

            // Log the failed attempt
            CustomConsumables.getLogger().info(
                    "Player {} used Shiny Egg (Spicy Curry) but failed the roll ({}% vs {}% chance).",
                    player.getName().getString(),
                    String.format("%.2f", roll),
                    SHINY_SPAWN_CHANCE
            );
        }

        // Consume the item unless in creative mode
        if (!player.abilities.instabuild) {
            stack.shrink(1);
        }
    }

    /**
     * Apply experience to a Pokémon (for XXL Exp Candy / Sour Curry)
     */
    private static boolean applyExperience(ServerPlayerEntity player, PixelmonEntity pixelmon) {
        try {
            // Get the Pokémon data
            Pokemon pokemon = pixelmon.getPokemon();

            if (pokemon == null) {
                CustomConsumables.getLogger().error("Could not get Pokémon data from entity");
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "Error: Could not get Pokémon data!"),
                        true
                );
                return false;
            }

            // Store the original level and experience
            int oldLevel = pokemon.getPokemonLevel();
            int currentExp = pokemon.getExperience();

            // If already at max level, just notify the player
            if (oldLevel >= 100) {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.YELLOW +
                                pokemon.getDisplayName() + " is already at maximum level!"),
                        true
                );
                return false;
            }

            // Apply the experience
            int targetLevel = oldLevel;
            int remainingExp = EXP_AMOUNT;

            // Apply experience levels one at a time until we've used all the experience
            // or reached max level
            while (remainingExp > 0 && targetLevel < 100) {
                // Get current XP needed to level up
                int expToNextLevel = pokemon.getExperienceToLevelUp();

                // If we have enough exp to level up
                if (remainingExp >= expToNextLevel) {
                    // Level up the Pokémon
                    targetLevel++;
                    remainingExp -= expToNextLevel;

                    // Set the new level - this will properly trigger Pixelmon's level system
                    pokemon.setLevel(targetLevel);
                } else {
                    // Not enough for full level, just add the remaining exp
                    int newExp = pokemon.getExperience() + remainingExp;
                    pokemon.setExperience(newExp);
                    remainingExp = 0;
                }
            }

            // Get the new level
            int newLevel = pokemon.getPokemonLevel();

            // Try to trigger evolution check if level changed
            if (newLevel > oldLevel) {
                // Trigger the evolution check with the new level
                pixelmon.testLevelEvolution(newLevel);

                // Make sure to call evolution tryEvolution to handle any potential evolutions
                pokemon.tryEvolution();
            }

            // Play sound
            player.level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.PLAYER_LEVELUP,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );

            // Notify player of success with exp message
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.GREEN + pokemon.getDisplayName() + " gained " +
                            TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.GREEN + " experience points!"),
                    true
            );

            // Add level up message if level changed
            if (newLevel > oldLevel) {
                int levelsGained = newLevel - oldLevel;
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.AQUA + pokemon.getDisplayName() +
                                TextFormatting.GREEN + " grew " +
                                (levelsGained > 1 ? levelsGained + " levels" : "a level") +
                                " to level " +
                                TextFormatting.YELLOW + newLevel + "!"),
                        true
                );
            } else {
                // Let player know this may need more exp for level up
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.GRAY + "Experience stored! " +
                                pokemon.getDisplayName() + " needs more experience to level up."),
                        true
                );
            }

            return true;
        } catch (Exception e) {
            // Detailed error logging
            CustomConsumables.getLogger().error("Error applying experience to Pokémon: {}", e.getMessage());
            CustomConsumables.getLogger().error("Stack trace:", e);
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED + "Error applying experience to Pokémon!"),
                    true
            );
            return false;
        }
    }
}