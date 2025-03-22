package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.init.ItemInit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

/**
 * Command handler for Custom Consumables
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class CommandRegistration {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CustomConsumables.getLogger().info("Registering CustomConsumables commands");

        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // Register the main command
        LiteralArgumentBuilder<CommandSource> customitemCommand = Commands.literal("customitem")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                // Legendary Egg command
                .then(Commands.literal("legendary")
                        .executes(context -> {
                            ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG.get());
                            return giveItem(context.getSource(), legendaryEgg);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG.get(), count);
                                    return giveItem(context.getSource(), legendaryEgg);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG.get());
                                    return giveItem(player, legendaryEgg, context.getSource());
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG.get(), count);
                                            return giveItem(player, legendaryEgg, context.getSource());
                                        }))))
                // Shiny Egg command
                .then(Commands.literal("shiny")
                        .executes(context -> {
                            ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG.get());
                            return giveItem(context.getSource(), shinyEgg);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG.get(), count);
                                    return giveItem(context.getSource(), shinyEgg);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG.get());
                                    return giveItem(player, shinyEgg, context.getSource());
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG.get(), count);
                                            return giveItem(player, shinyEgg, context.getSource());
                                        }))))
                // XXL Exp Candy command
                .then(Commands.literal("xxlcandy")
                        .executes(context -> {
                            ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY.get());
                            return giveItem(context.getSource(), expCandy);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY.get(), count);
                                    return giveItem(context.getSource(), expCandy);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY.get());
                                    return giveItem(player, expCandy, context.getSource());
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY.get(), count);
                                            return giveItem(player, expCandy, context.getSource());
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
                                    "Available items:"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.GOLD +
                                    "Legendary Egg" + TextFormatting.GRAY + " (/customitem legendary)"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.AQUA +
                                    "Shiny Egg" + TextFormatting.GRAY + " (/customitem shiny)"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.LIGHT_PURPLE +
                                    "XXL Exp. Candy" + TextFormatting.GRAY + " (/customitem expcandy)"), false);

                            return 1;
                        }));

        // Register the command
        dispatcher.register(customitemCommand);

        CustomConsumables.getLogger().info("Registered CustomConsumables commands successfully");
    }

    private static int giveItem(CommandSource source, ItemStack stack) {
        try {
            ServerPlayerEntity player = source.getPlayerOrException();
            return giveItem(player, stack, source);
        } catch (Exception e) {
            source.sendFailure(new StringTextComponent("Error: Must be run as a player"));
            return 0;
        }
    }

    private static int giveItem(ServerPlayerEntity player, ItemStack stack, CommandSource source) {
        boolean success = player.addItem(stack);

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
            source.sendFailure(new StringTextComponent("Could not give item to player (inventory full?)"));
            return 0;
        }
    }
}