package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.init.ItemInit;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class TypeAttractorItem extends Item {
    private final float typeChance;
    private final int durationTicks;
    private final String defaultType;

    public TypeAttractorItem(Item.Properties properties, float typeChance, int durationSeconds, String defaultType) {
        super(properties);
        this.typeChance = typeChance;
        this.durationTicks = durationSeconds * 20; // Convert seconds to ticks
        this.defaultType = defaultType;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (entityLiving instanceof PlayerEntity && !worldIn.isClientSide) {
            PlayerEntity playerIn = (PlayerEntity) entityLiving;

            // Get type from NBT or use default
            String type = getTypeFromStack(stack);

            // Apply the type attractor effect with the configured chance
            PlayerEffectManager.applyTypeAttractorEffect(playerIn, type, durationTicks, typeChance);

            // Format type name nicely
            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

            // Notify the player
            playerIn.displayClientMessage(
                    new StringTextComponent(
                            TextFormatting.RED + "You consumed a " + typeName + " Type Attractor! " +
                                    TextFormatting.YELLOW + "Encounter chance for " + typeName + " types increased to " +
                                    TextFormatting.GREEN + typeChance + "%" +
                                    TextFormatting.YELLOW + " for " + (durationTicks / 20) + " seconds!"
                    ),
                    true
            );

            // Log usage
            CustomConsumables.getLogger().info(
                    "Player {} used Type Attractor for {} type with {}% chance for {} seconds",
                    playerIn.getName().getString(),
                    typeName,
                    typeChance,
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
        String type = getTypeFromStack(stack);
        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

        tooltip.add(new StringTextComponent(TextFormatting.RED + typeName + " Type Attractor"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases " + typeName + " type encounter chance to " +
                TextFormatting.GREEN + typeChance + "%" +
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

    /**
     * Sets the Pokémon type for this attractor
     * @param stack The item stack
     * @param type The Pokémon type (e.g., "fire", "water", etc.)
     * @return The modified item stack
     */
    public ItemStack setType(ItemStack stack, String type) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString("type", type.toLowerCase());
        return stack;
    }

    /**
     * Gets the Pokémon type from this attractor
     * @param stack The item stack
     * @return The Pokémon type
     */
    public String getTypeFromStack(ItemStack stack) {
        CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains("type")) {
            return nbt.getString("type");
        }
        return defaultType;
    }

    /**
     * Static factory method to create a type attractor for a specific type
     * Used for commands and other programmatic creation
     *
     * @param type The Pokémon type
     * @param count The stack size
     * @return An ItemStack of the appropriate type attractor
     */
    public static ItemStack createForType(String type, int count) {
        ItemStack stack = new ItemStack(ItemInit.TYPE_ATTRACTOR.get(), count);
        TypeAttractorItem item = (TypeAttractorItem) stack.getItem();
        return item.setType(stack, type);
    }
}