package com.example.mobilecellnetwork.network;

public final class ClientAntennaConfigCache {
    private static volatile AntennaConfigSyncPacket snapshot;
    private static volatile long revision;

    private ClientAntennaConfigCache() {
    }

    public static void update(AntennaConfigSyncPacket packet) {
        snapshot = packet;
        revision++;
    }

    public static AntennaConfigSyncPacket snapshot() {
        return snapshot;
    }

    public static long revision() {
        return revision;
    }
}
