package com.example.mobilecellnetwork.network;

import java.util.List;

public final class ClientSocialCache {
    private static volatile List<SocialFeedPacket.PostEntry> posts = List.of();

    private ClientSocialCache() {
    }

    public static void update(SocialFeedPacket packet) {
        posts = List.copyOf(packet.posts());
    }

    public static List<SocialFeedPacket.PostEntry> posts() {
        return posts;
    }

    public static void clearSensitiveData() {
        posts = List.of();
    }
}
