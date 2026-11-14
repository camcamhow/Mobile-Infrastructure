package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.gui.PhoneStyle;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPhoneCache {
    private static volatile String phoneNumber = "";
    private static volatile String planId = "";
    private static volatile boolean lowPowerMode;
    private static volatile boolean phoneDead;
    private static volatile boolean ringtoneEnabled = true;
    private static volatile boolean notificationSoundsEnabled = true;
    private static volatile boolean doNotDisturb;
    private static volatile String ringtoneSound = "minecraft:block.note_block.bell";
    private static volatile String theme = "samsung";
    private static volatile PhoneStyle phoneStyle = PhoneStyle.SAMSUNG_ONE_UI;
    private static volatile int lowPowerRevision;
    private static volatile List<OnlinePlayer> onlinePlayers = List.of();
    private static volatile List<String> notifications = List.of();
    private static volatile java.util.Set<String> installedApps =
            java.util.Set.of("phone", "sms", "contacts", "settings");
    private static volatile List<PhoneDataSyncPacket.FollowedPlayer> followedPlayers = List.of();
    private static volatile Map<String, Boolean> appNotifications = Map.of();
    private static final Map<String, String> CONTACTS = new ConcurrentHashMap<>();

    private ClientPhoneCache() {
    }

    public static void update(PhoneDataSyncPacket packet) {
        phoneNumber = packet.phoneNumber();
        planId = packet.planId();
        lowPowerMode = packet.lowPowerMode();
        phoneDead = packet.phoneDead();
        ringtoneEnabled = packet.ringtoneEnabled();
        notificationSoundsEnabled = packet.notificationSoundsEnabled();
        doNotDisturb = packet.doNotDisturb();
        ringtoneSound = packet.ringtoneSound();
        theme = packet.theme();
        lowPowerRevision++;
        CONTACTS.clear();
        packet.contacts().forEach(contact -> CONTACTS.put(contact.name(), contact.number()));
        notifications = List.copyOf(packet.notifications());
        onlinePlayers = packet.onlinePlayers().stream()
                .map(player -> new OnlinePlayer(player.id(), player.name(), player.number()))
                .toList();
        installedApps = java.util.Set.copyOf(packet.installedApps());
        followedPlayers = List.copyOf(packet.followedPlayers());
        java.util.Map<String, Boolean> notificationsByApp = new java.util.HashMap<>();
        packet.appNotifications().forEach(entry -> notificationsByApp.put(entry.appId(), entry.enabled()));
        appNotifications = Map.copyOf(notificationsByApp);
    }

    public static void clearSensitiveData() {
        phoneNumber = "";
        planId = "";
        onlinePlayers = List.of();
        notifications = List.of();
        CONTACTS.clear();
        followedPlayers = List.of();
        appNotifications = Map.of();
        lowPowerMode = false;
        phoneDead = false;
        installedApps = java.util.Set.of("phone", "sms", "contacts", "settings");
        lowPowerRevision++;
    }

    public static List<OnlinePlayer> onlinePlayers() {
        return onlinePlayers;
    }

    public static Map<String, String> contacts() {
        return Map.copyOf(CONTACTS);
    }

    public static boolean lowPowerMode() {
        return lowPowerMode;
    }

    public static void setLowPowerMode(boolean enabled) {
        lowPowerMode = enabled;
        lowPowerRevision++;
    }

    public static void updateLowPowerState(boolean enabled) {
        lowPowerMode = enabled;
        lowPowerRevision++;
    }

    public static int lowPowerRevision() {
        return lowPowerRevision;
    }

    public static boolean phoneDead() {
        return phoneDead;
    }

    public static void setPhoneDead(boolean dead) {
        phoneDead = dead;
    }

    public static String phoneNumber() {
        return phoneNumber;
    }

    public static String planId() {
        return planId;
    }

    public static boolean ringtoneEnabled() {
        return ringtoneEnabled;
    }

    public static boolean notificationSoundsEnabled() { return notificationSoundsEnabled; }
    public static boolean doNotDisturb() { return doNotDisturb; }
    public static String ringtoneSound() { return ringtoneSound; }
    public static String theme() { return theme; }
    public static PhoneStyle phoneStyle() { return phoneStyle; }
    public static void setPhoneStyle(PhoneStyle value) {
        phoneStyle = value == null ? PhoneStyle.DEFAULT : value;
        theme = phoneStyle == PhoneStyle.IOS ? "iphone" : "samsung";
    }
    public static void setTheme(String value) {
        theme = "iphone".equalsIgnoreCase(value) || "light".equalsIgnoreCase(value)
                ? "iphone" : "samsung";
    }
    private static boolean flashlightOn;
    public static boolean flashlightOn() { return flashlightOn; }
    public static void setFlashlightOn(boolean on) { flashlightOn = on; }
    public static java.util.Set<String> installedApps() { return installedApps; }
    public static List<PhoneDataSyncPacket.FollowedPlayer> followedPlayers() { return followedPlayers; }
    public static Map<String, Boolean> appNotifications() { return appNotifications; }
    public static boolean canUseApp(String appId) {
        if ("settings".equals(appId) || "plan".equals(appId) || "store".equals(appId)
                    || "flashlight".equals(appId) || "camera".equals(appId) || "gallery".equals(appId)) return true;
        if (!installedApps.contains(appId)) return false;
        return !lowPowerMode || com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .LOW_POWER_ALLOWED_APPS.get().stream().anyMatch(appId::equalsIgnoreCase);
    }

    public static void setRingtoneEnabled(boolean enabled) {
        ringtoneEnabled = enabled;
    }

    public record OnlinePlayer(java.util.UUID id, String name, String number) {
    }
}
