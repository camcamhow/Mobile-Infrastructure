package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.RoamingAgreement;
import com.example.mobilecellnetwork.network.packet.RoamingProposalPacket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientRoamingCache {
    private static final Map<String, RoamingProposalPacket> PROPOSALS = new ConcurrentHashMap<>();
    private static final Map<String, List<RoamingAgreement>> AGREEMENTS = new ConcurrentHashMap<>();

    private ClientRoamingCache() {
    }

    public static void updateProposal(RoamingProposalPacket packet) {
        PROPOSALS.put(packet.targetNetworkId(), packet);
    }

    public static void updateAgreements(String networkId, List<RoamingAgreement> agreements) {
        AGREEMENTS.put(networkId, List.copyOf(agreements));
    }

    public static List<RoamingProposalPacket> proposals() {
        return List.copyOf(PROPOSALS.values());
    }

    public static List<RoamingAgreement> agreements(String networkId) {
        return AGREEMENTS.getOrDefault(networkId, List.of());
    }

    public static void clear() {
        PROPOSALS.clear();
        AGREEMENTS.clear();
    }

    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clear();
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
