GemExtension for TokenManager
GemExtension is an addon plugin for TokenManager that adds a second currency called "gems" to your server.
Features

Second currency system that works alongside TokenManager
Full command set for managing gems
Admin commands for giving, taking, and setting gem balances
Top gems leaderboard
Permission-based access to commands
Configurable settings

Requirements

Spigot/Paper 1.13+
TokenManager plugin installed and configured

Installation

Make sure TokenManager is installed and working properly
Download the GemExtension.jar file
Place the jar file in your server's plugins folder
Restart your server
Configure the plugin in the plugins/GemExtension/config.yml file

Commands
Player Commands

/gem - Show your gem balance
/gem balance [player] - Check gem balance
/gem send <player> <amount> - Send gems to another player
/gem top - View top gem holders
/gem help - Show help information

Admin Commands

/gemadmin give <player> <amount> - Give gems to a player
/gemadmin take <player> <amount> - Take gems from a player
/gemadmin set <player> <amount> - Set a player's gem balance
/gemadmin reload - Reload the configuration
/gemadmin update - Force update the top gems list

Permissions

gemextension.use - Access to basic gem commands
gemextension.balance - Check own gem balance
gemextension.balance.others - Check other players' gem balances
gemextension.send - Send gems to other players
gemextension.top - View top gem holders
gemextension.top.self - View own rank in top gems
gemextension.admin - Access to admin commands

Configuration
The plugin's configuration can be modified in the config.yml file:
yamlCopy# Default gem balance for new players
default-balance: 10

# Send command limits
send-amount-limit:
  min: 1        # Minimum amount that can be sent
  max: 1000     # Maximum amount that can be sent (0 for no limit)

# Top gems list update interval (minutes)
balance-top-update-interval: 5
Support
If you encounter any issues or have questions, please open an issue on the GitHub repository.
License
GemExtension is released under the MIT License. See the LICENSE file for details.