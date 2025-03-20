package com.blissy.customConsumables.effects;

import com.blissy.customConsumables.compat.PixelmonCompat;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEffectManager {

    private static final Map<UUID, PlayerEffects> playerEffects = new HashMap<>();

    public static void applyLegendaryBoost(PlayerEntity player, int duration) {
        getOrCreateEffects(player).legendaryBoostTicks = duration;
        saveEffects(player);

        // Also apply through Pixelmon compat
        PixelmonCompat.applyLegendaryBoost(player, duration);
    }

    public static void applyShinyBoost(PlayerEntity player, int duration) {
        getOrCreateEffects(player).shinyBoostTicks = duration;
        saveEffects(player);

        // Also apply through Pixelmon compat
        PixelmonCompat.applyShinyBoost(player, duration);
    }

    public static void applyHiddenAbilityBoost(PlayerEntity player, int duration) {
        getOrCreateEffects(player).hiddenAbilityBoostTicks = duration;
        saveEffects(player);

        // Also apply through Pixelmon compat
        PixelmonCompat.applyHiddenAbilityBoost(player, duration);
    }

    public static void applyTypeBoost(PlayerEntity player, String type, int duration) {
        PlayerEffects effects = getOrCreateEffects(player);
        effects.typeBoostTicks = duration;
        effects.boostedType = type;
        saveEffects(player);

        // Also apply through Pixelmon compat
        PixelmonCompat.applyTypeBoost(player, type, duration);
    }

    public static boolean hasLegendaryBoost(PlayerEntity player) {
        return getOrCreateEffects(player).legendaryBoostTicks > 0;
    }

    public static boolean hasShinyBoost(PlayerEntity player) {
        return getOrCreateEffects(player).shinyBoostTicks > 0;
    }

    public static boolean hasHiddenAbilityBoost(PlayerEntity player) {
        return getOrCreateEffects(player).hiddenAbilityBoostTicks > 0;
    }

    public static boolean hasTypeBoost(PlayerEntity player, String type) {
        PlayerEffects effects = getOrCreateEffects(player);
        return effects.typeBoostTicks > 0 && effects.boostedType.equals(type);
    }

    public static void tickEffects(PlayerEntity player) {
        PlayerEffects effects = getOrCreateEffects(player);
        boolean changed = false;

        if (effects.legendaryBoostTicks > 0) {
            effects.legendaryBoostTicks--;
            changed = true;
        }

        if (effects.shinyBoostTicks > 0) {
            effects.shinyBoostTicks--;
            changed = true;
        }

        if (effects.hiddenAbilityBoostTicks > 0) {
            effects.hiddenAbilityBoostTicks--;
            changed = true;
        }

        if (effects.typeBoostTicks > 0) {
            effects.typeBoostTicks--;
            changed = true;
        }

        if (changed) {
            saveEffects(player);
        }
    }

    private static PlayerEffects getOrCreateEffects(PlayerEntity player) {
        return playerEffects.computeIfAbsent(player.getUUID(), k -> {
            PlayerEffects effects = new PlayerEffects();
            loadEffects(player, effects);
            return effects;
        });
    }

    private static void saveEffects(PlayerEntity player) {
        CompoundNBT data = player.getPersistentData();
        CompoundNBT customConsumables = data.getCompound("CustomConsumables");

        PlayerEffects effects = getOrCreateEffects(player);
        customConsumables.putInt("legendaryBoostTicks", effects.legendaryBoostTicks);
        customConsumables.putInt("shinyBoostTicks", effects.shinyBoostTicks);
        customConsumables.putInt("hiddenAbilityBoostTicks", effects.hiddenAbilityBoostTicks);
        customConsumables.putInt("typeBoostTicks", effects.typeBoostTicks);
        customConsumables.putString("boostedType", effects.boostedType);

        data.put("CustomConsumables", customConsumables);
    }

    private static void loadEffects(PlayerEntity player, PlayerEffects effects) {
        CompoundNBT data = player.getPersistentData();
        if (data.contains("CustomConsumables")) {
            CompoundNBT customConsumables = data.getCompound("CustomConsumables");
            effects.legendaryBoostTicks = customConsumables.getInt("legendaryBoostTicks");
            effects.shinyBoostTicks = customConsumables.getInt("shinyBoostTicks");
            effects.hiddenAbilityBoostTicks = customConsumables.getInt("hiddenAbilityBoostTicks");
            effects.typeBoostTicks = customConsumables.getInt("typeBoostTicks");
            effects.boostedType = customConsumables.getString("boostedType");
        }
    }

    private static class PlayerEffects {
        int legendaryBoostTicks = 0;
        int shinyBoostTicks = 0;
        int hiddenAbilityBoostTicks = 0;
        int typeBoostTicks = 0;
        String boostedType = "";
    }
}