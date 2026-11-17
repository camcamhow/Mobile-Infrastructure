package com.example.mobilecellnetwork.network;

public final class ClientSocialProfileCache {
    private static volatile String username = "";
    private static volatile int color = 0xFF4F7CAC;
    private static volatile byte[] avatar = new byte[0];
    private static volatile boolean loaded;

    private ClientSocialProfileCache() {
    }

    public static void beginLoad() {
        loaded = false;
    }

    public static void update(SocialProfileSyncPacket packet) {
        username = packet.username();
        color = packet.color() | 0xFF000000;
        avatar = packet.avatar();
        loaded = true;
    }

    public static String username() {
        return username;
    }

    public static int color() {
        return color;
    }

    public static byte[] avatar() {
        return avatar.clone();
    }

    public static boolean configured() {
        return !username.isBlank();
    }

    public static boolean loaded() {
        return loaded;
    }

    public static void clearSensitiveData() {
        username = "";
        color = 0xFF4F7CAC;
        avatar = new byte[0];
        loaded = false;
    }
}
