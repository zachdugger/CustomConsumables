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

public class LegendaryLureItem extends Item {

    public LegendaryLureItem(Properties properties) {
        super(properties);
    }

    // Fix: use correct method name for Forge 1.16.5
    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (!worldIn.isClientSide) {
            // Apply the legendary boost effect to the player
            PlayerEffectManager.applyLegendaryBoost(playerIn, 1200); // 1200 ticks = 1 minute

            // Show message to player
            playerIn.sendMessage(new StringTextComponent(TextFormatting.GOLD + "You feel a mysterious energy that might attract legendary Pokémon..."), playerIn.getUUID());

            // Consume one item
            if (!playerIn.abilities.instabuild) {
                itemstack.shrink(1);
            }
        }

        return ActionResult.sidedSuccess(itemstack, worldIn.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.BLUE + "Increases legendary Pokémon spawn rate for 1 minute"));
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }
}