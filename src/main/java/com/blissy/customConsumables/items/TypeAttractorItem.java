package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class TypeAttractorItem extends Item {
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_DURATION = 3 * 60 * 20; // 3 minutes in ticks
    private static final float TYPE_MULTIPLIER = 10.0f; // 1000% boost
    private final String defaultType;

    // Valid Pokémon types
    private static final List<String> VALID_TYPES = Arrays.asList(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison",
            "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark",
            "steel", "fairy");

    public TypeAttractorItem(Properties properties, String defaultType) {
        super(properties);
        this.defaultType = validateType(defaultType);
    }

    /**
     * Validates and normalizes a type string
     */
    private String validateType(String type) {
        String normalized = type.toLowerCase();
        return VALID_TYPES.contains(normalized) ? normalized : "fire";
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, LivingEntity entityLiving) {
        if (entityLiving instanceof ServerPlayerEntity && !world.isClientSide) {
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

            // Get the type from the item
            String type = getTypeFromStack(stack);
            String typeName = formatTypeName(type);

            try {
                // Apply type boost effect to player
                PlayerEffectManager.applyTypeAttractorEffect(player, type, DEFAULT_DURATION, TYPE_MULTIPLIER);

                // Register boost using Pixelmon command - most reliable method
                boolean boostRegistered = registerTypeBoostCommand(player, type);

                // Display success message
                player.displayClientMessage(
                        new StringTextComponent(getTypeColor(type) + typeName + " Type Attractor " +
                                TextFormatting.GREEN + "activated!"),
                        true
                );

                // Show detailed status message about the boost
                if (boostRegistered) {
                    player.displayClientMessage(
                            new StringTextComponent(TextFormatting.GREEN + "Successfully boosted " + typeName +
                                    " type spawn rate by 1000% for 3 minutes!"),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            new StringTextComponent(TextFormatting.YELLOW + "Your Type Attractor will work, but with reduced effectiveness."),
                            true
                    );
                }

                // Create a visual effect
                createTypeBoostEffect(player, type);

                // Log the usage
                CustomConsumables.getLogger().info(
                        "Player {} used a {} Type Attractor",
                        player.getName().getString(), typeName
                );

                // Consume the item unless in creative mode
                if (!player.abilities.instabuild) {
                    stack.shrink(1);
                }
            } catch (Exception e) {
                // Detailed error logging
                CustomConsumables.getLogger().error("Error activating Type Attractor: {}", e.getMessage());

                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "Error: " + e.getMessage()),
                        true
                );
            }
        }

        return stack;
    }

    /**
     * Register the type boost using Pixelmon command
     */
    private boolean registerTypeBoostCommand(ServerPlayerEntity player, String type) {
        try {
            player.getServer().getCommands().performCommand(
                    player.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn boosttype " + type.toLowerCase() + " 10"
            );
            return true;
        } catch (Exception e) {
            CustomConsumables.getLogger().warn("Command approach failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Creates a visual particle effect based on the Pokémon type
     */
    /**
     * Creates a visual particle effect based on the Pokémon type
     */
    private void createTypeBoostEffect(ServerPlayerEntity player, String type) {
        if (!(player.level instanceof net.minecraft.world.server.ServerWorld)) return;

        net.minecraft.world.server.ServerWorld world = (net.minecraft.world.server.ServerWorld) player.level;

        // Play sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

        // Create a spiral effect based on type
        switch(type.toLowerCase()) {
            case "fire":
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.FLAME);
                break;
            case "water":
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.DRIPPING_WATER);
                break;
            case "electric":
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.FIREWORK);
                break;
            case "ice":
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.ITEM_SNOWBALL);
                break;
            case "grass":
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.COMPOSTER);
                break;
            case "dragon":
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.DRAGON_BREATH);
                break;
            default:
                createSpiralEffect(world, player, net.minecraft.particles.ParticleTypes.WITCH);
                break;
        }
    }

    /**
     * Helper method to create a spiral particle effect
     */
    private void createSpiralEffect(net.minecraft.world.server.ServerWorld world, ServerPlayerEntity player,
                                    net.minecraft.particles.IParticleData particleType) {
        // Create a spiral effect
        for (int i = 0; i < 80; i++) {
            double angle = i * 0.15;
            double radius = Math.min(5.0, 0.3 * i);
            double height = i * 0.03;

            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            world.sendParticles(
                    particleType,
                    x, player.getY() + height, z,
                    1, 0, 0, 0, 0.01
            );

            // Add some occasional enchantment particles
            if (i % 10 == 0) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.ENCHANT,
                        player.getX() + RANDOM.nextDouble() * 2 - 1,
                        player.getY() + 1 + RANDOM.nextDouble(),
                        player.getZ() + RANDOM.nextDouble() * 2 - 1,
                        1, 0, 0, 0, 0.05
                );
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        String type = getTypeFromStack(stack);
        String typeName = formatTypeName(type);

        TextFormatting typeColor = getTypeColor(type);

        tooltip.add(new StringTextComponent(typeColor + typeName + " Type Attractor"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases " + typeColor + typeName +
                TextFormatting.YELLOW + " type Pokémon spawns by " + TextFormatting.GREEN +
                (int)(TYPE_MULTIPLIER * 100) + "%" + TextFormatting.YELLOW + " for 3 minutes"));
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
        nbt.putString("type", validateType(type));
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
     * Format a type name for display
     */
    public static String formatTypeName(String type) {
        if (type == null || type.isEmpty()) return "";
        return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
    }

    /**
     * Get text formatting color for a type
     */
    private TextFormatting getTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "fire": return TextFormatting.RED;
            case "water": return TextFormatting.BLUE;
            case "grass": return TextFormatting.GREEN;
            case "electric": return TextFormatting.YELLOW;
            case "ice": return TextFormatting.AQUA;
            case "fighting": return TextFormatting.DARK_RED;
            case "poison": return TextFormatting.DARK_PURPLE;
            case "ground": return TextFormatting.GOLD;
            case "flying": return TextFormatting.LIGHT_PURPLE;
            case "psychic": return TextFormatting.LIGHT_PURPLE;
            case "bug": return TextFormatting.DARK_GREEN;
            case "rock": return TextFormatting.GOLD;
            case "ghost": return TextFormatting.DARK_PURPLE;
            case "dragon": return TextFormatting.DARK_BLUE;
            case "dark": return TextFormatting.DARK_GRAY;
            case "steel": return TextFormatting.GRAY;
            case "fairy": return TextFormatting.LIGHT_PURPLE;
            default: return TextFormatting.WHITE;
        }
    }

    /**
     * Checks if the given type is valid
     *
     * @param type The type to check
     * @return true if it's a valid Pokémon type
     */
    public static boolean isValidType(String type) {
        return VALID_TYPES.contains(type.toLowerCase());
    }

    /**
     * Gets the list of valid Pokémon types
     */
    public static List<String> getValidTypes() {
        return VALID_TYPES;
    }
}