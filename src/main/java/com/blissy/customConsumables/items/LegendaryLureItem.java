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

public class LegendaryLureItem extends Item {
    private final float legendaryChance;
    private final int durationTicks;

    public LegendaryLureItem(Item.Properties properties, float legendaryChance, int durationSeconds) {
        super(properties);
        this.legendaryChance = legendaryChance;
        this.durationTicks = durationSeconds * 20; // Convert seconds to ticks
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (entityLiving instanceof PlayerEntity && !worldIn.isClientSide) {
            PlayerEntity playerIn = (PlayerEntity) entityLiving;

            // Apply the legendary lure effect with the configured chance
            PlayerEffectManager.applyLegendaryLureEffect(playerIn, durationTicks, legendaryChance);

            // Notify the player
            playerIn.displayClientMessage(
                    new StringTextComponent(
                            TextFormatting.GOLD + "You consumed a Legendary Lure! " +
                                    TextFormatting.YELLOW + "Legendary spawn chance increased to " +
                                    TextFormatting.GREEN + legendaryChance + "%" +
                                    TextFormatting.YELLOW + " for " + (durationTicks / 20) + " seconds!"
                    ),
                    true
            );

            // Log usage
            CustomConsumables.getLogger().info(
                    "Player {} used Legendary Lure with {}% chance for {} seconds",
                    playerIn.getName().getString(),
                    legendaryChance,
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
        tooltip.add(new StringTextComponent(TextFormatting.GOLD + "Legendary Lure"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases legendary spawn chance to " +
                TextFormatting.GREEN + legendaryChance + "%" +
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