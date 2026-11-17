package com.example.mobilecellnetwork.network;

public final class ClientTowerCache {
    private static volatile TowerListPacket latest = new TowerListPacket(java.util.List.of(), Long.MIN_VALUE, Long.MIN_VALUE);

    private ClientTowerCache() {
    }

    public static TowerListPacket latest() {
        return latest;
    }

    public static void update(TowerListPacket packet) {
        latest = packet;
    }
}
