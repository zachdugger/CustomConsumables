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
import java.util.Random;

public class ShinyCharmItem extends Item {
    private static final Random RANDOM = new Random();
    private static final float SHINY_SPAWN_CHANCE = 50.0f; // 50% chance

    public ShinyCharmItem(Item.Properties properties) {
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

            // Roll for shiny spawn (50% chance)
            float roll = RANDOM.nextFloat() * 100.0f;
            boolean success = roll <= SHINY_SPAWN_CHANCE;

            if (success) {
                // Attempt to spawn a shiny Pokémon
                MinecraftServer server = player.getServer();
                if (server != null) {
                    // Run the command to spawn a random shiny Pokémon near the player
                    server.getCommands().performCommand(
                            player.createCommandSourceStack().withPermission(4), // Admin level permission
                            "pokespawn random shiny"
                    );

                    // Notify player of success
                    player.displayClientMessage(
                            new StringTextComponent(TextFormatting.AQUA + "The Shiny Charm worked! A shiny Pokémon is spawning!"),
                            true
                    );

                    // Log the successful spawn
                    CustomConsumables.getLogger().info(
                            "Player {} used Shiny Charm successfully! Spawning a shiny Pokémon.",
                            player.getName().getString()
                    );

                    // Play special sound and particles for dramatic effect
                    player.getLevel().playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            net.minecraft.util.SoundEvents.EXPERIENCE_ORB_PICKUP,
                            net.minecraft.util.SoundCategory.PLAYERS,
                            1.0F,
                            1.0F
                    );

                    // Spawn sparkling particles
                    player.getLevel().sendParticles(
                            net.minecraft.particles.ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1, player.getZ(),
                            50, 0.5, 0.5, 0.5, 0.1
                    );
                }
            } else {
                // Notify player of failure
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.YELLOW + "You used a Shiny Charm, but no shiny Pokémon appeared this time..."),
                        true
                );

                // Log the failed attempt
                CustomConsumables.getLogger().info(
                        "Player {} used Shiny Charm but failed the roll ({}% vs {}% chance).",
                        player.getName().getString(),
                        String.format("%.2f", roll),
                        SHINY_SPAWN_CHANCE
                );
            }

            // Consume the item unless in creative mode
            if (!player.abilities.instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new StringTextComponent(TextFormatting.AQUA + "Shiny Charm"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Has a " +
                TextFormatting.GREEN + SHINY_SPAWN_CHANCE + "%" +
                TextFormatting.YELLOW + " chance to instantly spawn a shiny Pokémon"));
        tooltip.add(new StringTextComponent(TextFormatting.ITALIC + "Consume to try your luck!"));
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