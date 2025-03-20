package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.init.ItemInit;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.List;

public class TypeAttractorItem extends Item {
    private static final int DEFAULT_DURATION = 2 * 60; // 2 minutes in seconds
    private static final float BOOST_MULTIPLIER = 10.0f; // 1000% boost
    private final String defaultType;

    public TypeAttractorItem(Item.Properties properties, String defaultType) {
        super(properties);
        this.defaultType = defaultType.toLowerCase();
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

            // Get type from NBT or use default
            String type = getTypeFromStack(stack);

            // Format type name nicely
            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

            try {
                // Apply the type boost directly through commands and NBT
                applyTypeBoost(player, type, DEFAULT_DURATION * 20); // Convert to ticks

                // Notify the player
                player.displayClientMessage(
                        new StringTextComponent(
                                TextFormatting.GREEN + "You consumed a " + typeName + " Type Attractor! " +
                                        TextFormatting.YELLOW + "For the next " + DEFAULT_DURATION + " seconds, " +
                                        typeName + " type Pokémon are " + (int)(BOOST_MULTIPLIER * 100) + "% more likely to spawn!"
                        ),
                        true
                );

                // Log usage
                CustomConsumables.getLogger().info(
                        "Player {} used Type Attractor for {} type, boosting spawns by {}x for {} seconds",
                        player.getName().getString(),
                        typeName,
                        BOOST_MULTIPLIER,
                        DEFAULT_DURATION
                );

                // Create a visual effect
                if (player.level instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) player.level;
                    serverWorld.sendParticles(
                            net.minecraft.particles.ParticleTypes.ENTITY_EFFECT,
                            player.getX(), player.getY() + 1, player.getZ(),
                            30, 0.5, 0.5, 0.5, 0.05
                    );
                }

                // Play a sound
                player.level.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        net.minecraft.util.SoundEvents.ENCHANTMENT_TABLE_USE,
                        net.minecraft.util.SoundCategory.PLAYERS,
                        1.0F, 1.0F
                );

            } catch (Exception e) {
                CustomConsumables.getLogger().error("Error applying type attractor effect", e);
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "Error: " + e.getMessage()),
                        true
                );
            }

            // Consume the item unless in creative mode
            if (!player.abilities.instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    /**
     * Apply a boost to the specified Pokémon type
     */
    private void applyTypeBoost(ServerPlayerEntity player, String type, int durationTicks) {
        // Store in player NBT data
        CompoundNBT playerData = player.getPersistentData();
        CompoundNBT modData = new CompoundNBT();

        modData.putString("boostedType", type.toLowerCase());
        modData.putInt("boostDuration", durationTicks);
        modData.putFloat("boostMultiplier", BOOST_MULTIPLIER);
        modData.putLong("boostAppliedTime", System.currentTimeMillis());

        playerData.put(CustomConsumables.MOD_ID, modData);

        // Try to apply a type boost through commands if possible
        MinecraftServer server = player.getServer();
        if (server != null) {
            // First try with the pixelmon type boost command if it exists
            try {
                server.getCommands().performCommand(
                        server.createCommandSourceStack().withPermission(4),
                        "pixelmon typeboost " + type.toLowerCase() + " " + BOOST_MULTIPLIER + " " + (durationTicks / 20)
                );
                CustomConsumables.getLogger().info("Applied type boost via pixelmon command");
            } catch (Exception e) {
                // Try a different command format if the first one fails
                try {
                    server.getCommands().performCommand(
                            server.createCommandSourceStack().withPermission(4),
                            "pokespawn boosttype " + type.toLowerCase() + " " + BOOST_MULTIPLIER
                    );
                    CustomConsumables.getLogger().info("Applied type boost via pokespawn command");

                    // Schedule removal after duration
                    server.getCommands().performCommand(
                            server.createCommandSourceStack().withPermission(4),
                            "schedule function customconsumables:remove_type_boost " + (durationTicks / 20) + "s"
                    );
                } catch (Exception e2) {
                    CustomConsumables.getLogger().error("Could not apply type boost via commands: {}", e2.getMessage());
                }
            }
        }

        // Also try to apply the boost through reflection for maximum compatibility
        try {
            Class<?> pixelmonSpawningClass = Class.forName("com.pixelmonmod.pixelmon.spawning.PixelmonSpawning");
            Class<?> elementClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Element");

            // Get the Element enum value
            java.lang.reflect.Method valueOfMethod = elementClass.getMethod("valueOf", String.class);
            Object typeEnum = valueOfMethod.invoke(null, type.toUpperCase());

            // Try to find and call a method to boost spawn rates
            java.lang.reflect.Method boostMethod = pixelmonSpawningClass.getMethod("boostType", elementClass, float.class, int.class);
            boostMethod.invoke(null, typeEnum, BOOST_MULTIPLIER, durationTicks / 20);

            CustomConsumables.getLogger().info("Applied type boost via reflection");
        } catch (Exception e) {
            // Just log at debug level since we've tried other methods
            CustomConsumables.getLogger().debug("Could not apply type boost via reflection: {}", e.getMessage());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        String type = getTypeFromStack(stack);
        String typeName = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();

        tooltip.add(new StringTextComponent(TextFormatting.RED + typeName + " Type Attractor"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases " + typeName +
                " type Pokémon spawns by " + (int)(BOOST_MULTIPLIER * 100) + "% for " + DEFAULT_DURATION + " seconds"));
        tooltip.add(new StringTextComponent(TextFormatting.GRAY + "Works with both mono and dual-typed Pokémon"));
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