Custom Consumables
A Minecraft Forge mod that adds special consumable items for enhancing Pixelmon gameplay.
Features
This mod adds several consumable items that enhance various aspects of Pokémon spawning:

Legendary Lure: 1% chance to spawn a legendary in the server
Shiny Charm: X percentage to instantly spawn a shiny on your player
XXL Candy: Right click to give a pokemon 100,000 experience.
Requirements

Minecraft 1.16.5
Forge 36.1.0+
Pixelmon Reforged 9.1.0+

Building from Source

Clone this repository
Run the following command:
Copy./gradlew build

The built JAR file will be in build/libs/customconsumables-1.0.jar

Installation

Install Minecraft Forge for Minecraft 1.16.5
Install Pixelmon Reforged 9.1.0+
Place the customconsumables-1.0.jar file into your mods folder
Launch Minecraft

Usage
Each item can be crafted using the included recipes. Right-click while holding an item to use it.
Development Notes
This mod uses a soft dependency approach to Pixelmon integration:

We don't directly reference Pixelmon classes at compile time
We use NBT data to store effects that Pixelmon can read
The mod will work even without Pixelmon, but the effects will only function when Pixelmon is present

To add actual Pixelmon integration, you would:

Add Pixelmon as a development dependency
Implement proper event handlers for Pixelmon events
Update the PixelmonCompat class to use actual Pixelmon API calls

License
All rights reserved