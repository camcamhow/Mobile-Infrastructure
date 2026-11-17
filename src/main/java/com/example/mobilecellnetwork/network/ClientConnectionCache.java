package com.example.mobilecellnetwork.network;

public final class ClientConnectionCache {
    private static volatile boolean connecting;
    private static volatile long completesAtMillis;

    private ClientConnectionCache() {
    }

    public static void update(ConnectionStatePacket packet) {
        connecting = packet.connecting();
        completesAtMillis = connecting
                ? System.currentTimeMillis() + Math.max(0, packet.estimatedTicks()) * 50L : 0L;
    }

    public static boolean connecting() {
        if (connecting && System.currentTimeMillis() >= completesAtMillis) {
            connecting = false;
        }
        return connecting;
    }
}
