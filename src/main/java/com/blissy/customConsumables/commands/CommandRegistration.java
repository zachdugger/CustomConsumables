package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.TypeAttractorItem;
import com.blissy.customConsumables.init.ItemInit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class CommandRegistration {

    private static final List<String> POKEMON_TYPES = Arrays.asList(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison",
            "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark",
            "steel", "fairy");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CustomConsumables.getLogger().info("Registering CustomConsumables commands");

        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // Register the main command
        LiteralArgumentBuilder<CommandSource> customitemCommand = Commands.literal("customitem")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .then(Commands.literal("legendary")
                        .executes(ctx -> giveItem(ctx.getSource(), new ItemStack(ItemInit.LEGENDARY_LURE.get())))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveItem(ctx.getSource(), new ItemStack(ItemInit.LEGENDARY_LURE.get(),
                                        IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveItemToTarget(ctx, new ItemStack(ItemInit.LEGENDARY_LURE.get())))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> giveItemToTarget(ctx, new ItemStack(ItemInit.LEGENDARY_LURE.get(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))))
                .then(Commands.literal("shiny")
                        .executes(ctx -> giveItem(ctx.getSource(), new ItemStack(ItemInit.SHINY_CHARM.get())))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> giveItem(ctx.getSource(), new ItemStack(ItemInit.SHINY_CHARM.get(),
                                        IntegerArgumentType.getInteger(ctx, "count")))))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveItemToTarget(ctx, new ItemStack(ItemInit.SHINY_CHARM.get())))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> giveItemToTarget(ctx, new ItemStack(ItemInit.SHINY_CHARM.get(),
                                                IntegerArgumentType.getInteger(ctx, "count")))))))
                .then(Commands.literal("type")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    POKEMON_TYPES.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String type = StringArgumentType.getString(context, "type");
                                    if (!POKEMON_TYPES.contains(type.toLowerCase())) {
                                        context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                        return 0;
                                    }

                                    ItemStack typeAttractor = new ItemStack(ItemInit.TYPE_ATTRACTOR.get());
                                    ((TypeAttractorItem)typeAttractor.getItem()).setType(typeAttractor, type);
                                    return giveItem(context.getSource(), typeAttractor);
                                })
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            String type = StringArgumentType.getString(context, "type");
                                            int count = IntegerArgumentType.getInteger(context, "count");

                                            if (!POKEMON_TYPES.contains(type.toLowerCase())) {
                                                context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                                return 0;
                                            }

                                            ItemStack typeAttractor = new ItemStack(ItemInit.TYPE_ATTRACTOR.get(), count);
                                            ((TypeAttractorItem)typeAttractor.getItem()).setType(typeAttractor, type);
                                            return giveItem(context.getSource(), typeAttractor);
                                        }))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            String type = StringArgumentType.getString(context, "type");
                                            ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");

                                            if (!POKEMON_TYPES.contains(type.toLowerCase())) {
                                                context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                                return 0;
                                            }

                                            ItemStack typeAttractor = new ItemStack(ItemInit.TYPE_ATTRACTOR.get());
                                            ((TypeAttractorItem)typeAttractor.getItem()).setType(typeAttractor, type);
                                            return giveItem(player, typeAttractor, context.getSource());
                                        })
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> {
                                                    String type = StringArgumentType.getString(context, "type");
                                                    int count = IntegerArgumentType.getInteger(context, "count");
                                                    ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");

                                                    if (!POKEMON_TYPES.contains(type.toLowerCase())) {
                                                        context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                                        return 0;
                                                    }

                                                    ItemStack typeAttractor = new ItemStack(ItemInit.TYPE_ATTRACTOR.get(), count);
                                                    ((TypeAttractorItem)typeAttractor.getItem()).setType(typeAttractor, type);
                                                    return giveItem(player, typeAttractor, context.getSource());
                                                })))))
                // Add a debug command to check if mod is working
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GREEN +
                                    "CustomConsumables mod is loaded and working!"), false);

                            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.YELLOW +
                                    "Pixelmon detected: " + (pixelmonLoaded ? "Yes" : "No")), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.AQUA +
                                    "Available items:"), false);

                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GOLD +
                                    " - Legendary Lure (1% chance to spawn a legendary)"), false);
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.AQUA +
                                    " - Shiny Charm (50% chance to spawn a shiny)"), false);
                            ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.RED +
                                    " - Type Attractor (for " + POKEMON_TYPES.size() + " types)"), false);

                            return 1;
                        }));

        // Register the command
        dispatcher.register(customitemCommand);

        CustomConsumables.getLogger().info("Registered CustomConsumables commands successfully");
    }

    private static int giveItemToTarget(CommandContext<CommandSource> context, ItemStack stack) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgument.getPlayer(context, "player");
        return giveItem(player, stack, context.getSource());
    }

    private static int giveItem(CommandSource source, ItemStack stack) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrException();
        return giveItem(player, stack, source);
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