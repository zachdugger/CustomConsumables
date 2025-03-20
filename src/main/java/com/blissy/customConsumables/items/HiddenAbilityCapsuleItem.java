package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class HiddenAbilityCapsuleItem extends Item {
    private final float hiddenAbilityChance;
    private final int durationTicks;

    public HiddenAbilityCapsuleItem(Item.Properties properties, float hiddenAbilityChance, int durationSeconds) {
        super(properties);
        this.hiddenAbilityChance = hiddenAbilityChance;
        this.durationTicks = durationSeconds * 20; // Convert seconds to ticks
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (entityLiving instanceof PlayerEntity && !worldIn.isClientSide) {
            PlayerEntity playerIn = (PlayerEntity) entityLiving;

            // Apply the hidden ability effect with the configured chance
            PlayerEffectManager.applyHiddenAbilityEffect(playerIn, durationTicks, hiddenAbilityChance);

            // Notify the player
            playerIn.displayClientMessage(
                    new StringTextComponent(
                            TextFormatting.LIGHT_PURPLE + "You consumed a Hidden Ability Capsule! " +
                                    TextFormatting.YELLOW + "Hidden Ability chance increased to " +
                                    TextFormatting.GREEN + hiddenAbilityChance + "%" +
                                    TextFormatting.YELLOW + " for " + (durationTicks / 20) + " seconds!"
                    ),
                    true
            );

            // Log usage
            CustomConsumables.getLogger().info(
                    "Player {} used Hidden Ability Capsule with {}% chance for {} seconds",
                    playerIn.getName().getString(),
                    hiddenAbilityChance,
                    durationTicks / 20
            );

            // Consume the item
            if (!playerIn.abilities.instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "Hidden Ability Capsule"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases hidden ability chance to " +
                TextFormatting.GREEN + hiddenAbilityChance + "%" +
                TextFormatting.YELLOW + " for " + (durationTicks / 20) + " seconds"));
        tooltip.add(new StringTextComponent(TextFormatting.ITALIC + "Consume to activate"));
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