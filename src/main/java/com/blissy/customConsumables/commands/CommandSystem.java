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
import net.minecraft.item.Item;
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
public class CommandSystem {

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
                            if (ItemInit.LEGENDARY_EGG == null) {
                                context.getSource().sendFailure(new StringTextComponent(
                                        TextFormatting.RED + "Error: Legendary Egg item is not initialized!"));
                                CustomConsumables.getLogger().error("Tried to give Legendary Egg but item is NULL");
                                return 0;
                            }

                            ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG);
                            return giveItem(context.getSource(), legendaryEgg);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    if (ItemInit.LEGENDARY_EGG == null) {
                                        context.getSource().sendFailure(new StringTextComponent(
                                                TextFormatting.RED + "Error: Legendary Egg item is not initialized!"));
                                        CustomConsumables.getLogger().error("Tried to give Legendary Egg but item is NULL");
                                        return 0;
                                    }

                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG, count);
                                    return giveItem(context.getSource(), legendaryEgg);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    if (ItemInit.LEGENDARY_EGG == null) {
                                        context.getSource().sendFailure(new StringTextComponent(
                                                TextFormatting.RED + "Error: Legendary Egg item is not initialized!"));
                                        CustomConsumables.getLogger().error("Tried to give Legendary Egg but item is NULL");
                                        return 0;
                                    }

                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG);
                                    return giveItem(player, legendaryEgg, context.getSource());
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            if (ItemInit.LEGENDARY_EGG == null) {
                                                context.getSource().sendFailure(new StringTextComponent(
                                                        TextFormatting.RED + "Error: Legendary Egg item is not initialized!"));
                                                CustomConsumables.getLogger().error("Tried to give Legendary Egg but item is NULL");
                                                return 0;
                                            }

                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            ItemStack legendaryEgg = new ItemStack(ItemInit.LEGENDARY_EGG, count);
                                            return giveItem(player, legendaryEgg, context.getSource());
                                        }))))
                // Shiny Egg command
                .then(Commands.literal("shiny")
                        .executes(context -> {
                            if (ItemInit.SHINY_EGG == null) {
                                context.getSource().sendFailure(new StringTextComponent(
                                        TextFormatting.RED + "Error: Shiny Egg item is not initialized!"));
                                CustomConsumables.getLogger().error("Tried to give Shiny Egg but item is NULL");
                                return 0;
                            }

                            ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG);
                            return giveItem(context.getSource(), shinyEgg);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    if (ItemInit.SHINY_EGG == null) {
                                        context.getSource().sendFailure(new StringTextComponent(
                                                TextFormatting.RED + "Error: Shiny Egg item is not initialized!"));
                                        CustomConsumables.getLogger().error("Tried to give Shiny Egg but item is NULL");
                                        return 0;
                                    }

                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG, count);
                                    return giveItem(context.getSource(), shinyEgg);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    if (ItemInit.SHINY_EGG == null) {
                                        context.getSource().sendFailure(new StringTextComponent(
                                                TextFormatting.RED + "Error: Shiny Egg item is not initialized!"));
                                        CustomConsumables.getLogger().error("Tried to give Shiny Egg but item is NULL");
                                        return 0;
                                    }

                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG);
                                    return giveItem(player, shinyEgg, context.getSource());
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            if (ItemInit.SHINY_EGG == null) {
                                                context.getSource().sendFailure(new StringTextComponent(
                                                        TextFormatting.RED + "Error: Shiny Egg item is not initialized!"));
                                                CustomConsumables.getLogger().error("Tried to give Shiny Egg but item is NULL");
                                                return 0;
                                            }

                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            ItemStack shinyEgg = new ItemStack(ItemInit.SHINY_EGG, count);
                                            return giveItem(player, shinyEgg, context.getSource());
                                        }))))
                // XXL Exp Candy command
                .then(Commands.literal("xxlcandy")
                        .executes(context -> {
                            if (ItemInit.XXL_EXP_CANDY == null) {
                                context.getSource().sendFailure(new StringTextComponent(
                                        TextFormatting.RED + "Error: XXL Exp. Candy item is not initialized!"));
                                CustomConsumables.getLogger().error("Tried to give XXL Exp. Candy but item is NULL");
                                return 0;
                            }

                            ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY);
                            return giveItem(context.getSource(), expCandy);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(context -> {
                                    if (ItemInit.XXL_EXP_CANDY == null) {
                                        context.getSource().sendFailure(new StringTextComponent(
                                                TextFormatting.RED + "Error: XXL Exp. Candy item is not initialized!"));
                                        CustomConsumables.getLogger().error("Tried to give XXL Exp. Candy but item is NULL");
                                        return 0;
                                    }

                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY, count);
                                    return giveItem(context.getSource(), expCandy);
                                }))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> {
                                    if (ItemInit.XXL_EXP_CANDY == null) {
                                        context.getSource().sendFailure(new StringTextComponent(
                                                TextFormatting.RED + "Error: XXL Exp. Candy item is not initialized!"));
                                        CustomConsumables.getLogger().error("Tried to give XXL Exp. Candy but item is NULL");
                                        return 0;
                                    }

                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                    ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY);
                                    return giveItem(player, expCandy, context.getSource());
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            if (ItemInit.XXL_EXP_CANDY == null) {
                                                context.getSource().sendFailure(new StringTextComponent(
                                                        TextFormatting.RED + "Error: XXL Exp. Candy item is not initialized!"));
                                                CustomConsumables.getLogger().error("Tried to give XXL Exp. Candy but item is NULL");
                                                return 0;
                                            }

                                            int count = IntegerArgumentType.getInteger(context, "count");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
                                            ItemStack expCandy = new ItemStack(ItemInit.XXL_EXP_CANDY, count);
                                            return giveItem(player, expCandy, context.getSource());
                                        }))))
                // Debug command
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GREEN +
                                    "CustomConsumables Registry Info:"), false);

                            // Legendary Egg
                            if (ItemInit.LEGENDARY_EGG != null) {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GOLD +
                                        "Legendary Egg: " + TextFormatting.GREEN + "Registered at " +
                                        ItemInit.LEGENDARY_EGG.getRegistryName()), false);
                            } else {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GOLD +
                                        "Legendary Egg: " + TextFormatting.RED + "Not Registered"), false);
                            }

                            // Shiny Egg
                            if (ItemInit.SHINY_EGG != null) {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.AQUA +
                                        "Shiny Egg: " + TextFormatting.GREEN + "Registered at " +
                                        ItemInit.SHINY_EGG.getRegistryName()), false);
                            } else {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.AQUA +
                                        "Shiny Egg: " + TextFormatting.RED + "Not Registered"), false);
                            }

                            // XXL Exp Candy
                            if (ItemInit.XXL_EXP_CANDY != null) {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.LIGHT_PURPLE +
                                        "XXL Exp Candy: " + TextFormatting.GREEN + "Registered at " +
                                        ItemInit.XXL_EXP_CANDY.getRegistryName()), false);
                            } else {
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.LIGHT_PURPLE +
                                        "XXL Exp Candy: " + TextFormatting.RED + "Not Registered"), false);
                            }

                            // Check Pixelmon
                            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.YELLOW +
                                    "Pixelmon detected: " + (pixelmonLoaded ? "Yes" : "No")), false);

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
        if (stack == null || stack.isEmpty()) {
            source.sendFailure(new StringTextComponent("Error: Invalid item stack"));
            CustomConsumables.getLogger().error("Attempted to give null or empty ItemStack to player");
            return 0;
        }

        try {
            // Try to add the item to the player's inventory
            boolean added = player.inventory.add(stack);

            // If that fails, try dropping it at their feet
            if (!added) {
                player.drop(stack, false);
                source.sendSuccess(new StringTextComponent("Inventory full! Dropped item at your feet."), true);
            } else {
                source.sendSuccess(new StringTextComponent("Gave " +
                        stack.getCount() + " " + stack.getHoverName().getString() + " to " + player.getName().getString()), true);

                // Also notify the player if they're not the source
                if (player != source.getEntity()) {
                    player.sendMessage(new StringTextComponent("You received " +
                            stack.getCount() + " " + stack.getHoverName().getString()), player.getUUID());
                }

                // Log to server console
                CustomConsumables.getLogger().info("Gave {} {} to {}",
                        stack.getCount(), stack.getHoverName().getString(), player.getName().getString());
            }

            return stack.getCount();
        } catch (Exception e) {
            source.sendFailure(new StringTextComponent("Could not give item to player: " + e.getMessage()));
            CustomConsumables.getLogger().error("Error giving item to player: {}", e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}