package com.blissy.gemextension;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration manager for GemExtension
 */
public class GemConfig {

    private final GemExtensionPlugin plugin;
    private int defaultBalance;
    private int sendMin;
    private int sendMax;
    private int topUpdateInterval;
    private String prefix;

    /**
     * Constructor
     * @param plugin GemExtensionPlugin instance
     */
    public GemConfig(GemExtensionPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load configuration from config.yml
     */
    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();

        // Reload config
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Load values
        defaultBalance = config.getInt("default-balance", 10);
        sendMin = config.getInt("send-amount-limit.min", 1);
        sendMax = config.getInt("send-amount-limit.max", 1000);
        topUpdateInterval = config.getInt("balance-top-update-interval", 5);
        prefix = config.getString("prefix", "&a[Gems]");
    }

    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Get the default gem balance for new players
     * @return Default balance
     */
    public int getDefaultBalance() {
        return defaultBalance;
    }

    /**
     * Get the minimum amount that can be sent
     * @return Minimum send amount
     */
    public int getSendMin() {
        return sendMin;
    }

    /**
     * Get the maximum amount that can be sent
     * @return Maximum send amount, or -1 if no limit
     */
    public int getSendMax() {
        return sendMax;
    }

    /**
     * Get the update interval for top gems list in minutes
     * @return Update interval
     */
    public int getTopUpdateInterval() {
        return topUpdateInterval;
    }

    /**
     * Get the plugin's message prefix
     * @return Message prefix
     */
    public String getPrefix() {
        return prefix;
    }
}