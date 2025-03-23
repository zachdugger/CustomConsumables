package com.blissy.gemextension;

import me.realized.tokenmanager.api.TokenManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GemExtension - An extension for TokenManager that adds gem currency
 */
public class GemExtensionPlugin extends JavaPlugin implements Listener {

    private TokenManager tokenManager;
    private final Map<UUID, Long> gemCache = new ConcurrentHashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;
    private int defaultBalance;
    private String prefix;
    private GemConfig gemConfig;
    private GemDataManager gemDataManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Load configuration
        loadConfig();

        // Initialize GemConfig
        gemConfig = new GemConfig(this);

        // Setup data file
        setupDataFile();

        // Connect to TokenManager
        if (!hookTokenManager()) {
            getLogger().severe("Failed to hook into TokenManager! Disabling GemExtension...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize GemDataManager
        gemDataManager = new GemDataManager(this);

        // Register commands
        getCommand("gem").setExecutor(new GemCommand(this));
        getCommand("gemadmin").setExecutor(new GemAdminCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GemListener(this), this);

        // Schedule auto-save
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveAllData, 6000L, 6000L);

        getLogger().info("GemExtension has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data
        saveAllData();

        getLogger().info("GemExtension has been disabled!");
    }

    /**
     * Load configuration values
     */
    private void loadConfig() {
        FileConfiguration config = getConfig();

        defaultBalance = config.getInt("default-balance", 10);
        prefix = ChatColor.translateAlternateColorCodes('&',
                config.getString("prefix", "&a[Gems]"));
    }

    /**
     * Set up the data file
     */
    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "gemdata.yml");

        if (!dataFile.exists()) {
            saveResource("gemdata.yml", false);
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAllData();
    }

    /**
     * Load all gem data
     */
    private void loadAllData() {
        if (dataConfig.contains("gems")) {
            for (String uuidStr : dataConfig.getConfigurationSection("gems").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long gems = dataConfig.getLong("gems." + uuidStr);
                    gemCache.put(uuid, gems);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid UUID in gemdata.yml: " + uuidStr);
                }
            }
        }
    }

    /**
     * Save all gem data
     */
    public void saveAllData() {
        // Clear current data
        dataConfig.set("gems", null);

        // Save all cached data
        for (Map.Entry<UUID, Long> entry : gemCache.entrySet()) {
            dataConfig.set("gems." + entry.getKey().toString(), entry.getValue());
        }

        // Save to file
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save gemdata.yml: " + e.getMessage());
        }
    }

    /**
     * Hook into TokenManager
     * @return true if successful, false otherwise
     */
    private boolean hookTokenManager() {
        if (Bukkit.getPluginManager().getPlugin("TokenManager") == null) {
            return false;
        }

        RegisteredServiceProvider<TokenManager> provider =
                Bukkit.getServicesManager().getRegistration(TokenManager.class);

        if (provider == null) {
            return false;
        }

        tokenManager = provider.getProvider();
        return tokenManager != null;
    }

    /**
     * Handle player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load from storage if not in cache
        if (!gemCache.containsKey(uuid)) {
            long gems = dataConfig.getLong("gems." + uuid.toString(), defaultBalance);
            gemCache.put(uuid, gems);
        }
    }

    /**
     * Get a player's gem balance
     * @param player Player to check
     * @return Gem balance
     */
    public long getGems(Player player) {
        return gemCache.getOrDefault(player.getUniqueId(), (long)defaultBalance);
    }

    /**
     * Set a player's gem balance
     * @param player Player to set balance for
     * @param amount Amount to set
     */
    public void setGems(Player player, long amount) {
        gemCache.put(player.getUniqueId(), Math.max(0, amount));
    }

    /**
     * Add gems to a player's balance
     * @param player Player to add gems to
     * @param amount Amount to add
     * @return True if successful
     */
    public boolean addGems(Player player, long amount) {
        long current = getGems(player);
        setGems(player, current + amount);
        return true;
    }

    /**
     * Remove gems from a player's balance
     * @param player Player to remove gems from
     * @param amount Amount to remove
     * @return True if successful, false if player doesn't have enough gems
     */
    public boolean removeGems(Player player, long amount) {
        long current = getGems(player);
        if (current < amount) {
            return false;
        }

        setGems(player, current - amount);
        return true;
    }

    /**
     * Get the TokenManager instance
     * @return TokenManager instance
     */
    public TokenManager getTokenManager() {
        return tokenManager;
    }

    /**
     * Get the default gem balance
     * @return Default balance
     */
    public int getDefaultBalance() {
        return defaultBalance;
    }

    /**
     * Get the plugin's message prefix
     * @return Message prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Reload the plugin configuration
     */
    public void reloadGemConfig() {
        reloadConfig();
        loadConfig();
        if (gemConfig != null) {
            gemConfig.reloadConfig();
        }
    }

    /**
     * Get the GemConfig instance
     * @return GemConfig instance
     */
    public GemConfig getGemConfig() {
        return gemConfig;
    }

    /**
     * Get the GemDataManager instance
     * @return GemDataManager instance
     */
    public GemDataManager getGemDataManager() {
        return gemDataManager;
    }
}