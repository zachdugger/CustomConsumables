package com.blissy.customConsumables.items;

import com.blissy.customConsumables.CustomConsumables;
import com.blissy.customConsumables.data.PokemonTypeDataHandler;
import com.blissy.customConsumables.effects.PlayerEffectManager;
import com.blissy.customConsumables.events.TypeSpawnManager;
import com.blissy.customConsumables.init.ItemInit;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TypeAttractorItem extends Item {
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_DURATION = 3 * 60 * 20; // 3 minutes in ticks
    private static final float TYPE_MULTIPLIER = 10.0f; // 1000% boost
    private final String defaultType;

    // Valid Pokémon types
    private static final List<String> VALID_TYPES = Arrays.asList(
            "normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison",
            "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark",
            "steel", "fairy");

    // Color mappings for different types (for particle effects)
    private static final java.util.Map<String, int[]> TYPE_COLORS = new java.util.HashMap<>();

    static {
        // Initialize type colors for visual effects
        TYPE_COLORS.put("normal", new int[]{168, 168, 120});
        TYPE_COLORS.put("fire", new int[]{240, 128, 48});
        TYPE_COLORS.put("water", new int[]{104, 144, 240});
        TYPE_COLORS.put("grass", new int[]{120, 200, 80});
        TYPE_COLORS.put("electric", new int[]{248, 208, 48});
        TYPE_COLORS.put("ice", new int[]{152, 216, 216});
        TYPE_COLORS.put("fighting", new int[]{192, 48, 40});
        TYPE_COLORS.put("poison", new int[]{160, 64, 160});
        TYPE_COLORS.put("ground", new int[]{224, 192, 104});
        TYPE_COLORS.put("flying", new int[]{168, 144, 240});
        TYPE_COLORS.put("psychic", new int[]{248, 88, 136});
        TYPE_COLORS.put("bug", new int[]{168, 184, 32});
        TYPE_COLORS.put("rock", new int[]{184, 160, 56});
        TYPE_COLORS.put("ghost", new int[]{112, 88, 152});
        TYPE_COLORS.put("dragon", new int[]{112, 56, 248});
        TYPE_COLORS.put("dark", new int[]{112, 88, 72});
        TYPE_COLORS.put("steel", new int[]{184, 184, 208});
        TYPE_COLORS.put("fairy", new int[]{238, 153, 172});
    }

    public TypeAttractorItem(Properties properties, String defaultType) {
        super(properties);
        this.defaultType = validateType(defaultType);

        // Initialize the Pokemon data handler when the item is created
        PokemonTypeDataHandler.getInstance().initialize();
    }

    /**
     * Validates and normalizes a type string
     */
    private String validateType(String type) {
        String normalized = type.toLowerCase();
        return VALID_TYPES.contains(normalized) ? normalized : "fire";
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, LivingEntity entityLiving) {
        if (entityLiving instanceof ServerPlayerEntity && !world.isClientSide) {
            ServerPlayerEntity player = (ServerPlayerEntity) entityLiving;

            // Check if Pixelmon is loaded
            boolean pixelmonLoaded = ModList.get().isLoaded("pixelmon");
            if (!pixelmonLoaded) {
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "Pixelmon mod is not installed! This item requires Pixelmon."),
                        true
                );
                return stack;
            }

            // Get the type from the item
            String type = getTypeFromStack(stack);
            String typeName = formatTypeName(type);

            try {
                // Get eligible Pokémon for this player's biome and the selected type
                Set<String> eligiblePokemon = PokemonTypeDataHandler.getInstance()
                        .getEligiblePokemonForPlayer(player, type);

                // Apply the effect directly to the player
                PlayerEffectManager.applyTypeAttractorEffect(player, type, DEFAULT_DURATION, TYPE_MULTIPLIER);

                // Also register with the spawn manager for advanced features
                // FIX: Pass only the correct number of arguments (type, duration, multiplier)
                TypeSpawnManager.getInstance().registerTypeBoost(player, type, DEFAULT_DURATION, TYPE_MULTIPLIER);

                // Send info to the player
                player.displayClientMessage(
                        new StringTextComponent(getTypeColor(type) + typeName + " Type Attractor " +
                                TextFormatting.GREEN + "activated!"),
                        true
                );

                // Show information about the current biome
                Biome biome = world.getBiome(player.blockPosition());
                String biomeName = biome.getRegistryName() != null
                        ? biome.getRegistryName().getPath()
                        : biome.getBiomeCategory().getName();

                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.YELLOW +
                                "Current biome: " + TextFormatting.AQUA + formatBiomeName(biomeName)),
                        true
                );

                // Show information about eligible Pokémon
                int numEligible = eligiblePokemon.size();
                if (numEligible > 0) {
                    player.displayClientMessage(
                            new StringTextComponent(TextFormatting.YELLOW +
                                    "Found " + TextFormatting.GREEN + numEligible + TextFormatting.YELLOW +
                                    " " + typeName + " type Pokémon for this biome."),
                            true
                    );

                    // Show a small sample of eligible Pokémon if there are many
                    if (numEligible > 3) {
                        // Pick 3 random examples
                        Object[] pokemonArray = eligiblePokemon.toArray();
                        StringBuilder examples = new StringBuilder();
                        for (int i = 0; i < Math.min(3, numEligible); i++) {
                            if (i > 0) examples.append(", ");
                            String pokemon = (String) pokemonArray[RANDOM.nextInt(pokemonArray.length)];
                            examples.append(formatPokemonName(pokemon));
                        }

                        player.displayClientMessage(
                                new StringTextComponent(TextFormatting.GRAY +
                                        "Examples: " + TextFormatting.WHITE + examples.toString() +
                                        TextFormatting.GRAY + " and more..."),
                                true
                        );
                    }
                } else {
                    // No eligible Pokémon found
                    player.displayClientMessage(
                            new StringTextComponent(TextFormatting.RED +
                                    "No " + typeName + " type Pokémon found for this biome. Using global type list."),
                            true
                    );
                }

                // Create a visual effect
                createTypeBoostEffect(player, type);

                // Try to immediately spawn a Pokémon of this type
                trySpawnTypePokemon(player, type);

                // Log the usage
                CustomConsumables.getLogger().info(
                        "Player {} used a {} Type Attractor in biome {}. Found {} eligible Pokémon.",
                        player.getName().getString(), typeName, biomeName, numEligible
                );

            } catch (Exception e) {
                CustomConsumables.getLogger().error("Error activating Type Attractor", e);
                player.displayClientMessage(
                        new StringTextComponent(TextFormatting.RED + "Error: " + e.getMessage()),
                        true
                );
            }

            // Consume the item unless in creative mode
            if (!player.abilities.instabuild) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    /**
     * Creates a visual particle effect based on the Pokémon type
     */
    private void createTypeBoostEffect(ServerPlayerEntity player, String type) {
        if (!(player.level instanceof net.minecraft.world.server.ServerWorld)) return;

        net.minecraft.world.server.ServerWorld world = (net.minecraft.world.server.ServerWorld) player.level;
        int[] typeColor = TYPE_COLORS.getOrDefault(type.toLowerCase(), new int[]{255, 255, 255});

        // Play sound based on type
        switch (type.toLowerCase()) {
            case "fire":
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                break;
            case "water":
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WATER_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                break;
            case "electric":
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.3F, 1.5F);
                break;
            default:
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                break;
        }

        // Create a spiral effect with type-specific "color" (as much as possible with default particles)
        for (int i = 0; i < 150; i++) {
            double angle = i * 0.15;
            double radius = Math.min(7.0, 0.3 * i);
            double height = i * 0.03;

            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;

            // Choose particle based on type
            if (type.equalsIgnoreCase("fire")) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.FLAME,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            } else if (type.equalsIgnoreCase("water")) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.DRIPPING_WATER,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            } else if (type.equalsIgnoreCase("electric")) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.FIREWORK,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            } else if (type.equalsIgnoreCase("ice")) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.ITEM_SNOWBALL,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            } else if (type.equalsIgnoreCase("grass")) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.COMPOSTER,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            } else if (type.equalsIgnoreCase("dragon")) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.DRAGON_BREATH,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            } else {
                // Generic particles for other types
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.WITCH,
                        x, player.getY() + height, z,
                        1, 0, 0, 0, 0.01
                );
            }

            // Add some occasional enchantment particles for magical effect
            if (i % 10 == 0) {
                world.sendParticles(
                        net.minecraft.particles.ParticleTypes.ENCHANT,
                        player.getX() + RANDOM.nextDouble() * 2 - 1,
                        player.getY() + 1 + RANDOM.nextDouble(),
                        player.getZ() + RANDOM.nextDouble() * 2 - 1,
                        1, 0, 0, 0, 0.05
                );
            }
        }
    }

    /**
     * Try to spawn a Pokémon of the specified type immediately
     */
    private void trySpawnTypePokemon(ServerPlayerEntity player, String type) {
        if (player.getServer() == null) return;

        try {
            // Try to spawn using command
            player.getServer().getCommands().performCommand(
                    player.getServer().createCommandSourceStack().withPermission(4),
                    "pokespawn " + type.toLowerCase()
            );

            CustomConsumables.getLogger().info(
                    "Attempted to spawn {} type for player {}",
                    type, player.getName().getString()
            );
        } catch (Exception e) {
            // Just log at debug level, this is a nice-to-have feature
            CustomConsumables.getLogger().debug(
                    "Error trying to spawn initial Pokémon: {}", e.getMessage()
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        String type = getTypeFromStack(stack);
        String typeName = formatTypeName(type);

        TextFormatting typeColor = getTypeColor(type);

        tooltip.add(new StringTextComponent(typeColor + typeName + " Type Attractor"));
        tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Increases " + typeColor + typeName +
                TextFormatting.YELLOW + " type Pokémon spawns by " + TextFormatting.GREEN +
                (int)(TYPE_MULTIPLIER * 100) + "%" + TextFormatting.YELLOW + " for 3 minutes"));
        tooltip.add(new StringTextComponent(TextFormatting.GRAY + "Analyzes local biome for compatible spawns"));
        tooltip.add(new StringTextComponent(TextFormatting.ITALIC + "Consume to activate"));
    }

    @Override
    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        playerIn.startUsingItem(handIn);
        return ActionResult.consume(itemstack);
    }

    /**
     * Sets the Pokémon type for this attractor
     * @param stack The item stack
     * @param type The Pokémon type (e.g., "fire", "water", etc.)
     * @return The modified item stack
     */
    public ItemStack setType(ItemStack stack, String type) {
        CompoundNBT nbt = stack.getOrCreateTag();
        nbt.putString("type", validateType(type));
        return stack;
    }

    /**
     * Gets the Pokémon type from this attractor
     * @param stack The item stack
     * @return The Pokémon type
     */
    public String getTypeFromStack(ItemStack stack) {
        CompoundNBT nbt = stack.getTag();
        if (nbt != null && nbt.contains("type")) {
            return nbt.getString("type");
        }
        return defaultType;
    }

    /**
     * Format a type name for display
     */
    public static String formatTypeName(String type) {
        if (type == null || type.isEmpty()) return "";
        return type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
    }

    /**
     * Format a Pokémon name for display
     */
    public static String formatPokemonName(String pokemon) {
        if (pokemon == null || pokemon.isEmpty()) return "";
        return pokemon.substring(0, 1).toUpperCase() + pokemon.substring(1).toLowerCase();
    }

    /**
     * Format a biome name for display (convert snake_case to Title Case)
     */
    private String formatBiomeName(String biomeName) {
        if (biomeName == null || biomeName.isEmpty()) return "";

        String[] parts = biomeName.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(part.substring(0, 1).toUpperCase());
                result.append(part.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    /**
     * Get text formatting color for a type
     */
    private TextFormatting getTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "fire": return TextFormatting.RED;
            case "water": return TextFormatting.BLUE;
            case "grass": return TextFormatting.GREEN;
            case "electric": return TextFormatting.YELLOW;
            case "ice": return TextFormatting.AQUA;
            case "fighting": return TextFormatting.DARK_RED;
            case "poison": return TextFormatting.DARK_PURPLE;
            case "ground": return TextFormatting.GOLD;
            case "flying": return TextFormatting.LIGHT_PURPLE;
            case "psychic": return TextFormatting.LIGHT_PURPLE;
            case "bug": return TextFormatting.DARK_GREEN;
            case "rock": return TextFormatting.GOLD;
            case "ghost": return TextFormatting.DARK_PURPLE;
            case "dragon": return TextFormatting.DARK_BLUE;
            case "dark": return TextFormatting.DARK_GRAY;
            case "steel": return TextFormatting.GRAY;
            case "fairy": return TextFormatting.LIGHT_PURPLE;
            default: return TextFormatting.WHITE;
        }
    }

    /**
     * Static factory method to create a type attractor for a specific type
     * Used for commands and other programmatic creation
     *
     * @param type The Pokémon type
     * @param count The stack size
     * @return An ItemStack of the appropriate type attractor
     */
    public static ItemStack createForType(String type, int count) {
        ItemStack stack = new ItemStack(ItemInit.TYPE_ATTRACTOR.get(), count);
        TypeAttractorItem item = (TypeAttractorItem) stack.getItem();
        return item.setType(stack, type);
    }

    /**
     * Checks if the given type is valid
     *
     * @param type The type to check
     * @return true if it's a valid Pokémon type
     */
    public static boolean isValidType(String type) {
        return VALID_TYPES.contains(type.toLowerCase());
    }

    /**
     * Gets the list of valid Pokémon types
     */
    public static List<String> getValidTypes() {
        return VALID_TYPES;
    }
}