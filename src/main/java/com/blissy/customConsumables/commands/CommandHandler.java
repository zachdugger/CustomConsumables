package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.init.ItemInit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

/**
 * Handles custom mod commands
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class CommandHandler {

    private static final SuggestionProvider<CommandSource> ITEM_SUGGESTIONS = (context, builder) -> {
        return ISuggestionProvider.suggest(
                Arrays.asList("legendary_potion", "shiny_potion", "xxl_exp_candy"),
                builder);
    };

    /**
     * Register commands event handler
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // Register main command
        dispatcher.register(
                Commands.literal("customitem")
                        .requires(source -> source.hasPermission(2)) // Only ops can use
                        .then(Commands.literal("give")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .suggests(ITEM_SUGGESTIONS)
                                        .executes(context -> giveItem(context, StringArgumentType.getString(context, "item")))
                                )
                        )
                        .then(Commands.literal("debug")
                                .executes(context -> debugInfo(context))
                        )
        );
    }

    /**
     * Gives an item to the player
     */
    private static int giveItem(CommandContext<CommandSource> context, String itemName) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrException();
        ItemStack stack;

        switch (itemName.toLowerCase()) {
            case "legendary_potion":
                stack = new ItemStack(ItemInit.LEGENDARY_POTION);
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave Legendary Potion to " + player.getName().getString()), true);
                break;
            case "shiny_potion":
                stack = new ItemStack(ItemInit.SHINY_POTION);
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave Shiny Potion to " + player.getName().getString()), true);
                break;
            case "xxl_exp_candy":
                stack = new ItemStack(ItemInit.XXL_EXP_CANDY);
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave XXL Exp. Candy to " + player.getName().getString()), true);
                break;
            default:
                source.sendFailure(new StringTextComponent(TextFormatting.RED + "Unknown item: " + itemName));
                return 0;
        }

        // Give the item to the player
        boolean added = player.inventory.add(stack);
        if (!added) {
            player.drop(stack, false);
        }

        return 1;
    }

    /**
     * Prints debug information
     */
    private static int debugInfo(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        // Print mod info
        source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "====== CustomConsumables Debug Info ======"), false);
        source.sendSuccess(new StringTextComponent(TextFormatting.YELLOW + "Mod Version: " + "1.0"), false);

        // Check Pixelmon integration
        boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
        source.sendSuccess(new StringTextComponent(TextFormatting.YELLOW + "Pixelmon Integration: " +
                (pixelmonLoaded ? TextFormatting.GREEN + "Enabled" : TextFormatting.RED + "Disabled")), false);

        // Print which Minecraft items we're using for textures
        source.sendSuccess(new StringTextComponent(TextFormatting.AQUA + "Item Appearances:"), false);
        source.sendSuccess(new StringTextComponent(" - " + TextFormatting.GOLD + "Legendary Potion" + TextFormatting.YELLOW + " (appears as Minecraft Potion)"), false);
        source.sendSuccess(new StringTextComponent(" - " + TextFormatting.AQUA + "Shiny Potion" + TextFormatting.YELLOW + " (appears as Minecraft Strength Potion)"), false);
        source.sendSuccess(new StringTextComponent(" - " + TextFormatting.LIGHT_PURPLE + "XXL Exp. Candy" + TextFormatting.YELLOW + " (appears as Minecraft Diamond Horse Armor)"), false);

        return 1;
    }
}