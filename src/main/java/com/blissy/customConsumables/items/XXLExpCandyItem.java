package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    /**
     * Override the use method to handle right-click behavior
     */
    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (world.isClientSide) {
            return ActionResult.success(itemstack);
        }

        if (!(player instanceof ServerPlayerEntity)) {
            return ActionResult.pass(itemstack);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

        // Check if Pixelmon is loaded
        boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
        if (!pixelmonLoaded) {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED +
                            "Pixelmon mod is not installed! This item requires Pixelmon."),
                    true
            );
            return ActionResult.fail(itemstack);
        }

        // Use direct command execution instead of reflection
        boolean success = applyExpWithCommand(serverPlayer);

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
                itemstack.shrink(1);
            }

            return ActionResult.consume(itemstack);
        } else {
            // Notify player they need to select a Pokémon first
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED + "Make sure you have a Pokémon selected in your party!"),
                    true
            );
            return ActionResult.fail(itemstack);
        }
    }

    /**
     * Apply experience using Pixelmon commands instead of reflection
     * This is more reliable across different Pixelmon versions
     */
    private boolean applyExpWithCommand(ServerPlayerEntity player) {
        try {
            if (player.getServer() != null) {
                // Get the selected pokemon index first
                player.getServer().getCommands().performCommand(
                        player.createCommandSourceStack().withPermission(4),
                        "pokegive " + player.getName().getString() + " exp " + EXP_AMOUNT + " selected"
                );
                return true;
            }
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Failed to apply experience via command: {}", e.getMessage());

            // Try alternative command syntax as fallback
            try {
                if (player.getServer() != null) {
                    player.getServer().getCommands().performCommand(
                            player.createCommandSourceStack().withPermission(4),
                            "exp add " + player.getName().getString() + " " + EXP_AMOUNT
                    );
                    return true;
                }
            } catch (Exception cmdEx) {
                CustomConsumables.getLogger().error("Failed to apply experience via alt command: {}", cmdEx.getMessage());
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Gives " +
                TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.YELLOW +
                " experience points to a selected Pokémon"));
        tooltip.add(new StringTextComponent(TextFormatting.ITALIC + "Select a Pokémon first, then right-click with this item"));
    }
}