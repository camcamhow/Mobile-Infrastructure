package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.MapPointOfInterest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class MapPointShare {
    private static final String PREFIX = "[MCN-POI:";

    private MapPointShare() {
    }

    public static String encode(MapPointOfInterest point) {
        String name = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(point.name().getBytes(StandardCharsets.UTF_8));
        String dimension = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(point.dimension().getBytes(StandardCharsets.UTF_8));
        return PREFIX + point.id() + ":" + dimension + ":" + point.x() + ":" + point.y() + ":"
                + point.z() + ":" + name + "]";
    }

    public static MapPointOfInterest decode(String body, String ownerName) {
        try {
            if (body == null || !body.startsWith(PREFIX) || !body.endsWith("]")) {
                return null;
            }
            String[] fields = body.substring(PREFIX.length(), body.length() - 1).split(":", -1);
            if (fields.length != 6) {
                return null;
            }
            String name = new String(Base64.getUrlDecoder().decode(fields[5]), StandardCharsets.UTF_8);
            String dimension = new String(Base64.getUrlDecoder().decode(fields[1]), StandardCharsets.UTF_8);
            if (name.isBlank() || name.length() > 64 || !dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                return null;
            }
            return new MapPointOfInterest(UUID.fromString(fields[0]), name, dimension,
                    Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Integer.parseInt(fields[4]),
                    null, ownerName, MapPointOfInterest.Visibility.PRIVATE);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static boolean isShare(String body) {
        return body != null && body.startsWith(PREFIX);
    }

    public static String displayText(String body, String sender) {
        MapPointOfInterest point = decode(body, sender);
        if (point == null) {
            return body;
        }
        return "Map point: " + point.name() + " at "
                + point.x() + ", " + point.y() + ", " + point.z() + " in " + point.dimension();
    }
}
