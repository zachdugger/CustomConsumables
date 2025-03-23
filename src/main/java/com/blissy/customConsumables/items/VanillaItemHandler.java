package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Handler for custom vanilla items
 * This approach avoids registry desync by using vanilla items with custom NBT data
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class VanillaItemHandler {
    private static final Random RANDOM = new Random();

    // NBT tag keys
    public static final String CUSTOM_ITEM_TAG = "CustomConsumableType";
    public static final String LEGENDARY_POTION = "legendary_potion";
    public static final String SHINY_POTION = "shiny_potion";
    public static final String XXL_EXP_CANDY = "xxl_exp_candy";

    // Spawn chances
    private static final float LEGENDARY_SPAWN_CHANCE = 0.5f; // 0.5% chance
    private static final float SHINY_SPAWN_CHANCE = 35.0f; // 35% chance
    private static final int EXP_AMOUNT = 100000; // Experience points to add

    /**
     * Create proper lore list for item tooltips
     */
    private static ListNBT createLoreList(List<String> loreLines) {
        ListNBT loreList = new ListNBT();

        for (String line : loreLines) {
            CompoundNBT lineTag = new CompoundNBT();
            lineTag.putString("text", line);
            loreList.add(lineTag);
        }

        return loreList;
    }

    /**
     * Creates a legendary potion item
     */
    public static ItemStack createLegendaryPotion(int count) {
        ItemStack stack = new ItemStack(Items.POTION, count);
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString(CUSTOM_ITEM_TAG, LEGENDARY_POTION);
        stack.setHoverName(new StringTextComponent(TextFormatting.GOLD + "Legendary Potion"));

        // Make it look like a special potion
        CompoundNBT display = stack.getOrCreateTagElement("display");
        // Custom Potion Color (Gold/Yellow)
        display.putInt("CustomPotionColor", 0xFFD700);

        // Add lore as proper list
        List<String> loreLines = new ArrayList<>();
        loreLines.add(TextFormatting.YELLOW + "Has a " + TextFormatting.GREEN + LEGENDARY_SPAWN_CHANCE + "%" + TextFormatting.YELLOW + " chance to spawn a legendary Pokémon");
        loreLines.add(TextFormatting.ITALIC + "" + TextFormatting.GRAY + "Drink the potion to try your luck!");

        // Create a proper lore NBT list
        display.put("Lore", createLoreList(loreLines));

        // Add glint effect
        stack.enchant(null, 0);
        // Hide enchantment tags
        nbt.putInt("HideFlags", 1);

        return stack;
    }

    /**
     * Creates a shiny potion item
     */
    public static ItemStack createShinyPotion(int count) {
        ItemStack stack = new ItemStack(Items.POTION, count);
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString(CUSTOM_ITEM_TAG, SHINY_POTION);
        stack.setHoverName(new StringTextComponent(TextFormatting.AQUA + "Shiny Potion"));

        // Make it look like a special potion
        CompoundNBT display = stack.getOrCreateTagElement("display");
        // Custom Potion Color (Aqua/Cyan)
        display.putInt("CustomPotionColor", 0x55FFFF);

        // Add lore as proper list
        List<String> loreLines = new ArrayList<>();
        loreLines.add(TextFormatting.YELLOW + "Has a " + TextFormatting.GREEN + SHINY_SPAWN_CHANCE + "%" + TextFormatting.YELLOW + " chance to spawn a shiny Pokémon");
        loreLines.add(TextFormatting.ITALIC + "" + TextFormatting.GRAY + "Drink the potion to try your luck!");

        // Create a proper lore NBT list
        display.put("Lore", createLoreList(loreLines));

        // Add glint effect
        stack.enchant(null, 0);
        // Hide enchantment tags
        nbt.putInt("HideFlags", 1);

        return stack;
    }

    /**
     * Creates an XXL exp candy item
     */
    public static ItemStack createXXLExpCandy(int count) {
        // Using diamond horse armor as base, just like the original
        ItemStack stack = new ItemStack(Items.DIAMOND_HORSE_ARMOR, count);
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString(CUSTOM_ITEM_TAG, XXL_EXP_CANDY);
        stack.setHoverName(new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy"));

        // Add lore as proper list
        List<String> loreLines = new ArrayList<>();
        loreLines.add(TextFormatting.YELLOW + "Gives " + TextFormatting.GOLD + EXP_AMOUNT + TextFormatting.YELLOW + " experience points to a Pokémon");
        loreLines.add(TextFormatting.ITALIC + "" + TextFormatting.GRAY + "Right-click directly on a sent-out Pokémon");

        // Create a proper lore NBT list
        CompoundNBT display = stack.getOrCreateTagElement("display");
        display.put("Lore", createLoreList(loreLines));

        // Add glint effect
        stack.enchant(null, 0);
        // Hide enchantment tags
        nbt.putInt("HideFlags", 1);

        return stack;
    }

    /**
     * Check if an item is a custom consumable
     */
    public static boolean isCustomConsumable(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }

        CompoundNBT nbt = stack.getTag();
        return nbt != null && nbt.contains(CUSTOM_ITEM_TAG);
    }

    /**
     * Get the custom consumable type
     */
    public static String getCustomConsumableType(ItemStack stack) {
        if (!isCustomConsumable(stack)) {
            return "";
        }

        return stack.getTag().getString(CUSTOM_ITEM_TAG);
    }

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        // This is called when a player right-clicks with an item
        ItemStack stack = event.getItemStack();

        if (!isCustomConsumable(stack)) {
            return;
        }

        if (event.getWorld().isClientSide()) {
            return; // Only process on server side
        }

        String itemType = getCustomConsumableType(stack);
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

        // Handle based on item type
        switch (itemType) {
            case LEGENDARY_POTION:
                handleLegendaryPotion(player, stack);
                event.setCanceled(true);
                break;
            case SHINY_POTION:
                handleShinyPotion(player, stack);
                event.setCanceled(true);
                break;
            case XXL_EXP_CANDY:
                // Better message for the XXL Exp Candy
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy: " +
                                TextFormatting.YELLOW + "Right-click directly on a sent-out Pokémon"),
                        true
                );
                break;
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        // This is called when a player right-clicks on an entity
        ItemStack stack = event.getItemStack();

        if (!isCustomConsumable(stack)) {
            return;
        }

        if (event.getWorld().isClientSide()) {
            return; // Only process on server side
        }

        String itemType = getCustomConsumableType(stack);

        // Only handle XXL Exp Candy here - it needs to target a Pokémon entity
        if (itemType.equals(XXL_EXP_CANDY)) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
            Entity target = event.getTarget();

            if (handleXXLExpCandy(player, target, stack)) {
                event.setCanceled(true);
            }
        }
    }

    private static void handleLegendaryPotion(ServerPlayerEntity player, ItemStack stack) {
        // Check if Pixelmon is loaded
        boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
        if (!pixelmonLoaded) {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED + "Pixelmon mod is not installed! This item requires Pixelmon."),
                    true
            );
            return;
        }

        // Roll for legendary spawn (0.5% chance)
        float roll = RANDOM.nextFloat() * 100.0f;
        boolean success = roll <= LEGENDARY_SPAWN_CHANCE;

        if (success) {
            // Spawn a legendary using our helper class
            if (DirectPixelmonHandler.spawnLegendaryPokemon(player)) {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.GOLD + "The Legendary Potion worked! A legendary Pokémon is spawning!"),
                        true
                );

                CustomConsumables.getLogger().info(
                        "Player {} used Legendary Potion successfully!",
                        player.getName().getString()
                );
            } else {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "The Legendary Potion failed! (Command error)"),
                        true
                );
            }
        } else {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.YELLOW + "You drank a Legendary Potion, but no legendary appeared this time..."),
                    true
            );

            CustomConsumables.getLogger().info(
                    "Player {} used Legendary Potion but failed the roll ({}% vs {}% chance).",
                    player.getName().getString(),
                    String.format("%.2f", roll),
                    LEGENDARY_SPAWN_CHANCE
            );
        }

        // Play drinking effect
        player.level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.GENERIC_DRINK,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        // Consume the item if not in creative mode
        if (!player.abilities.instabuild) {
            stack.shrink(1);
        }
    }

    private static void handleShinyPotion(ServerPlayerEntity player, ItemStack stack) {
        // Check if Pixelmon is loaded
        boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
        if (!pixelmonLoaded) {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED + "Pixelmon mod is not installed! This item requires Pixelmon."),
                    true
            );
            return;
        }

        // Roll for shiny spawn
        float roll = RANDOM.nextFloat() * 100.0f;
        boolean success = roll <= SHINY_SPAWN_CHANCE;

        if (success) {
            // Spawn a shiny Pokémon using our helper class
            if (DirectPixelmonHandler.spawnShinyPokemon(player)) {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.AQUA + "The Shiny Potion worked! A shiny Pokémon is spawning!"),
                        true
                );

                CustomConsumables.getLogger().info(
                        "Player {} used Shiny Potion successfully!",
                        player.getName().getString()
                );

                // Play special sound and particles for dramatic effect
                player.level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Spawn sparkling particles - using the correct method for 1.16.5
                for (int i = 0; i < 50; i++) {
                    double offsetX = RANDOM.nextDouble() * 0.5 - 0.25;
                    double offsetY = RANDOM.nextDouble() * 0.5 - 0.25;
                    double offsetZ = RANDOM.nextDouble() * 0.5 - 0.25;
                    player.level.addParticle(
                            net.minecraft.particles.ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1, player.getZ(),
                            offsetX, 0.1, offsetZ
                    );
                }
            } else {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "The Shiny Potion failed! (Command error)"),
                        true
                );
            }
        } else {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.YELLOW + "You drank a Shiny Potion, but no shiny Pokémon appeared this time..."),
                    true
            );

            CustomConsumables.getLogger().info(
                    "Player {} used Shiny Potion but failed the roll ({}% vs {}% chance).",
                    player.getName().getString(),
                    String.format("%.2f", roll),
                    SHINY_SPAWN_CHANCE
            );
        }

        // Play drinking effect
        player.level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.GENERIC_DRINK,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        // Consume the item if not in creative mode
        if (!player.abilities.instabuild) {
            stack.shrink(1);
        }
    }

    private static boolean handleXXLExpCandy(ServerPlayerEntity player, Entity target, ItemStack stack) {
        // Check if the entity is a Pixelmon
        if (!DirectPixelmonHandler.isPixelmon(target)) {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED + "You must use this on a Pokémon!"),
                    true
            );
            return false;
        }

        // Check if the Pokémon belongs to the player
        if (!DirectPixelmonHandler.belongsToPlayer(target, player)) {
            player.displayClientMessage(
                    new StringTextComponent(TextFormatting.RED + "You can only use this on your own Pokémon!"),
                    true
            );
            return false;
        }

        // Apply experience to the Pokémon using our helper class
        boolean success = DirectPixelmonHandler.applyExperience(player, target);

        // If successful, consume the item
        if (success && !player.abilities.instabuild) {
            stack.shrink(1);
        }

        return success;
    }}