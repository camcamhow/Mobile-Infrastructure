package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.MapPointOfInterest;
import java.util.List;

public final class MapPointCache {
    private static volatile List<MapPointOfInterest> points = List.of();

    private MapPointCache() {
    }

    public static List<MapPointOfInterest> points() {
        return points;
    }

    public static void update(MapPointSyncPacket packet) {
        points = List.copyOf(packet.points());
    }
}
