package com.blissy.customConsumables.items;

import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.init.ItemInit;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    public TypeAttractorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (!worldIn.isClientSide) {
            // Get type from NBT or default to "NORMAL"
            CompoundNBT nbt = itemstack.getOrCreateTag();
            String type = nbt.contains("attractorType") ? nbt.getString("attractorType") : "NORMAL";

            // Apply the type boost effect to the player
            PlayerEffectManager.applyTypeBoost(playerIn, type, 2400); // 2400 ticks = 2 minutes

            // Show message to player
            playerIn.sendMessage(new StringTextComponent(TextFormatting.GREEN + "You feel an aura that might attract " + type + " type Pokémon..."), playerIn.getUUID());

            // Consume one item
            if (!playerIn.abilities.instabuild) {
                itemstack.shrink(1);
            }
        }

        return ActionResult.sidedSuccess(itemstack, worldIn.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        CompoundNBT nbt = stack.getOrCreateTag();
        String type = nbt.contains("attractorType") ? nbt.getString("attractorType") : "NORMAL";

        tooltip.add(new StringTextComponent(TextFormatting.GREEN + "Increases " + type + " type Pokémon spawn chance for 2 minutes"));
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    // Helper method to create a type-specific attractor
    public static ItemStack createForType(String type) {
        ItemStack stack = new ItemStack(ItemInit.TYPE_ATTRACTOR.get());
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString("attractorType", type);
        stack.setTag(nbt);
        return stack;
    }
}