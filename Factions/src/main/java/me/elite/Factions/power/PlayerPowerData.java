package me.elite.Factions.power;

/**
 * Stores power-related data for a player
 */
public class PlayerPowerData {
    private int currentPower;
    private int maxPower;
    private long lastPowerUpdate;

    public PlayerPowerData(int currentPower, int maxPower) {
        this.currentPower = currentPower;
        this.maxPower = maxPower;
        this.lastPowerUpdate = System.currentTimeMillis();
    }

    // Constructor for loading from data
    public PlayerPowerData(int currentPower, int maxPower, long lastPowerUpdate) {
        this.currentPower = currentPower;
        this.maxPower = maxPower;
        this.lastPowerUpdate = lastPowerUpdate;
    }

    public int getCurrentPower() {
        return currentPower;
    }

    public void setCurrentPower(int currentPower) {
        this.currentPower = Math.max(0, Math.min(currentPower, maxPower));
    }

    public int getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(int maxPower) {
        this.maxPower = maxPower;
        // Ensure current power doesn't exceed new max
        this.currentPower = Math.min(this.currentPower, maxPower);
    }

    public long getLastPowerUpdate() {
        return lastPowerUpdate;
    }

    public void setLastPowerUpdate(long lastPowerUpdate) {
        this.lastPowerUpdate = lastPowerUpdate;
    }

    /**
     * Add power to current power (respecting max power limit)
     */
    public void addPower(int amount) {
        setCurrentPower(currentPower + amount);
    }

    /**
     * Remove power from current power (minimum 0)
     */
    public void removePower(int amount) {
        setCurrentPower(currentPower - amount);
    }

    /**
     * Check if player has at least the specified amount of power
     */
    public boolean hasPower(int amount) {
        return currentPower >= amount;
    }

    /**
     * Get power as a percentage of max power
     */
    public double getPowerPercentage() {
        return maxPower > 0 ? (double) currentPower / maxPower : 0.0;
    }

    @Override
    public String toString() {
        return "PlayerPowerData{" +
                "currentPower=" + currentPower +
                ", maxPower=" + maxPower +
                ", lastPowerUpdate=" + lastPowerUpdate +
                '}';
    }
}