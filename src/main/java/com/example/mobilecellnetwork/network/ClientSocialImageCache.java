package com.example.mobilecellnetwork.network;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientSocialImageCache {
    private static final Map<String, byte[]> IMAGES = new ConcurrentHashMap<>();
    private static final Set<String> REQUESTED_IMAGES = ConcurrentHashMap.newKeySet();
    private static volatile byte[] pendingImage = new byte[0];

    private ClientSocialImageCache() {
    }

    public static void update(SocialImagePacket packet) {
        IMAGES.put(packet.postId(), packet.image());
        REQUESTED_IMAGES.remove(packet.postId());
        if (IMAGES.size() > 8) {
            IMAGES.keySet().stream().filter(id -> !id.equals(packet.postId()))
                    .limit(IMAGES.size() - 8).forEach(IMAGES::remove);
        }
    }

    public static byte[] image(String postId) {
        byte[] image = IMAGES.get(postId);
        return image == null ? new byte[0] : image.clone();
    }

    public static byte[] imageForDisplay(String postId) {
        return IMAGES.getOrDefault(postId, new byte[0]);
    }

    public static boolean markImageRequested(String postId) {
        return !IMAGES.containsKey(postId) && REQUESTED_IMAGES.add(postId);
    }

    public static void setPendingImage(byte[] image) {
        pendingImage = image == null ? new byte[0] : image.clone();
    }

    public static byte[] pendingImage() {
        return pendingImage.clone();
    }

    public static boolean hasPendingImage() {
        return pendingImage.length > 0;
    }

    public static void clearPendingImage() {
        pendingImage = new byte[0];
    }

    public static void clearSensitiveData() {
        IMAGES.clear();
        REQUESTED_IMAGES.clear();
        clearPendingImage();
    }
}
