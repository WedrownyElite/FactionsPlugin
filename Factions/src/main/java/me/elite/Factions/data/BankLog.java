package me.elite.Factions.data;

import java.util.UUID;

public class BankLog {
    public final UUID playerUUID;
    public final String playerName;
    public final BankLogType type;
    public final double amount;
    public final double oldBalance;
    public final double newBalance;
    public long timestamp; // Made non-final for loading from save data

    public BankLog(UUID playerUUID, String playerName, BankLogType type, double amount, double oldBalance, double newBalance) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.type = type;
        this.amount = amount;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor for loading from save data with custom timestamp
    public BankLog(UUID playerUUID, String playerName, BankLogType type, double amount, double oldBalance, double newBalance, long timestamp) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.type = type;
        this.amount = amount;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.timestamp = timestamp;
    }

    public enum BankLogType {
        DEPOSIT("Deposit", "+"),
        WITHDRAW("Withdraw", "-");

        private final String displayName;
        private final String symbol;

        BankLogType(String displayName, String symbol) {
            this.displayName = displayName;
            this.symbol = symbol;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}