package com.example.mobilecellnetwork.network;

import java.util.List;

public final class ClientBankAccountCache {
    private static volatile BankAccountSyncPacket snapshot = new BankAccountSyncPacket(0L, List.of());

    private ClientBankAccountCache() {
    }

    public static void update(BankAccountSyncPacket packet) {
        snapshot = packet;
    }

    public static BankAccountSyncPacket snapshot() {
        return snapshot;
    }

    public static void clearSensitiveData() {
        snapshot = new BankAccountSyncPacket(0L, List.of());
    }
}
