package com.blissy.gemextension;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
 * Main command handler for /gem command
 */
public class GemCommand implements CommandExecutor, TabCompleter {

    private final GemExtensionPlugin plugin;
    private final List<String> subCommands = Arrays.asList("balance", "send", "help");

    /**
     * Constructor
     * @param plugin GemExtensionPlugin instance
     */
    public GemCommand(GemExtensionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Get prefix for messages
        String prefix = plugin.getPrefix() + " ";

        // Default command - show balance
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;
            long gems = plugin.getGems(player);

            sender.sendMessage(prefix + ChatColor.GREEN + "You have " +
                    ChatColor.GOLD + formatNumber(gems) + ChatColor.GREEN + " gems.");
            return true;
        }

        // Subcommand handling
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "balance":
            case "bal":
                handleBalanceCommand(sender, args, prefix);
                break;

            case "send":
            case "pay":
            case "give":
                handleSendCommand(sender, args, prefix);
                break;

            case "help":
                showHelp(sender, prefix);
                break;

            default:
                sender.sendMessage(prefix + ChatColor.RED + "Unknown subcommand. Use /gem help for available commands.");
                break;
        }

        return true;
    }

    /**
     * Handle the balance command
     */
    private void handleBalanceCommand(CommandSender sender, String[] args, String prefix) {
        // Check other player's balance
        if (args.length > 1) {
            if (!sender.hasPermission("gemextension.balance.others")) {
                sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to check other players' balances.");
                return;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + ChatColor.RED + "Player not found.");
                return;
            }

            long gems = plugin.getGems(target);
            sender.sendMessage(prefix + ChatColor.GREEN + target.getName() + " has " +
                    ChatColor.GOLD + formatNumber(gems) + ChatColor.GREEN + " gems.");
            return;
        }

        // Check own balance
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "This command can only be used by players.");
            return;
        }

        Player player = (Player) sender;
        long gems = plugin.getGems(player);

        sender.sendMessage(prefix + ChatColor.GREEN + "You have " +
                ChatColor.GOLD + formatNumber(gems) + ChatColor.GREEN + " gems.");
    }

    /**
     * Handle the send command
     */
    private void handleSendCommand(CommandSender sender, String[] args, String prefix) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "This command can only be used by players.");
            return;
        }

        if (!sender.hasPermission("gemextension.send")) {
            sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to send gems.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(prefix + ChatColor.RED + "Usage: /gem send <player> <amount>");
            return;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Player not found.");
            return;
        }

        if (player.equals(target)) {
            sender.sendMessage(prefix + ChatColor.RED + "You cannot send gems to yourself.");
            return;
        }

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

        long playerGems = plugin.getGems(player);
        if (playerGems < amount) {
            sender.sendMessage(prefix + ChatColor.RED + "You don't have enough gems. You need " +
                    amount + " but only have " + playerGems + ".");
            return;
        }

        // Transfer the gems
        plugin.removeGems(player, amount);
        plugin.addGems(target, amount);

        // Send messages
        sender.sendMessage(prefix + ChatColor.GREEN + "You sent " + ChatColor.GOLD +
                formatNumber(amount) + ChatColor.GREEN + " gems to " + target.getName() + ".");

        target.sendMessage(prefix + ChatColor.GREEN + "You received " + ChatColor.GOLD +
                formatNumber(amount) + ChatColor.GREEN + " gems from " + player.getName() + ".");
    }

    /**
     * Show help message
     */
    private void showHelp(CommandSender sender, String prefix) {
        sender.sendMessage(ChatColor.GREEN + "--------- " + prefix + "Help ---------");
        sender.sendMessage(ChatColor.GOLD + "/gem " + ChatColor.YELLOW + "- Show your gem balance");
        sender.sendMessage(ChatColor.GOLD + "/gem balance [player] " + ChatColor.YELLOW + "- Check gem balance");
        sender.sendMessage(ChatColor.GOLD + "/gem send <player> <amount> " + ChatColor.YELLOW + "- Send gems to a player");

        if (sender.hasPermission("gemextension.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/gemadmin " + ChatColor.YELLOW + "- Admin commands");
        }
    }

    /**
     * Format a number with commas
     */
    private String formatNumber(long number) {
        return NumberFormat.getNumberInstance(Locale.US).format(number);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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
            if (args[0].equalsIgnoreCase("balance") && sender.hasPermission("gemextension.balance.others") ||
                    args[0].equalsIgnoreCase("send") && sender.hasPermission("gemextension.send")) {
                String arg = args[1].toLowerCase();
                completions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3) {
            // Third argument - provide suggested amounts for send command
            if (args[0].equalsIgnoreCase("send") && sender.hasPermission("gemextension.send")) {
                String arg = args[2].toLowerCase();
                if (arg.isEmpty()) {
                    completions.add("10");
                    completions.add("50");
                    completions.add("100");
                    completions.add("1000");
                }
            }
        }

        return completions;
    }
}