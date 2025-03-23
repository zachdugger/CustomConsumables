package com.blissy.gemextension;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages gem data storage
 */
public class GemDataManager implements Listener {

    private final GemExtensionPlugin plugin;
    private final Map<UUID, Long> gemCache = new ConcurrentHashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;
    private List<TopGemHolder> topGems = new ArrayList<>();
    private long lastTopUpdate = 0;

    /**
     * Constructor
     * @param plugin GemExtensionPlugin instance
     */
    public GemDataManager(GemExtensionPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "gemdata.yml");

        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            plugin.saveResource("gemdata.yml", false);
        }

        // Load data
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAllData();

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Schedule regular saving
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllData, 6000L, 6000L); // Save every 5 minutes

        // Schedule top gems updates
        int updateInterval = plugin.getGemConfig().getTopUpdateInterval();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateTopGems, 100L, updateInterval * 1200L);
    }

    /**
     * Load all data from storage
     */
    private void loadAllData() {
        if (dataConfig.contains("gems")) {
            for (String uuidStr : dataConfig.getConfigurationSection("gems").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long gems = dataConfig.getLong("gems." + uuidStr);
                    gemCache.put(uuid, gems);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in gemdata.yml: " + uuidStr);
                }
            }
        }

        // Load initial top gems
        updateTopGems();
    }

    /**
     * Save all data to storage
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
            plugin.getLogger().severe("Failed to save gemdata.yml: " + e.getMessage());
        }
    }

    /**
     * Get a player's gem balance
     * @param player Player to check
     * @return Gem balance
     */
    public long getGems(Player player) {
        return getGems(player.getUniqueId());
    }

    /**
     * Get a player's gem balance
     * @param uuid UUID of player to check
     * @return Gem balance
     */
    public long getGems(UUID uuid) {
        return gemCache.getOrDefault(uuid, 0L);
    }

    /**
     * Set a player's gem balance
     * @param player Player to set balance for
     * @param amount Amount to set
     */
    public void setGems(Player player, long amount) {
        setGems(player.getUniqueId(), amount);
    }

    /**
     * Set a player's gem balance
     * @param uuid UUID of player to set balance for
     * @param amount Amount to set
     */
    public void setGems(UUID uuid, long amount) {
        gemCache.put(uuid, Math.max(0, amount));
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
     * Update the top gem holders
     */
    public void updateTopGems() {
        lastTopUpdate = System.currentTimeMillis();
        List<TopGemHolder> newTop = new ArrayList<>();

        // Convert cache to a sortable list
        Map<UUID, Long> tempMap = new HashMap<>(gemCache);

        // Sort by gem balance (descending)
        tempMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(10) // Top 10 players
                .forEach(entry -> {
                    UUID uuid = entry.getKey();
                    long gems = entry.getValue();
                    String name = getPlayerName(uuid);
                    newTop.add(new TopGemHolder(uuid, name, gems));
                });

        // Update the cached top list
        topGems = newTop;
    }

    /**
     * Get player's rank in the top gems list
     * @param player Player to check
     * @return Player's rank (1-based) or -1 if not in top players
     */
    public int getPlayerRank(Player player) {
        UUID uuid = player.getUniqueId();
        for (int i = 0; i < topGems.size(); i++) {
            if (topGems.get(i).getUuid().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Get the top gem holders
     * @return List of top gem holders
     */
    public List<TopGemHolder> getTopGems() {
        return topGems;
    }

    /**
     * Get the time of the last top update
     * @return Last update time in milliseconds
     */
    public long getLastTopUpdate() {
        return lastTopUpdate;
    }

    /**
     * Get a player's name from their UUID
     * @param uuid UUID to look up
     * @return Player name or "Unknown" if not found
     */
    private String getPlayerName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }

    /**
     * Load player data on join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load from storage if not in cache
        if (!gemCache.containsKey(uuid)) {
            long gems = dataConfig.getLong("gems." + uuid.toString(), plugin.getGemConfig().getDefaultBalance());
            gemCache.put(uuid, gems);
        }
    }

    /**
     * Class to represent a top gem holder
     */
    public static class TopGemHolder {
        private final UUID uuid;
        private final String name;
        private final long gems;

        public TopGemHolder(UUID uuid, String name, long gems) {
            this.uuid = uuid;
            this.name = name;
            this.gems = gems;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public long getGems() {
            return gems;
        }
    }
}