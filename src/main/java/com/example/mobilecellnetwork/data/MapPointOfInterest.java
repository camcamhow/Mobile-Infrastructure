package com.example.mobilecellnetwork.data;

import java.util.UUID;

public record MapPointOfInterest(UUID id, String name, String dimension, int x, int y, int z,
                                 UUID ownerUUID, String ownerName, Visibility visibility) {
    public enum Visibility {
        PERSONAL,
        PRIVATE,
        PUBLIC
    }
}
