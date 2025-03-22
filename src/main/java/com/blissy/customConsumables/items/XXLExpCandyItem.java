package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class XXLExpCandyItem extends Item {
    private static final int EXP_AMOUNT = 100000; // Experience points to add

    public XXLExpCandyItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (world.isClientSide) {
            return ActionResult.success(player.getItemInHand(hand));
        }

        // Give the player a hint when right-clicking in the air
        player.displayClientMessage(
                new StringTextComponent(TextFormatting.YELLOW + "Right-click directly on a Pokémon to use this item!"),
                true
        );

        return ActionResult.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Gives " +
                TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.YELLOW +
                " experience points to a Pokémon"));
        tooltip.add(new StringTextComponent(TextFormatting.ITALIC + "Right-click directly on a sent-out Pokémon"));
    }

    // Event handler for item interactions
    @Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
    public static class ItemInteractionHandler {
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            PlayerEntity player = event.getPlayer();
            ItemStack stack = event.getItemStack();
            Entity target = event.getTarget();

            // Check if the player is using this item
            if (stack.getItem() instanceof XXLExpCandyItem && target instanceof PixelmonEntity) {
                PixelmonEntity pixelmon = (PixelmonEntity) target;

                // Make sure the Pokémon belongs to the player
                UUID pixelmonOwner = pixelmon.getOwnerUUID();
                if (pixelmonOwner != null && pixelmonOwner.equals(player.getUUID())) {
                    // Apply experience to the Pokémon
                    if (applyExperience(player, pixelmon)) {
                        // Consume the item if successful
                        if (!player.abilities.instabuild) {
                            stack.shrink(1);
                        }
                        event.setCanceled(true);
                        event.setCancellationResult(ActionResultType.SUCCESS);
                    }
                } else {
                    // Not the player's Pokémon
                    player.displayClientMessage(
                            new StringTextComponent(TextFormatting.RED + "You can only use this on your own Pokémon!"),
                            true
                    );
                    event.setCanceled(true);
                    event.setCancellationResult(ActionResultType.FAIL);
                }
            }
        }

        private static boolean applyExperience(PlayerEntity player, PixelmonEntity pixelmon) {
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

                // Log detailed Pokémon info for debugging
                CustomConsumables.getLogger().info(
                        "Pokemon details before XP candy - Species: {}, Level: {}, Exp: {}, ExpToNext: {}, Health: {}/{}, Form: {}, Status: {}",
                        pokemon.getSpecies().getName(),
                        oldLevel,
                        currentExp,
                        pokemon.getExperienceToLevelUp(),
                        pokemon.getHealth(),
                        pokemon.getMaxHealth(),
                        pokemon.getForm().getName(),
                        pokemon.getStatus().type.toString()
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
    }
}