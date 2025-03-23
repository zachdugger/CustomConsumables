package com.blissy.gemextension;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Admin command handler for /gemadmin command
 */
public class GemAdminCommand implements CommandExecutor, TabCompleter {

    private final GemExtensionPlugin plugin;
    private final List<String> subCommands = Arrays.asList("give", "take", "set", "reload");

    /**
     * Constructor
     * @param plugin GemExtensionPlugin instance
     */
    public GemAdminCommand(GemExtensionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Get prefix for messages
        String prefix = plugin.getPrefix() + " ";

        // Check permission
        if (!sender.hasPermission("gemextension.admin")) {
            sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Show help if no arguments
        if (args.length == 0) {
            showHelp(sender, prefix);
            return true;
        }

        // Subcommand handling
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGiveCommand(sender, args, prefix);
                break;

            case "take":
                handleTakeCommand(sender, args, prefix);
                break;

            case "set":
                handleSetCommand(sender, args, prefix);
                break;

            case "reload":
                handleReloadCommand(sender, prefix);
                break;

            default:
                sender.sendMessage(prefix + ChatColor.RED + "Unknown subcommand. Use /gemadmin for help.");
                break;
        }

        return true;
    }

    /**
     * Handle the give command
     */
    private void handleGiveCommand(CommandSender sender, String[] args, String prefix) {
        if (args.length < 3) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /gemadmin give <player> <amount>");
            return;
        }

        // Get target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Player not found.");
            return;
        }

        // Parse amount
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ChatColor.RED + "Invalid amount.");
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix + ChatColor.RED + "Amount must be greater than 0.");
            return;
        }

        // Add gems to player
        plugin.addGems(target, amount);

        // Notify
        sender.sendMessage(prefix + ChatColor.GREEN + "Gave " + ChatColor.GOLD +
                formatNumber(amount) + ChatColor.GREEN + " gems to " + target.getName() + ".");

        target.sendMessage(prefix + ChatColor.GREEN + "You received " + ChatColor.GOLD +
                formatNumber(amount) + ChatColor.GREEN + " gems from an admin.");
    }

    /**
     * Handle the take command
     */
    private void handleTakeCommand(CommandSender sender, String[] args, String prefix) {
        if (args.length < 3) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /gemadmin take <player> <amount>");
            return;
        }

        // Get target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Player not found.");
            return;
        }

        // Parse amount
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ChatColor.RED + "Invalid amount.");
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix + ChatColor.RED + "Amount must be greater than 0.");
            return;
        }

        long playerGems = plugin.getGems(target);
        if (playerGems < amount) {
            sender.sendMessage(prefix + ChatColor.RED + "Player only has " +
                    formatNumber(playerGems) + " gems. Cannot take " + formatNumber(amount) + ".");
            return;
        }

        // Remove gems from player
        plugin.removeGems(target, amount);

        // Notify
        sender.sendMessage(prefix + ChatColor.GREEN + "Took " + ChatColor.GOLD +
                formatNumber(amount) + ChatColor.GREEN + " gems from " + target.getName() + ".");

        target.sendMessage(prefix + ChatColor.RED + "An admin has removed " + ChatColor.GOLD +
                formatNumber(amount) + ChatColor.RED + " gems from your account.");
    }

    /**
     * Handle the set command
     */
    private void handleSetCommand(CommandSender sender, String[] args, String prefix) {
        if (args.length < 3) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /gemadmin set <player> <amount>");
            return;
        }

        // Get target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Player not found.");
            return;
        }

        // Parse amount
        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ChatColor.RED + "Invalid amount.");
            return;
        }

        if (amount < 0) {
            sender.sendMessage(prefix + ChatColor.RED + "Amount cannot be negative.");
            return;
        }

        // Set player's gem balance
        plugin.setGems(target, amount);

        // Notify
        sender.sendMessage(prefix + ChatColor.GREEN + "Set " + target.getName() +
                "'s gem balance to " + ChatColor.GOLD + formatNumber(amount) + ChatColor.GREEN + ".");

        target.sendMessage(prefix + ChatColor.YELLOW + "Your gem balance has been set to " +
                ChatColor.GOLD + formatNumber(amount) + ChatColor.YELLOW + " by an admin.");
    }

    /**
     * Handle the reload command
     */
    private void handleReloadCommand(CommandSender sender, String prefix) {
        // Save current data
        plugin.saveAllData();

        // Reload config
        plugin.reloadGemConfig();

        sender.sendMessage(prefix + ChatColor.GREEN + "Configuration reloaded!");
    }

    /**
     * Show help message
     */
    private void showHelp(CommandSender sender, String prefix) {
        sender.sendMessage(ChatColor.GREEN + "--------- " + prefix + "Admin Commands ---------");
        sender.sendMessage(ChatColor.GOLD + "/gemadmin give <player> <amount> " + ChatColor.YELLOW + "- Give gems to a player");
        sender.sendMessage(ChatColor.GOLD + "/gemadmin take <player> <amount> " + ChatColor.YELLOW + "- Take gems from a player");
        sender.sendMessage(ChatColor.GOLD + "/gemadmin set <player> <amount> " + ChatColor.YELLOW + "- Set player's gem balance");
        sender.sendMessage(ChatColor.GOLD + "/gemadmin reload " + ChatColor.YELLOW + "- Reload configuration");
    }

    /**
     * Format a number with commas
     */
    private String formatNumber(long number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("gemextension.admin")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - provide subcommands
            String arg = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(arg)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            // Second argument - provide player names for relevant commands
            if (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("take") ||
                    args[0].equalsIgnoreCase("set")) {

                String arg = args[1].toLowerCase();
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            // Third argument - provide suggested amounts
            if (args[0].equalsIgnoreCase("give") ||
                    args[0].equalsIgnoreCase("take") ||
                    args[0].equalsIgnoreCase("set")) {

                String arg = args[2].toLowerCase();
                if (arg.isEmpty()) {
                    completions.add("100");
                    completions.add("1000");
                    completions.add("10000");
                }
            }
        }

        return completions;
    }
}