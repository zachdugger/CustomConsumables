package com.blissy.customConsumables.events;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.data.PokemonTypeDataHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages type-based spawn filtering and boosting
 */
@Mod.EventBusSubscriber(modid = CustomConsumables.MOD_ID)
public class TypeSpawnManager {
    private static TypeSpawnManager instance;
    private static final Random random = new Random();

    // Type boost tracking
    // Player UUID -> TypeBoostInfo
    private final Map<UUID, TypeBoostInfo> activeBoosts = new ConcurrentHashMap<>();

    // Reflection cache for Pixelmon classes
    private static boolean initialized = false;
    private static Class<?> pixelmonEntityClass = null;
    private static Method getPokemonMethod = null;
    private static Method getSpeciesMethod = null;
    private static Method getNameMethod = null;

    // Cooldown tracking for forced spawns
    private final Map<UUID, Long> spawnCooldowns = new ConcurrentHashMap<>();
    private static final long SPAWN_COOLDOWN_MS = 5000; // 5 second cooldown

    // Constants
    private static final String BOOST_TYPE_KEY = "typeBoostType";
    private static final String BOOST_DURATION_KEY = "typeBoostDuration";
    private static final String BOOST_MULTIPLIER_KEY = "typeBoostMultiplier";
    private static final String BOOST_POKEMON_KEY = "typeBoostPokemon";

    // Private constructor for singleton
    private TypeSpawnManager() {
        initialize();
    }

    /**
     * Get the singleton instance
     */
    public static TypeSpawnManager getInstance() {
        if (instance == null) {
            instance = new TypeSpawnManager();
        }
        return instance;
    }

    /**
     * Initialize reflection for Pixelmon classes
     */
    private void initialize() {
        if (initialized) return;

        try {
            // Only try to initialize if Pixelmon is loaded
            if (!ModList.get().isLoaded("pixelmon")) {
                return;
            }

            // Get the Pixelmon entity class
            pixelmonEntityClass = Class.forName("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");

            // Get method to access the Pokémon object
            getPokemonMethod = pixelmonEntityClass.getMethod("getPokemon");

            // Get methods to access species and name
            Class<?> pokemonClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
            getSpeciesMethod = pokemonClass.getMethod("getSpecies");

            Class<?> speciesClass = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.species.Species");
            getNameMethod = speciesClass.getMethod("getName");

            initialized = true;
            CustomConsumables.getLogger().info("TypeSpawnManager initialized successfully");
        } catch (Exception e) {
            CustomConsumables.getLogger().error("Error initializing TypeSpawnManager", e);
        }
    }

    /**
     * Register a type boost for a player
     * @param player The player to receive the boost
     * @param type The Pokémon type to boost
     * @param durationTicks Duration in ticks
     * @param multiplier Spawn rate multiplier
     * @param eligiblePokemon Set of Pokémon names that are eligible for spawning
     */
    public void registerTypeBoost(PlayerEntity player, String type, int durationTicks, float multiplier, Set<String> eligiblePokemon) {
        UUID playerId = player.getUUID();

        // Create boost info
        TypeBoostInfo boost = new TypeBoostInfo(type, durationTicks, multiplier, eligiblePokemon);

        // Save in our tracking map
        activeBoosts.put(playerId, boost);

        // Also save in player NBT for persistence
        saveBoostToPlayer(player, boost);

        CustomConsumables.getLogger().info(
                "Registered {} type boost for player {} with {} eligible Pokémon",
                type, player.getName().getString(), eligiblePokemon.size()
        );
    }

    /**
     * Save boost info to player NBT data
     */
    private void saveBoostToPlayer(PlayerEntity player, TypeBoostInfo boost) {
        CompoundNBT playerData = player.getPersistentData();
        CompoundNBT modData;

        if (playerData.contains(CustomConsumables.MOD_ID)) {
            modData = playerData.getCompound(CustomConsumables.MOD_ID);
        } else {
            modData = new CompoundNBT();
        }

        // Save basic boost info
        modData.putString(BOOST_TYPE_KEY, boost.type);
        modData.putInt(BOOST_DURATION_KEY, boost.remainingTicks);
        modData.putFloat(BOOST_MULTIPLIER_KEY, boost.multiplier);

        // Save eligible Pokémon as a serialized string (limited by NBT size)
        if (boost.eligiblePokemon != null && !boost.eligiblePokemon.isEmpty()) {
            // Join with commas, but limit to avoid NBT size issues
            List<String> limitedList = new ArrayList<>(boost.eligiblePokemon);
            if (limitedList.size() > 100) {
                limitedList = limitedList.subList(0, 100);
            }
            modData.putString(BOOST_POKEMON_KEY, String.join(",", limitedList));
        }

        // Save back to player
        playerData.put(CustomConsumables.MOD_ID, modData);
    }

    /**
     * Load boost info from player NBT data
     */
    private TypeBoostInfo loadBoostFromPlayer(PlayerEntity player) {
        CompoundNBT playerData = player.getPersistentData();

        if (playerData.contains(CustomConsumables.MOD_ID)) {
            CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);

            if (modData.contains(BOOST_TYPE_KEY) && modData.contains(BOOST_DURATION_KEY)) {
                String type = modData.getString(BOOST_TYPE_KEY);
                int duration = modData.getInt(BOOST_DURATION_KEY);
                float multiplier = modData.getFloat(BOOST_MULTIPLIER_KEY);

                // If duration is still positive, restore the boost
                if (duration > 0) {
                    Set<String> eligiblePokemon = new HashSet<>();

                    // Load eligible Pokémon if saved
                    if (modData.contains(BOOST_POKEMON_KEY)) {
                        String pokemonStr = modData.getString(BOOST_POKEMON_KEY);
                        if (pokemonStr != null && !pokemonStr.isEmpty()) {
                            String[] pokemonArray = pokemonStr.split(",");
                            eligiblePokemon.addAll(Arrays.asList(pokemonArray));
                        }
                    }

                    // If we don't have any eligible Pokémon loaded, get them from the data handler
                    if (eligiblePokemon.isEmpty()) {
                        eligiblePokemon = com.blissy.customConsumables.data.PokemonTypeDataHandler.getInstance().getPokemonOfType(type);
                    }

                    return new TypeBoostInfo(type, duration, multiplier, eligiblePokemon);
                }
            }
        }

        return null;
    }

    /**
     * Check if a player has an active type boost
     */
    public boolean hasActiveBoost(PlayerEntity player) {
        UUID playerId = player.getUUID();

        // Check in-memory cache first
        if (activeBoosts.containsKey(playerId)) {
            TypeBoostInfo boost = activeBoosts.get(playerId);
            return boost != null && boost.remainingTicks > 0;
        }

        // If not in cache, try to load from player NBT
        TypeBoostInfo boost = loadBoostFromPlayer(player);
        if (boost != null) {
            activeBoosts.put(playerId, boost);
            return true;
        }

        return false;
    }

    /**
     * Get the current boost info for a player
     */
    public TypeBoostInfo getBoostInfo(PlayerEntity player) {
        UUID playerId = player.getUUID();

        // Check in-memory cache first
        if (activeBoosts.containsKey(playerId)) {
            return activeBoosts.get(playerId);
        }

        // If not in cache, try to load from player NBT
        TypeBoostInfo boost = loadBoostFromPlayer(player);
        if (boost != null) {
            activeBoosts.put(playerId, boost);
        }

        return boost;
    }

    /**
     * Update boost durations and remove expired boosts
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }

        PlayerEntity player = event.player;
        TypeSpawnManager manager = getInstance();

        // Check for active boost
        UUID playerId = player.getUUID();
        TypeBoostInfo boost = manager.activeBoosts.get(playerId);

        if (boost != null) {
            // Update duration
            boost.remainingTicks--;

            // Save updated duration to player NBT
            CompoundNBT playerData = player.getPersistentData();
            if (playerData.contains(CustomConsumables.MOD_ID)) {
                CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);
                modData.putInt(BOOST_DURATION_KEY, boost.remainingTicks);
                playerData.put(CustomConsumables.MOD_ID, modData);
            }

            // Show occasional reminders
            if (boost.remainingTicks > 0) {
                if (boost.remainingTicks % 1200 == 0) { // Every minute
                    int minutesLeft = boost.remainingTicks / 1200;
                    String typeName = boost.type.substring(0, 1).toUpperCase() + boost.type.substring(1);

                    if (player instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) player).sendMessage(
                                new StringTextComponent(TextFormatting.GREEN +
                                        typeName + " Type Attractor: " + minutesLeft + " minute(s) remaining"),
                                player.getUUID()
                        );
                    }

                    // Try to force a spawn of this type
                    manager.tryForceSpawn(player, boost);
                }
            } else {
                // Boost expired
                manager.activeBoosts.remove(playerId);

                if (player instanceof ServerPlayerEntity) {
                    String typeName = boost.type.substring(0, 1).toUpperCase() + boost.type.substring(1);

                    ((ServerPlayerEntity) player).sendMessage(
                            new StringTextComponent(TextFormatting.RED +
                                    typeName + " Type Attractor has expired!"),
                            player.getUUID()
                    );
                }

                // Also clear from player NBT
                playerData = player.getPersistentData();
                if (playerData.contains(CustomConsumables.MOD_ID)) {
                    CompoundNBT modData = playerData.getCompound(CustomConsumables.MOD_ID);
                    modData.remove(BOOST_TYPE_KEY);
                    modData.remove(BOOST_DURATION_KEY);
                    modData.remove(BOOST_MULTIPLIER_KEY);
                    modData.remove(BOOST_POKEMON_KEY);
                    playerData.put(CustomConsumables.MOD_ID, modData);
                }

                CustomConsumables.getLogger().info(
                        "{} type boost expired for player {}",
                        boost.type, player.getName().getString()
                );
            }
        }
    }

    /**
     * Event handler for entity spawns
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntitySpawn(EntityJoinWorldEvent event) {
        // Skip if not on server or if not initialized
        if (event.getWorld().isClientSide() || !initialized) {
            return;
        }

        // Skip if not a Pixelmon entity
        if (pixelmonEntityClass == null || !pixelmonEntityClass.isInstance(event.getEntity())) {
            return;
        }

        try {
            // Find the nearest player with an active type boost
            World world = event.getWorld();
            BlockPos spawnPos = event.getEntity().blockPosition();

            PlayerEntity nearestPlayerWithBoost = null;
            double closestDistance = 100 * 100; // 100 blocks squared
            TypeBoostInfo activeBoost = null;

            for (PlayerEntity player : world.players()) {
                if (getInstance().hasActiveBoost(player)) {
                    double distance = player.distanceToSqr(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

                    if (distance < closestDistance) {
                        closestDistance = distance;
                        nearestPlayerWithBoost = player;
                        activeBoost = getInstance().getBoostInfo(player);
                    }
                }
            }

            // If no player with boost is nearby, allow spawn
            if (nearestPlayerWithBoost == null || activeBoost == null) {
                return;
            }

            // Get the Pokémon entity and its species
            Object pixelmon = event.getEntity();
            Object pokemon = getPokemonMethod.invoke(pixelmon);
            Object species = getSpeciesMethod.invoke(pokemon);
            String pokemonName = ((String) getNameMethod.invoke(species)).toLowerCase();

            // Check if this Pokémon is in the eligible list
            boolean isEligible = activeBoost.eligiblePokemon.contains(pokemonName.toLowerCase());

            // Also double-check via the data handler
            if (!isEligible) {
                isEligible = com.blissy.customConsumables.data.PokemonTypeDataHandler.getInstance()
                        .hasPokemonType(pokemonName, activeBoost.type);
            }

            // Allow the spawn if it's an eligible Pokémon
            if (isEligible) {
                // This is a Pokémon of the boosted type or in the eligible list
                // Allow it and give it a higher spawn weight
                return;
            }

            // For non-matching types, decide based on the multiplier
            float roll = random.nextFloat();
            float blockChance = Math.min(0.9f, 1.0f - (1.0f / activeBoost.multiplier));

            if (roll < blockChance) {
                // Block this spawn to make room for the boosted type
                event.setCanceled(true);

                // Try to replace with a spawn of the correct type
                if (random.nextFloat() < 0.3f) { // 30% chance to try a replacement
                    getInstance().tryForceSpawn(nearestPlayerWithBoost, activeBoost);
                }

                CustomConsumables.getLogger().debug(
                        "Blocked spawn of {} for player {} with {} type boost",
                        pokemonName, nearestPlayerWithBoost.getName().getString(), activeBoost.type
                );
            }

        } catch (Exception e) {
            // Just log at debug level and allow the spawn
            CustomConsumables.getLogger().debug("Error in entity spawn handler: {}", e.getMessage());
        }
    }

    /**
     * Force a test spawn check (used by legacy code)
     */
    public boolean forceTestSpawnCheck(PlayerEntity player) {
        TypeBoostInfo boost = getBoostInfo(player);
        if (boost != null) {
            tryForceSpawn(player, boost);
            return true;
        }
        return false;
    }

    /**
     * Try to force spawn a Pokémon of the correct type
     */
    private void tryForceSpawn(PlayerEntity player, TypeBoostInfo boost) {
        // Check cooldown
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        if (spawnCooldowns.containsKey(playerId)) {
            long lastSpawnTime = spawnCooldowns.get(playerId);
            if (currentTime - lastSpawnTime < SPAWN_COOLDOWN_MS) {
                return; // Still in cooldown
            }
        }

        // Set cooldown
        spawnCooldowns.put(playerId, currentTime);

        // Get the server
        MinecraftServer server = player instanceof ServerPlayerEntity
                ? ((ServerPlayerEntity) player).getServer()
                : ServerLifecycleHooks.getCurrentServer();

        if (server == null) return;

        try {
            if (boost.eligiblePokemon != null && !boost.eligiblePokemon.isEmpty()) {
                // Choose a random eligible Pokémon
                List<String> pokemonList = new ArrayList<>(boost.eligiblePokemon);
                String pokemonName = pokemonList.get(random.nextInt(pokemonList.size()));

                // Try to spawn it
                server.getCommands().performCommand(
                        server.createCommandSourceStack().withPermission(4),
                        "pokespawn " + pokemonName
                );

                CustomConsumables.getLogger().debug(
                        "Forced spawn of {} for player {} with {} type boost",
                        pokemonName, player.getName().getString(), boost.type
                );
            } else {
                // Fall back to spawning by type
                server.getCommands().performCommand(
                        server.createCommandSourceStack().withPermission(4),
                        "pokespawn " + boost.type
                );

                CustomConsumables.getLogger().debug(
                        "Forced spawn of type {} for player {}",
                        boost.type, player.getName().getString()
                );
            }

            // Add some particles as feedback
            if (player.level instanceof ServerWorld) {
                ((ServerWorld) player.level).sendParticles(
                        net.minecraft.particles.ParticleTypes.WITCH,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        10, 0.5, 0.5, 0.5, 0.05
                );
            }

        } catch (Exception e) {
            CustomConsumables.getLogger().debug("Error forcing spawn: {}", e.getMessage());
        }
    }

    /**
     * Handle player login to restore active boosts
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();

        // Try to load boost from NBT
        TypeBoostInfo boost = getInstance().loadBoostFromPlayer(player);

        if (boost != null && boost.remainingTicks > 0) {
            // Restore the boost
            getInstance().activeBoosts.put(player.getUUID(), boost);

            // Notify the player
            if (player instanceof ServerPlayerEntity) {
                String typeName = boost.type.substring(0, 1).toUpperCase() + boost.type.substring(1);
                int seconds = boost.remainingTicks / 20;

                ((ServerPlayerEntity) player).sendMessage(
                        new StringTextComponent(TextFormatting.GREEN +
                                typeName + " Type Attractor is still active! " +
                                TextFormatting.YELLOW + "(" + seconds + " seconds remaining)"),
                        player.getUUID()
                );
            }

            CustomConsumables.getLogger().info(
                    "Restored {} type boost for player {} with {} ticks remaining",
                    boost.type, player.getName().getString(), boost.remainingTicks
            );
        }
    }

    /**
     * Class to hold information about an active type boost
     */
    public static class TypeBoostInfo {
        public final String type;
        public int remainingTicks;
        public final float multiplier;
        public final Set<String> eligiblePokemon;

        public TypeBoostInfo(String type, int durationTicks, float multiplier, Set<String> eligiblePokemon) {
            this.type = type.toLowerCase();
            this.remainingTicks = durationTicks;
            this.multiplier = multiplier;
            this.eligiblePokemon = eligiblePokemon != null ? eligiblePokemon : new HashSet<>();
        }
    }
}