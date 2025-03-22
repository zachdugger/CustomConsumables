package com.blissy.customConsumables.effects;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Base class for temporary player effects
 */
public abstract class PlayerEffect {
    private int duration;

    /**
     * Creates a new player effect with the specified duration in ticks
     */
    public PlayerEffect(int duration) {
        this.duration = duration;
    }

    /**
     * Get the current duration in ticks
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Decrements the duration by 1 tick
     */
    public void decrementDuration() {
        if (duration > 0) {
            duration--;
        }
    }

    /**
     * Gets the display name for this effect
     */
    public abstract String getDisplayName();

    /**
     * Called every tick while this effect is active
     */
    public abstract void onTick(PlayerEntity player);

    /**
     * Called when this effect expires
     */
    public abstract void onExpire(PlayerEntity player);
}