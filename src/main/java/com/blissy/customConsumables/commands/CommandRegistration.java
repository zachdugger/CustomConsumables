package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.TypeAttractorItem;
import com.blissy.customConsumables.init.ItemInit;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.compat.PixelmonIntegration;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * Command handler for Custom Consumables
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class CommandRegistration {

    // Get the list of valid Pokémon types directly from the TypeAttractorItem
    private static final List<String> POKEMON_TYPES = TypeAttractorItem.getValidTypes();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CustomConsumables.getLogger().info("Registering CustomConsumables commands");

        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // Register the main command
        LiteralArgumentBuilder<CommandSource> customitemCommand = Commands.literal("customitem")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                // Type items
                .then(Commands.literal("type")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    POKEMON_TYPES.forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String type = StringArgumentType.getString(context, "type");
                                    if (!TypeAttractorItem.isValidType(type.toLowerCase())) {
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

                                            if (!TypeAttractorItem.isValidType(type.toLowerCase())) {
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

                                            if (!TypeAttractorItem.isValidType(type.toLowerCase())) {
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

                                                    if (!TypeAttractorItem.isValidType(type.toLowerCase())) {
                                                        context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                                        return 0;
                                                    }

                                                    ItemStack typeAttractor = new ItemStack(ItemInit.TYPE_ATTRACTOR.get(), count);
                                                    ((TypeAttractorItem)typeAttractor.getItem()).setType(typeAttractor, type);
                                                    return giveItem(player, typeAttractor, context.getSource());
                                                })))))
                // Apply type boost directly via effect
                .then(Commands.literal("effect")
                        .then(Commands.literal("type")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            POKEMON_TYPES.forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String type = StringArgumentType.getString(context, "type").toLowerCase();
                                            if (!TypeAttractorItem.isValidType(type)) {
                                                context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                                return 0;
                                            }

                                            ServerPlayerEntity player = context.getSource().getPlayerOrException();

                                            // Default 3 minutes (3600 ticks) with 10x multiplier
                                            PlayerEffectManager.applyTypeAttractorEffect(player, type, 3600, 1000.0f);

                                            // Also register with Pixelmon's system
                                            PixelmonIntegration.applyTypeBoost(player, type);

                                            String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
                                            context.getSource().sendSuccess(
                                                    new StringTextComponent(TextFormatting.GREEN + "Applied " + TextFormatting.BOLD +
                                                            typeName + TextFormatting.RESET + TextFormatting.GREEN +
                                                            " Type Attractor effect for 3 minutes"),
                                                    true);

                                            return 1;
                                        })
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                                                .executes(context -> {
                                                    String type = StringArgumentType.getString(context, "type").toLowerCase();
                                                    int durationSeconds = IntegerArgumentType.getInteger(context, "duration");

                                                    if (!TypeAttractorItem.isValidType(type)) {
                                                        context.getSource().sendFailure(new StringTextComponent("Invalid Pokémon type: " + type));
                                                        return 0;
                                                    }

                                                    ServerPlayerEntity player = context.getSource().getPlayerOrException();

                                                    // Convert seconds to ticks (20 ticks per second)
                                                    int durationTicks = durationSeconds * 20;

                                                    // Apply effect
                                                    PlayerEffectManager.applyTypeAttractorEffect(player, type, durationTicks, 1000.0f);

                                                    // Register with Pixelmon too
                                                    PixelmonIntegration.applyTypeBoost(player, type);

                                                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
                                                    context.getSource().sendSuccess(
                                                            new StringTextComponent(TextFormatting.GREEN + "Applied " + TextFormatting.BOLD +
                                                                    typeName + TextFormatting.RESET + TextFormatting.GREEN +
                                                                    " Type Attractor effect for " + durationSeconds + " seconds"),
                                                            true);

                                                    return 1;
                                                })))))
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
                                    "Available type attractors:"), false);

                            for (String type : POKEMON_TYPES) {
                                ctx.getSource().sendSuccess(new StringTextComponent(" - " + TextFormatting.GREEN +
                                        type.substring(0, 1).toUpperCase() + type.substring(1)), false);
                            }

                            // Check for active effects
                            try {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrException();

                                boolean hasTypeEffect = PlayerEffectManager.hasTypeAttractorEffect(player);

                                if (hasTypeEffect) {
                                    String type = PlayerEffectManager.getTypeAttractorType(player);
                                    int duration = PlayerEffectManager.getRemainingTypeBoostTime(player);
                                    float multiplier = PlayerEffectManager.getTypeAttractorChance(player, 0) / 100.0f;

                                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GREEN +
                                            "Active Type Attractor: " + TextFormatting.BOLD + typeName +
                                            TextFormatting.RESET + TextFormatting.GREEN + " for " + formatTime(duration) +
                                            " (" + (multiplier * 100) + "% boost)"), false);
                                } else {
                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GRAY +
                                            "No active effects"), false);

                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.YELLOW +
                                            "Use /customitem type <type> to get a Type Attractor"), false);
                                }
                            } catch (Exception e) {
                                // Not a player, skip effect check
                                ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.YELLOW +
                                        "Commands must be run as a player to see active effects"), false);
                            }

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

            // If this is a Type Attractor, also show type information
            if (stack.getItem() instanceof TypeAttractorItem && stack.hasTag()) {
                String type = stack.getTag().getString("type");
                if (!type.isEmpty()) {
                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
                    String message = "This is a " + typeName + " Type Attractor. Use it to attract " + typeName + " type Pokémon!";

                    player.sendMessage(new StringTextComponent(TextFormatting.GREEN + message), player.getUUID());
                }
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

    /**
     * Format ticks into a readable time string
     */
    private static String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;

        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }
}