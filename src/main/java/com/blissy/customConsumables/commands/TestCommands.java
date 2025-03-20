package com.blissy.customConsumables.commands;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.events.PixelmonCommandHooks;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Adds test commands for verifying the legendary lure functionality
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class TestCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CustomConsumables.getLogger().info("Registering test commands for CustomConsumables");

        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();

        // Register a command to test legendary spawn
        dispatcher.register(Commands.literal("testlegendary")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .executes(context -> {
                    try {
                        ServerPlayerEntity player = context.getSource().getPlayerOrException();

                        CustomConsumables.getLogger().info("Player {} used testlegendary command", player.getName().getString());

                        // Check if player has the legendary lure effect
                        if (PlayerEffectManager.hasLegendaryLureEffect(player)) {
                            float chance = PlayerEffectManager.getLegendaryLureChance(player, 100.0f);
                            int remaining = PlayerEffectManager.getLegendaryLureRemainingDuration(player);

                            // Show effect status
                            context.getSource().sendSuccess(
                                    new StringTextComponent(TextFormatting.GOLD + "You have an active legendary lure!"), false);
                            context.getSource().sendSuccess(
                                    new StringTextComponent(TextFormatting.YELLOW + "Legendary chance: " +
                                            TextFormatting.GREEN + chance + "%" +
                                            TextFormatting.YELLOW + " (Duration: " +
                                            TextFormatting.GREEN + (remaining / 20) + " seconds" +
                                            TextFormatting.YELLOW + " remaining)"), false);

                            // Attempt to force a legendary spawn check
                            boolean result = PixelmonCommandHooks.forceTestLegendaryCheck(player);

                            if (!result) {
                                context.getSource().sendSuccess(
                                        new StringTextComponent(TextFormatting.YELLOW +
                                                "Could not trigger a legendary spawn check."), false);
                                context.getSource().sendSuccess(
                                        new StringTextComponent(TextFormatting.YELLOW +
                                                "Make sure Pixelmon is installed and you have permission to use pokespawn commands."), false);
                            }
                        } else {
                            // Player doesn't have the effect
                            context.getSource().sendSuccess(
                                    new StringTextComponent(TextFormatting.RED +
                                            "You don't have an active legendary lure effect!"), false);
                            context.getSource().sendSuccess(
                                    new StringTextComponent(TextFormatting.YELLOW +
                                            "Use '/customitem legendary' to get a lure first."), false);
                        }
                    } catch (Exception e) {
                        CustomConsumables.getLogger().error("Error in testlegendary command", e);
                        context.getSource().sendFailure(new StringTextComponent("Error: " + e.getMessage()));
                    }

                    return 1;
                }));

        // Register a command to apply a temporary test lure effect
        dispatcher.register(Commands.literal("applylegendary")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .executes(context -> {
                    try {
                        ServerPlayerEntity player = context.getSource().getPlayerOrException();

                        CustomConsumables.getLogger().info("Player {} used applylegendary command", player.getName().getString());

                        // Apply a 1-minute legendary lure with 100% chance
                        PlayerEffectManager.applyLegendaryLureEffect(player, 20 * 60, 100.0f);

                        context.getSource().sendSuccess(
                                new StringTextComponent(TextFormatting.GREEN +
                                        "Applied a 1-minute legendary lure effect with 100% chance!"), false);
                        context.getSource().sendSuccess(
                                new StringTextComponent(TextFormatting.YELLOW +
                                        "Use '/testlegendary' to test it or '/testmode' for continuous testing."), false);
                        context.getSource().sendSuccess(
                                new StringTextComponent(TextFormatting.YELLOW +
                                        "Or try '/pokespawn legendary' to force a spawn with your lure."), false);
                    } catch (Exception e) {
                        CustomConsumables.getLogger().error("Error in applylegendary command", e);
                        context.getSource().sendFailure(new StringTextComponent("Error: " + e.getMessage()));
                    }

                    return 1;
                })
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                        .executes(context -> {
                            try {
                                ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                CustomConsumables.getLogger().info("Player {} used applylegendary command with {} seconds",
                                        player.getName().getString(), seconds);

                                // Apply a custom duration legendary lure with 100% chance
                                PlayerEffectManager.applyLegendaryLureEffect(player, 20 * seconds, 100.0f);

                                context.getSource().sendSuccess(
                                        new StringTextComponent(TextFormatting.GREEN +
                                                "Applied a " + seconds + "-second legendary lure effect with 100% chance!"), false);
                                context.getSource().sendSuccess(
                                        new StringTextComponent(TextFormatting.YELLOW +
                                                "Use '/testlegendary' to test it or '/testmode' for continuous testing."), false);
                                context.getSource().sendSuccess(
                                        new StringTextComponent(TextFormatting.YELLOW +
                                                "Or try '/pokespawn legendary' to force a spawn with your lure."), false);
                            } catch (Exception e) {
                                CustomConsumables.getLogger().error("Error in applylegendary command", e);
                                context.getSource().sendFailure(new StringTextComponent("Error: " + e.getMessage()));
                            }

                            return 1;
                        })));

        // Add a command that starts test mode for frequent legendary checks
        dispatcher.register(Commands.literal("testmode")
                .requires(source -> source.hasPermission(2)) // Requires permission level 2 (op)
                .executes(context -> {
                    try {
                        ServerPlayerEntity player = context.getSource().getPlayerOrException();

                        CustomConsumables.getLogger().info("Player {} started test mode for 120 seconds", player.getName().getString());

                        // Start test mode for 2 minutes
                        PixelmonCommandHooks.startTestMode(player, 120);
                    } catch (Exception e) {
                        CustomConsumables.getLogger().error("Error in testmode command", e);
                        context.getSource().sendFailure(new StringTextComponent("Error: " + e.getMessage()));
                    }

                    return 1;
                })
                .then(Commands.argument("seconds", IntegerArgumentType.integer(10, 600))
                        .executes(context -> {
                            try {
                                ServerPlayerEntity player = context.getSource().getPlayerOrException();
                                int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                CustomConsumables.getLogger().info("Player {} started test mode for {} seconds",
                                        player.getName().getString(), seconds);

                                // Start test mode for specified duration
                                PixelmonCommandHooks.startTestMode(player, seconds);
                            } catch (Exception e) {
                                CustomConsumables.getLogger().error("Error in testmode command", e);
                                context.getSource().sendFailure(new StringTextComponent("Error: " + e.getMessage()));
                            }

                            return 1;
                        })));

        CustomConsumables.getLogger().info("Successfully registered test commands for CustomConsumables");
    }
}