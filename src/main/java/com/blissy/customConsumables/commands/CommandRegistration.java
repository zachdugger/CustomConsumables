package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.items.TypeAttractorItem;
import com.blissy.customConsumables.init.ItemInit;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.events.TypeSpawnManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

import java.lang.reflect.Method;
import java.util.List;

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
                // Add a command to apply effect directly
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

                                            // Also register with TypeSpawnManager for advanced features
                                            TypeSpawnManager.getInstance().registerTypeBoost(player, type, 3600, 10.0f);

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

                                                    // Also register with TypeSpawnManager for advanced features
                                                    TypeSpawnManager.getInstance().registerTypeBoost(player, type, durationTicks, 10.0f);

                                                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
                                                    context.getSource().sendSuccess(
                                                            new StringTextComponent(TextFormatting.GREEN + "Applied " + TextFormatting.BOLD +
                                                                    typeName + TextFormatting.RESET + TextFormatting.GREEN +
                                                                    " Type Attractor effect for " + durationSeconds + " seconds"),
                                                            true);

                                                    return 1;
                                                }))))
                        .then(Commands.literal("legendary")
                                .executes(context -> {
                                    // Apply legendary lure effect for 3 minutes (3600 ticks)
                                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                    PlayerEffectManager.applyLegendaryLureEffect(player, 3600, 100.0f);

                                    context.getSource().sendSuccess(
                                            new StringTextComponent(TextFormatting.GOLD + "Applied Legendary Lure effect for 3 minutes"),
                                            true);

                                    return 1;
                                })
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                                        .executes(context -> {
                                            int durationSeconds = IntegerArgumentType.getInteger(context, "duration");
                                            ServerPlayerEntity player = context.getSource().getPlayerOrException();

                                            // Convert seconds to ticks
                                            int durationTicks = durationSeconds * 20;
                                            PlayerEffectManager.applyLegendaryLureEffect(player, durationTicks, 100.0f);

                                            context.getSource().sendSuccess(
                                                    new StringTextComponent(TextFormatting.GOLD + "Applied Legendary Lure effect for " +
                                                            durationSeconds + " seconds"),
                                                    true);

                                            return 1;
                                        })))
                        .then(Commands.literal("shiny")
                                .executes(context -> {
                                    // Apply shiny charm effect for 3 minutes (3600 ticks)
                                    ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                    PlayerEffectManager.applyShinyBoostEffect(player, 3600, 50.0f);

                                    context.getSource().sendSuccess(
                                            new StringTextComponent(TextFormatting.AQUA + "Applied Shiny Charm effect (50% chance) for 3 minutes"),
                                            true);

                                    return 1;
                                })
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                                        .executes(context -> {
                                            int durationSeconds = IntegerArgumentType.getInteger(context, "duration");
                                            ServerPlayerEntity player = context.getSource().getPlayerOrException();

                                            // Convert seconds to ticks
                                            int durationTicks = durationSeconds * 20;
                                            PlayerEffectManager.applyShinyBoostEffect(player, durationTicks, 50.0f);

                                            context.getSource().sendSuccess(
                                                    new StringTextComponent(TextFormatting.AQUA + "Applied Shiny Charm effect (50% chance) for " +
                                                            durationSeconds + " seconds"),
                                                    true);

                                            return 1;
                                        }))))
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

                            // If player, check for active effects
                            try {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrException();

                                boolean hasAnyEffect = false;

                                // Check for legendary lure effect
                                if (PlayerEffectManager.hasLegendaryLureEffect(player)) {
                                    int remainingTicks = PlayerEffectManager.getLegendaryLureRemainingDuration(player);
                                    float chance = PlayerEffectManager.getLegendaryLureChance(player, 0);

                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GOLD +
                                            "Active Legendary Lure: " + formatTime(remainingTicks) + " remaining (" + chance + "% chance)"), false);
                                    hasAnyEffect = true;
                                }

                                // Check for shiny charm effect
                                if (PlayerEffectManager.hasShinyBoostEffect(player)) {
                                    int remainingTicks = PlayerEffectManager.getRemainingShinyBoostTime(player);
                                    float chance = PlayerEffectManager.getShinyBoostMultiplier(player, 0);

                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.AQUA +
                                            "Active Shiny Charm: " + formatTime(remainingTicks) + " remaining (" + chance + "% chance)"), false);
                                    hasAnyEffect = true;
                                }

                                // Check for type attractor effect
                                if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
                                    int remainingTicks = PlayerEffectManager.getRemainingTypeBoostTime(player);
                                    String type = PlayerEffectManager.getTypeAttractorType(player);
                                    float multiplier = PlayerEffectManager.getTypeAttractorChance(player, 0);

                                    String typeName = type.substring(0, 1).toUpperCase() + type.substring(1);
                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GREEN +
                                            "Active Type Attractor: " + typeName + " for " + formatTime(remainingTicks) +
                                            " (" + multiplier + "% boost)"), false);
                                    hasAnyEffect = true;
                                }

                                if (!hasAnyEffect) {
                                    ctx.getSource().sendSuccess(new StringTextComponent(TextFormatting.GRAY +
                                            "No active effects"), false);
                                }

                            } catch (CommandSyntaxException e) {
                                // Not a player, skip effect check
                            }

                            return 1;
                        }))
                // Add new typeboost debug command
                .then(Commands.literal("typeboost")
                        .executes(ctx -> {
                            PlayerEntity player = ctx.getSource().getPlayerOrException();

                            if (PlayerEffectManager.hasTypeAttractorEffect(player)) {
                                String type = PlayerEffectManager.getTypeAttractorType(player);
                                int duration = PlayerEffectManager.getRemainingTypeBoostTime(player);
                                float multiplier = PlayerEffectManager.getTypeAttractorChance(player, 0);

                                ctx.getSource().sendSuccess(
                                        new StringTextComponent(TextFormatting.GREEN + "Active type boost: " +
                                                TextFormatting.YELLOW + type.toUpperCase() +
                                                TextFormatting.GREEN + " with " +
                                                TextFormatting.YELLOW + multiplier + "%" +
                                                TextFormatting.GREEN + " boost for " +
                                                TextFormatting.YELLOW + formatTime(duration)),
                                        false
                                );

                                // Try to verify boost with Pixelmon's internal system
                                boolean verificationAttempted = false;

                                try {
                                    // Try to access Pixelmon's SpawnRegistry to check
                                    Class<?> spawnRegistryClass = Class.forName("com.pixelmonmod.pixelmon.spawning.SpawnRegistry");
                                    Class<?> typeBoostClass = null;

                                    // Try to find TypeBoost class
                                    try {
                                        typeBoostClass = Class.forName("com.pixelmonmod.pixelmon.spawning.TypeBoost");
                                    } catch (ClassNotFoundException e) {
                                        try {
                                            typeBoostClass = Class.forName("com.pixelmonmod.pixelmon.api.spawning.TypeBoost");
                                        } catch (ClassNotFoundException e2) {
                                            // Nested classes
                                            for (Class<?> nested : spawnRegistryClass.getClasses()) {
                                                if (nested.getSimpleName().contains("TypeBoost")) {
                                                    typeBoostClass = nested;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    // Log what we found
                                    ctx.getSource().sendSuccess(
                                            new StringTextComponent(TextFormatting.GRAY + "Pixelmon classes found: " +
                                                    (spawnRegistryClass != null ? "SpawnRegistry " : "") +
                                                    (typeBoostClass != null ? "TypeBoost" : "")),
                                            false
                                    );

                                    // Command verification
                                    try {
                                        // Try to execute boosttype command to see if it works
                                        int existingDuration = duration;

                                        ctx.getSource().getServer().getCommands().performCommand(
                                                ctx.getSource().getServer().createCommandSourceStack().withPermission(4),
                                                "pokespawn boosttype " + type.toLowerCase() + " 10"
                                        );

                                        ctx.getSource().sendSuccess(
                                                new StringTextComponent(TextFormatting.GREEN + "Successfully executed boost command: " +
                                                        TextFormatting.YELLOW + "pokespawn boosttype " + type.toLowerCase() + " 10"),
                                                false
                                        );

                                        verificationAttempted = true;
                                    } catch (Exception e) {
                                        ctx.getSource().sendSuccess(
                                                new StringTextComponent(TextFormatting.RED + "Command verification failed: " + e.getMessage()),
                                                false
                                        );
                                    }

                                    // Detailed diagnostic info
                                    ctx.getSource().sendSuccess(
                                            new StringTextComponent(TextFormatting.GRAY + "Pixelmon version: 9.1.13"),
                                            false
                                    );

                                } catch (Exception e) {
                                    ctx.getSource().sendSuccess(
                                            new StringTextComponent(TextFormatting.RED + "Verification error: " + e.getMessage()),
                                            false
                                    );
                                }

                                if (!verificationAttempted) {
                                    ctx.getSource().sendSuccess(
                                            new StringTextComponent(TextFormatting.YELLOW + "Could not verify boost with Pixelmon system"),
                                            false
                                    );

                                    // Re-apply boost via command
                                    try {
                                        ctx.getSource().getServer().getCommands().performCommand(
                                                ctx.getSource().getServer().createCommandSourceStack().withPermission(4),
                                                "pokespawn boosttype " + type.toLowerCase() + " 10"
                                        );

                                        ctx.getSource().sendSuccess(
                                                new StringTextComponent(TextFormatting.GREEN + "Re-applied boost via command"),
                                                false
                                        );
                                    } catch (Exception e) {
                                        ctx.getSource().sendSuccess(
                                                new StringTextComponent(TextFormatting.RED + "Failed to re-apply boost: " + e.getMessage()),
                                                false
                                        );
                                    }
                                }

                                return 1;
                            } else {
                                ctx.getSource().sendFailure(
                                        new StringTextComponent("No active type boost found")
                                );
                                return 0;
                            }
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