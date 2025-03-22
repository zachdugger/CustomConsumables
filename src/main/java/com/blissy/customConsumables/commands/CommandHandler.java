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

    private static final SuggestionProvider<CommandSource> TYPE_SUGGESTIONS = (context, builder) -> {
        return ISuggestionProvider.suggest(
                TypeHelper.getValidTypes().stream()
                        .map(String::toLowerCase),
                builder);
    };

    private static final SuggestionProvider<CommandSource> ITEM_SUGGESTIONS = (context, builder) -> {
        return ISuggestionProvider.suggest(
                Arrays.asList("legendary_lure", "shiny_charm"),
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
                                        .executes(context -> giveItem(context, StringArgumentType.getString(context, "item"), null))
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(TYPE_SUGGESTIONS)
                                                .executes(context -> giveItem(context, StringArgumentType.getString(context, "item"),
                                                        StringArgumentType.getString(context, "type")))
                                        )
                                )
                        )
                        .then(Commands.literal("debug")
                                .executes(CommandHandler::debugInfo)
                        )
                        .then(Commands.literal("reload")
                                .executes(CommandHandler::reloadData)
                        )
        );
    }

    /**
     * Gives an item to the player
     */
    private static int giveItem(CommandContext<CommandSource> context, String itemName, String type) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrException();
        ItemStack stack;

        switch (itemName.toLowerCase()) {
            case "legendary_lure":
                stack = new ItemStack(ItemInit.LEGENDARY_LURE.get());
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave Legendary Lure to " + player.getName().getString()), true);
                break;
            case "shiny_charm":
                stack = new ItemStack(ItemInit.SHINY_CHARM.get());
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave Shiny Charm to " + player.getName().getString()), true);
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
    private static void debugInfo(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        // Print mod info
        source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "====== CustomConsumables Debug Info ======"), false);
        source.sendSuccess(new StringTextComponent(TextFormatting.YELLOW + "Mod Version: " + "1.0"), false);

        // Check Pixelmon integration
        boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
        source.sendSuccess(new StringTextComponent(TextFormatting.YELLOW + "Pixelmon Integration: " +
                (pixelmonLoaded ? TextFormatting.GREEN + "Enabled" : TextFormatting.RED + "Disabled")), false);


    };

    /**
    ; * Reloads data files
     */
    private static int reloadData(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();

        try {
            // Reload type data
            TypeHelper.reloadTypeData();
            source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Successfully reloaded CustomConsumables data!"), true);
            return 1;
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error reloading data: {}", e.getMessage(), e);
            source.sendFailure(new StringTextComponent(TextFormatting.RED + "Error reloading data: " + e.getMessage()));
            return 0;
        }
    }
}