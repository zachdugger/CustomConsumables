package com.blissy.customConsumables.items;

import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ShinyCharmItem extends Item {

    public ShinyCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (!worldIn.isClientSide) {
            // Apply the shiny boost effect to the player
            PlayerEffectManager.applyShinyBoost(playerIn, 3600); // 3600 ticks = 3 minutes

            // Show message to player
            playerIn.sendMessage(new StringTextComponent(TextFormatting.AQUA + "You feel a sparkling aura that might attract shiny Pokémon..."), playerIn.getUUID());

            // Consume one item
            if (!playerIn.abilities.instabuild) {
                itemstack.shrink(1);
            }
        }

        return ActionResult.sidedSuccess(itemstack, worldIn.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases shiny Pokémon spawn chance for 3 minutes"));
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }
}