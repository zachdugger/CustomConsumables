package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.UUID;

/**
 * Handler for direct Pixelmon API interactions
 */
public class DirectPixelmonHandler {
    private static final int EXP_AMOUNT = 100000; // Experience points to add

    /**
     * Check if entity is a Pixelmon
     */
    public static boolean isPixelmon(Entity entity) {
        return entity instanceof PixelmonEntity;
    }

    /**
     * Check if Pixelmon belongs to player
     */
    public static boolean belongsToPlayer(Entity entity, ServerPlayerEntity player) {
        if (!(entity instanceof PixelmonEntity)) {
            return false;
        }

        PixelmonEntity pixelmon = (PixelmonEntity) entity;
        UUID pixelmonOwner = pixelmon.getOwner().getUUID();
        return pixelmonOwner != null && pixelmonOwner.equals(player.getUUID());
    }

    /**
     * Apply experience to a Pixelmon
     * This is based on the original XXLExpCandyItem implementation
     */
    public static boolean applyExperience(ServerPlayerEntity player, Entity entity) {
        if (!(entity instanceof PixelmonEntity)) {
            return false;
        }

        PixelmonEntity pixelmon = (PixelmonEntity) entity;

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
                        new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy: " +
                                TextFormatting.YELLOW + pokemon.getDisplayName() + " is already at maximum level!"),
                        true
                );
                return false;
            }

            // Log detailed Pokémon info for debugging
            CustomConsumables.getLogger().info(
                    "Pokemon details before XP candy - Species: {}, Level: {}, Exp: {}, ExpToNext: {}, Health: {}/{}, Form: {}",
                    pokemon.getSpecies().getName(),
                    oldLevel,
                    currentExp,
                    pokemon.getExperienceToLevelUp(),
                    pokemon.getHealth(),
                    pokemon.getMaxHealth(),
                    pokemon.getForm().getName()
            );

            // Instead of directly setting experience, we'll use the level-up system
            // This will trigger proper Pixelmon level-up mechanics
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

            // Get the new level and experience
            int newLevel = pokemon.getPokemonLevel();
            int newExp = pokemon.getExperience();

            // Log detailed data about the experience and level change
            CustomConsumables.getLogger().info(
                    "Experience before: {}, after: {}, difference: {}",
                    currentExp, newExp, newExp - currentExp
            );

            CustomConsumables.getLogger().info(
                    "Level after exp change: {} (was: {}), ExpToNext: {}",
                    newLevel, oldLevel, pokemon.getExperienceToLevelUp()
            );

            // Try to trigger evolution check if level changed
            if (newLevel > oldLevel) {
                // Use testLevelEvolution which properly checks evolution requirements
                pixelmon.testLevelEvolution(newLevel);

                // Make sure to call tryEvolution to handle any potential evolutions
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
                    new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy: " +
                            TextFormatting.GREEN + pokemon.getDisplayName() + " gained " +
                            TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.GREEN + " experience points!"),
                    true
            );

            // Add level up message if level changed
            if (newLevel > oldLevel) {
                int levelsGained = newLevel - oldLevel;
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy: " +
                                TextFormatting.AQUA + pokemon.getDisplayName() +
                                TextFormatting.GREEN + " grew " +
                                (levelsGained > 1 ? levelsGained + " levels" : "a level") +
                                " to level " +
                                TextFormatting.YELLOW + newLevel + "!"),
                        true
                );
            } else {
                // Let player know this may need more exp for level up
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy: " +
                                TextFormatting.GRAY + "Experience stored! " +
                                pokemon.getDisplayName() + " needs more experience to level up."),
                        true
                );
            }

            // Final log with detailed Pokemon info after the change
            CustomConsumables.getLogger().info(
                    "Successfully added experience to {} (Now Level: {}, Exp: {}, ExpToNext: {})",
                    pokemon.getDisplayName(), newLevel, newExp, pokemon.getExperienceToLevelUp()
            );

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

    /**
     * Spawn a legendary Pokemon
     */
    public static boolean spawnLegendaryPokemon(ServerPlayerEntity player) {
        try {
            player.getServer().getCommands().performCommand(
                    player.createCommandSourceStack().withPermission(4),
                    "pokespawn legendary"
            );
            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error spawning legendary Pokemon: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Spawn a shiny Pokemon
     */
    public static boolean spawnShinyPokemon(ServerPlayerEntity player) {
        try {
            player.getServer().getCommands().performCommand(
                    player.createCommandSourceStack().withPermission(4),
                    "pokespawn random shiny"
            );
            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error spawning shiny Pokemon: {}", e.getMessage());
            return false;
        }
    }
}