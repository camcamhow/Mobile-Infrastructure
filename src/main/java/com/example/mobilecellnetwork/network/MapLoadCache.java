package com.example.mobilecellnetwork.network;

public final class MapLoadCache {
    private static volatile boolean ready;
    private static volatile String message = "";

    private MapLoadCache() {
    }

    public static void begin() {
        ready = false;
        message = "";
    }

    public static void update(MapLoadStatePacket packet) {
        ready = packet.ready();
        message = packet.message();
    }

    public static boolean ready() {
        return ready;
    }

    public static String message() {
        return message;
    }
}
