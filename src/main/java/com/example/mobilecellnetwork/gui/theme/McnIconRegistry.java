package com.example.mobilecellnetwork.gui.theme;

import com.example.mobilecellnetwork.gui.PhoneStyle;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class McnIconRegistry {
    private record IconEntry(String appId, String glyph, int brandColor) {
    }

    private static final Map<String, IconEntry> REGISTRY;

    static {
        Map<String, IconEntry> icons = new HashMap<>();
        icons.put("phone", new IconEntry("phone", "\u260E", 0xFF34C759));
        icons.put("facetime", new IconEntry("facetime", "\u25B6", 0xFF34C759));
        icons.put("sms", new IconEntry("sms", "\u2709", 0xFF34C759));
        icons.put("contacts", new IconEntry("contacts", "\u263A", 0xFF0A84FF));
        icons.put("map", new IconEntry("map", "\u25A6", 0xFF34C759));
        icons.put("store", new IconEntry("store", "\u2605", 0xFFFF9F0A));
        icons.put("socials", new IconEntry("socials", "\u2665", 0xFFAF52DE));
        icons.put("bank", new IconEntry("bank", "$", 0xFF0A84FF));
        icons.put("settings", new IconEntry("settings", "\u2699", 0xFF8E8E93));
        icons.put("plan", new IconEntry("plan", "\u25A4", 0xFF0A84FF));
        icons.put("voicemail", new IconEntry("voicemail", "\u25B6", 0xFFFF9F0A));
        icons.put("ringtones", new IconEntry("ringtones", "\u266B", 0xFFFF9F0A));
        icons.put("camera", new IconEntry("camera", "\u25C9", 0xFF8E8E93));
        icons.put("gallery", new IconEntry("gallery", "\u25A3", 0xFF8E8E93));
        icons.put("flashlight", new IconEntry("flashlight", "\u2600", 0xFFFFD60A));
        icons.put("lock", new IconEntry("lock", "\u2302", 0xFF8E8E93));
        REGISTRY = Collections.unmodifiableMap(icons);
    }

    private McnIconRegistry() {
    }

    public static String getGlyph(String appId) {
        IconEntry entry = REGISTRY.get(appId);
        return entry == null ? "?" : entry.glyph();
    }

    public static int getBrandColor(String appId) {
        IconEntry entry = REGISTRY.get(appId);
        return entry == null ? 0xFF6E6E6E : entry.brandColor();
    }

    public static boolean hasPngTexture(String appId) {
        if (appId == null || appId.isBlank()) {
            return false;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft != null && minecraft.getResourceManager() != null
                    && minecraft.getResourceManager().getResource(getPngLocation(appId)).isPresent();
        } catch (LinkageError | IllegalStateException exception) {
            return false;
        }
    }

    private static final Map<String, String> PNG_MAP = Map.ofEntries(
            Map.entry("phone", "phone"),
            Map.entry("sms", "sms"),
            Map.entry("contacts", "contacts"),
            Map.entry("map", "map"),
            Map.entry("store", "store"),
            Map.entry("socials", "instagram"),
            Map.entry("bank", "bank"),
            Map.entry("settings", "settings"),
            Map.entry("plan", "plan"),
            Map.entry("voicemail", "voicemail"),
            Map.entry("camera", "camera"),
            Map.entry("gallery", "image"),
            Map.entry("flashlight", "flashlight"),
            Map.entry("lock", "lock")
        );

        private static final Set<String> SNAPPLE_ICONS = Set.of(
                "phone", "sms", "contacts", "map", "store", "socials", "bank",
                "settings", "plan", "voicemail", "camera", "gallery", "flashlight", "facetime");

        public static ResourceLocation getPngLocation(String appId) {
            if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS && SNAPPLE_ICONS.contains(appId)) {
                return ResourceLocation.fromNamespaceAndPath("mobilecellnetwork",
                        "textures/gui/snapple_apps/" + appId + ".png");
            }
            if (ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI && SNAPPLE_ICONS.contains(appId)) {
                return ResourceLocation.fromNamespaceAndPath("mobilecellnetwork",
                        "textures/gui/g24_apps/" + appId + ".png");
            }
            String fileName = PNG_MAP.getOrDefault(appId, appId);
            return ResourceLocation.fromNamespaceAndPath("mobilecellnetwork",
                    "textures/gui/apps/" + fileName + ".png");
        }

    public static Set<String> getKnownAppIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
