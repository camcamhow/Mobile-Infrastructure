package com.example.mobilecellnetwork.network;

import java.util.List;

public final class ClientServerAdminCache {
    private static volatile ServerAdminDataPacket snapshot = new ServerAdminDataPacket("Networks", List.of());

    private ClientServerAdminCache() {
    }

    public static void update(ServerAdminDataPacket packet) {
        snapshot = packet;
    }

    public static ServerAdminDataPacket snapshot() {
        return snapshot;
    }
}
