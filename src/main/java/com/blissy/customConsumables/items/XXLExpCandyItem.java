package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.List;

public class XXLExpCandyItem extends Item {
    private static final int EXP_AMOUNT = 100000; // 100,000 experience points

    public XXLExpCandyItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (entityLiving instanceof ServerPlayerEntity && !worldIn.isClientSide) {
            ServerPlayerEntity player = (ServerPlayerEntity) entityLiving;

            // Check if Pixelmon is loaded
            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
            if (!pixelmonLoaded) {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "Pixelmon mod is not installed! This item requires Pixelmon."),
                        true
                );
                return stack;
            }

            // Apply experience to selected Pokémon
            boolean success = applyExperienceToSelectedPokemon(player);

            if (success) {
                // Notify player of success
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.GREEN + "Your Pokémon gained " +
                                TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.GREEN + " experience points!"),
                        true
                );

                // Log the usage
                CustomConsumables.getLogger().info(
                        "Player {} used XXL Exp. Candy for {} experience points",
                        player.getName().getString(), EXP_AMOUNT
                );

                // Consume the item unless in creative mode
                if (!player.abilities.instabuild) {
                    stack.shrink(1);
                }
            } else {
                // Notify player they need to select a Pokémon first
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "You need to select a Pokémon first to use this item!"),
                        true
                );
            }
        }

        return stack;
    }

    /**
     * Apply experience to the player's selected Pokémon using Pixelmon's API via reflection
     * to avoid direct dependencies
     */
    private boolean applyExperienceToSelectedPokemon(ServerPlayerEntity player) {
        try {
            // Use reflection to access Pixelmon's API
            // Get the PlayerPartyStorage class
            Class<?> storageClass = Class.forName("com.pixelmonmod.pixelmon.api.storage.StorageProxy");

            // Get the method to get player's party
            java.lang.reflect.Method getParty = storageClass.getMethod("getParty", PlayerEntity.class);

            // Get the player's party
            Object playerParty = getParty.invoke(null, player);

            // Check if party exists
            if (playerParty == null) {
                return false;
            }

            // Get the method to get selected Pokémon
            java.lang.reflect.Method getSelected = playerParty.getClass().getMethod("getSelectedPokemon");

            // Get the selected Pokémon
            Object selectedPokemon = getSelected.invoke(playerParty);

            // Check if a Pokémon is selected
            if (selectedPokemon == null) {
                return false;
            }

            // Add experience to the Pokémon
            java.lang.reflect.Method addExperience = selectedPokemon.getClass().getMethod("addExperience", PlayerEntity.class, int.class, boolean.class);
            addExperience.invoke(selectedPokemon, player, EXP_AMOUNT, true);

            return true;
        } catch (Exception e) {
            // Attempt an alternative approach with command
            try {
                // Try using a command as a fallback
                MinecraftServer server = player.getServer();
                if (server != null) {
                    String uuid = player.getStringUUID();
                    // Command format might vary depending on Pixelmon version
                    server.getCommands().performCommand(
                            player.createCommandSourceStack().withPermission(4),
                            "pokegive " + player.getName().getString() + " exp " + EXP_AMOUNT
                    );
                    return true;
                }
            } catch (Exception cmdEx) {
                CustomConsumables.getLogger().error("Failed to apply experience: {}", cmdEx.getMessage());
            }

            CustomConsumables.getLogger().error("Failed to apply experience: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Gives " +
                TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.YELLOW +
                " experience points to a selected Pokémon"));
        tooltip.add(new StringTextComponent(TextFormatting.ITALIC + "Select a Pokémon first, then use this item"));
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        playerIn.startUsingItem(handIn);
        return ActionResult.consume(itemstack);
    }
}