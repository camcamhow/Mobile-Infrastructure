package com.example.mobilecellnetwork.network;

import java.util.List;
import java.util.UUID;

public final class PhoneDeviceClientCache {
    private static volatile PhoneDeviceStatePacket state;
    private static volatile List<KnownPhoneDevicesPacket.Device> devices = List.of();
    private static volatile boolean unlocked;
    private static volatile boolean pinAccepted;
    private static volatile int pinResultRevision;

    private PhoneDeviceClientCache() {
    }

    public static void update(PhoneDeviceStatePacket packet) {
        state = packet;
        unlocked = !packet.lockScreenEnabled() && !packet.lockedAll() && !packet.pinSet();
        pinAccepted = false;
    }

    public static void updateDevices(KnownPhoneDevicesPacket packet) {
        devices = List.copyOf(packet.devices());
    }

    public static PhoneDeviceStatePacket state() { return state; }
    public static List<KnownPhoneDevicesPacket.Device> devices() { return devices; }
    public static UUID phoneId() { return state == null ? null : state.phoneId(); }
    public static boolean unlocked() { return unlocked; }
    public static void setUnlocked(boolean value) { unlocked = value; }
    public static boolean pinAccepted() { return pinAccepted; }
    public static void setPinAccepted(boolean value) { pinAccepted = value; }
    public static int pinResultRevision() { return pinResultRevision; }
    public static void recordPinResult(PhonePinResultPacket packet) {
        pinAccepted = packet.accepted();
        pinResultRevision++;
    }
    public static boolean lockScreenEnabled() { return state != null && state.lockScreenEnabled(); }
    public static boolean pinSet() { return state != null && state.pinSet(); }
    public static boolean lockedAll() { return state != null && state.lockedAll(); }
    public static boolean lockedBank() { return state != null && state.lockedBank(); }
    public static boolean bankReadOnly() { return state != null && state.readOnlyBank(); }
    public static boolean hasUnreadNotifications() { return state != null && state.hasUnreadNotifications(); }
}
