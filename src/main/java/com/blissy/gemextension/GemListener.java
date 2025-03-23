package com.blissy.gemextension;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Event listener for the GemExtension plugin
 */
public class GemListener implements Listener {

    private final GemExtensionPlugin plugin;

    /**
     * Constructor
     * @param plugin GemExtensionPlugin instance
     */
    public GemListener(GemExtensionPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // This is handled in the GemDataManager, but we could add additional functionality here
    }

    /**
     * Handle player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data when they leave
        plugin.getGemDataManager().saveAllData();
    }
}