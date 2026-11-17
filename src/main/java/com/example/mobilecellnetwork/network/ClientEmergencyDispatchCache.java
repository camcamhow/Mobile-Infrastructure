package com.example.mobilecellnetwork.network;

import java.util.List;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientEmergencyDispatchCache {
    private static volatile List<EmergencyDispatchDataPacket.Entry> messages = List.of();

    private ClientEmergencyDispatchCache() {
    }

    public static void update(EmergencyDispatchDataPacket packet) {
        messages = List.copyOf(packet.messages());
    }

    public static List<EmergencyDispatchDataPacket.Entry> messages() {
        return messages;
    }

    public static void clear() {
        messages = List.of();
    }

    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clear();
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
