package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.VanillaItemHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.Arrays;

/**
 * Command handler for Custom Consumables
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class CommandRegistration {

    private static final SuggestionProvider<CommandSource> ITEM_SUGGESTIONS = (context, builder) -> {
        return ISuggestionProvider.suggest(
                Arrays.asList("legendary", "shiny", "xxlcandy"),
                builder);
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CustomConsumables.getLogger().info("Registering CustomConsumables commands");

        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // Register the main command
        LiteralArgumentBuilder<CommandSource> customitemCommand = Commands.literal("customitem")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)

                // Give subcommand with item argument
                .then(Commands.literal("give")
                        .then(Commands.argument("item", StringArgumentType.word())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> giveItem(context, StringArgumentType.getString(context, "item"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveItem(context, StringArgumentType.getString(context, "item"),
                                                IntegerArgumentType.getInteger(context, "count"))))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> giveItemToPlayer(context,
                                                StringArgumentType.getString(context, "item"),
                                                EntityArgument.getPlayer(context, "player"), 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> giveItemToPlayer(context,
                                                        StringArgumentType.getString(context, "item"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "count")))))))

                // Legendary Potion command (direct access)
                .then(Commands.literal("legendary")
                        .executes(context -> {
                            return giveItemDirect(context, "legendary", 1);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    return giveItemDirect(context, "legendary", count);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    return giveItemToPlayerDirect(context, "legendary", player, 1);
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            return giveItemToPlayerDirect(context, "legendary", player, count);
                                        }))))
                // Shiny Potion command
                .then(Commands.literal("shiny")
                        .executes(context -> {
                            return giveItemDirect(context, "shiny", 1);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    return giveItemDirect(context, "shiny", count);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    return giveItemToPlayerDirect(context, "shiny", player, 1);
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            return giveItemToPlayerDirect(context, "shiny", player, count);
                                        }))))
                // XXL Exp Candy command
                .then(Commands.literal("xxlcandy")
                        .executes(context -> {
                            return giveItemDirect(context, "xxlcandy", 1);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    return giveItemDirect(context, "xxlcandy", count);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    return giveItemToPlayerDirect(context, "xxlcandy", player, 1);
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            return giveItemToPlayerDirect(context, "xxlcandy", player, count);
                                        }))))
                // Debug command
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GREEN +
                                    "CustomConsumables mod is loaded and working!"), false);

                            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.YELLOW +
                                    "Pixelmon detected: " + (pixelmonLoaded ? "Yes" : "No")), false);

                            if (!pixelmonLoaded) {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.RED +
                                        "Warning: The Custom Consumables mod requires Pixelmon to be installed."), false);
                                return 1;
                            }

                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.AQUA +
                                    "Custom consumable items:"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.GOLD +
                                    "Legendary Potion" + TextFormatting.GRAY + " (/customitem legendary)"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.AQUA +
                                    "Shiny Potion" + TextFormatting.GRAY + " (/customitem shiny)"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.LIGHT_PURPLE +
                                    "XXL Exp. Candy" + TextFormatting.GRAY + " (/customitem xxlcandy)"), false);

                            return 1;
                        }));

        // Register the command
        dispatcher.register(customitemCommand);

        CustomConsumables.getLogger().info("Registered CustomConsumables commands successfully");
    }

    /**
     * Give an item specified by name - wrapper method
     */
    private static int giveItem(CommandContext<CommandSource> context, String itemName, int count) throws CommandSyntaxException {
        return giveItemDirect(context, itemName, count);
    }

    /**
     * Directly give an item to the command source
     */
    private static int giveItemDirect(CommandContext<CommandSource> context, String itemName, int count) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrException();
        ItemStack stack;

        switch (itemName.toLowerCase()) {
            case "legendary":
                stack = VanillaItemHandler.createLegendaryPotion(count);
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave " + count + " Legendary Potion to " + player.getName().getString()), true);
                break;
            case "shiny":
                stack = VanillaItemHandler.createShinyPotion(count);
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave " + count + " Shiny Potion to " + player.getName().getString()), true);
                break;
            case "xxlcandy":
                stack = VanillaItemHandler.createXXLExpCandy(count);
                source.sendSuccess(new StringTextComponent(TextFormatting.GREEN + "Gave " + count + " XXL Exp. Candy to " + player.getName().getString()), true);
                break;
            default:
                source.sendFailure(new StringTextComponent(TextFormatting.RED + "Unknown item: " + itemName));
                return 0;
        }

        // Give the item to the player
        giveItemStack(player, stack, source);
        return count;
    }

    /**
     * Give an item to a specific player - wrapper method
     */
    private static int giveItemToPlayer(CommandContext<CommandSource> context, String itemName, ServerPlayerEntity targetPlayer, int count) {
        return giveItemToPlayerDirect(context, itemName, targetPlayer, count);
    }

    /**
     * Give an item to a specific player directly
     */
    private static int giveItemToPlayerDirect(CommandContext<CommandSource> context, String itemName, ServerPlayerEntity targetPlayer, int count) {
        CommandSource source = context.getSource();
        ItemStack stack;

        switch (itemName.toLowerCase()) {
            case "legendary":
                stack = VanillaItemHandler.createLegendaryPotion(count);
                break;
            case "shiny":
                stack = VanillaItemHandler.createShinyPotion(count);
                break;
            case "xxlcandy":
                stack = VanillaItemHandler.createXXLExpCandy(count);
                break;
            default:
                source.sendFailure(new StringTextComponent(TextFormatting.RED + "Unknown item: " + itemName));
                return 0;
        }

        return giveItemStack(targetPlayer, stack, source);
    }

    /**
     * Helper method to give an ItemStack to a player
     */
    private static int giveItemStack(ServerPlayerEntity player, ItemStack stack, CommandSource source) {
        boolean success = player.inventory.add(stack);

        if (success) {
            String itemName = stack.getHoverName().getString();
            source.sendSuccess(new StringTextComponent("Gave " +
                    stack.getCount() + " " + itemName + " to " + player.getName().getString()), true);

            // Also notify the player if they're not the source
            if (player != source.getEntity()) {
                player.sendMessage(new StringTextComponent("You received " +
                        stack.getCount() + " " + itemName), player.getUUID());
            }

            // Log to server console
            CustomConsumables.getLogger().info("Gave {} {} to {}",
                    stack.getCount(), itemName, player.getName().getString());

            return stack.getCount();
        } else {
            // If inventory is full, try to drop the item
            player.drop(stack, false);
            source.sendSuccess(new StringTextComponent("Inventory full, dropped " +
                    stack.getCount() + " " + stack.getHoverName().getString() + " for " +
                    player.getName().getString()), true);

            return stack.getCount();
        }
    }
}