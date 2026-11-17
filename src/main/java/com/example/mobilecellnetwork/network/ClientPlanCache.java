package com.example.mobilecellnetwork.network;

import java.util.List;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientPlanCache {
    private static volatile PlanSyncPacket snapshot =
            new PlanSyncPacket(List.of(), "", 0L, 0, 0, 0L, 0.0D, List.of());

    private ClientPlanCache() {
    }

    public static void update(PlanSyncPacket packet) {
        snapshot = packet;
    }

    public static PlanSyncPacket snapshot() {
        return snapshot;
    }

    public static String getActiveCarrierId() {
        String activePlanId = snapshot.activePlanId();
        return snapshot.plans().stream()
                .filter(plan -> plan.id().equals(activePlanId))
                .map(PlanSyncPacket.PlanEntry::carrierNetworkId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst().orElse(null);
    }

    public static void clear() {
        snapshot = new PlanSyncPacket(List.of(), "", 0L, 0, 0, 0L, 0.0D, List.of());
    }

    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clearSession();
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearSession();
    }

    public static void onClientClone(ClientPlayerNetworkEvent.Clone event) {
        clearSession();
    }

    public static void clearSensitiveData() {
        clear();
    }

    private static void clearSession() {
        clear();
        ClientSignalCache.clear();
    }
}
