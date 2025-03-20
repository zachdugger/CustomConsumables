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

public class HiddenAbilityCapsuleItem extends Item {

    public HiddenAbilityCapsuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (!worldIn.isClientSide) {
            // Apply the hidden ability boost effect to the player
            PlayerEffectManager.applyHiddenAbilityBoost(playerIn, 2400); // 2400 ticks = 2 minutes

            // Show message to player
            playerIn.sendMessage(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "You feel an aura that might reveal hidden abilities..."), playerIn.getUUID());

            // Consume one item
            if (!playerIn.abilities.instabuild) {
                itemstack.shrink(1);
            }
        }

        return ActionResult.sidedSuccess(itemstack, worldIn.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "Increases chance of hidden abilities for 2 minutes"));
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }
}