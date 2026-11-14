package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.data.NetworkSettings;
import com.example.mobilecellnetwork.data.ProposalState;
import com.example.mobilecellnetwork.data.RoamingAgreement;
import com.example.mobilecellnetwork.data.RoamingProposal;
import com.example.mobilecellnetwork.network.packet.ProposalResponse;
import com.example.mobilecellnetwork.network.packet.RoamingAgreementSyncPacket;
import com.example.mobilecellnetwork.network.packet.RoamingProposalPacket;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RoamingManager {
    private static final int AUTO_REQUEST_INTERVAL_TICKS = 1_200;
    private static final Map<UUID, Long> LAST_ROAMING_NOTIFICATION = new ConcurrentHashMap<>();
    private static final Set<UUID> CURRENTLY_ROAMING = ConcurrentHashMap.newKeySet();

    private RoamingManager() {
    }

    public static boolean propose(ServerPlayer actor, String fromNetworkId, String toNetworkId,
                                  long perMinute, long perText, long perMb) {
        NetworkSavedData saved = NetworkSavedData.get(actor.server.overworld());
        if (!canManage(actor, saved, fromNetworkId)
                || !validProposal(saved, fromNetworkId, toNetworkId, perMinute, perText, perMb)) {
            return false;
        }
        return createProposal(saved, fromNetworkId, toNetworkId, perMinute, perText, perMb, actor.server);
    }

    public static boolean forceAgreement(ServerPlayer actor, String networkA, String networkB,
                                         long perMinute, long perText, long perMb) {
        if (!actor.hasPermissions(2) || perMinute < 0L || perText < 0L || perMb < 0L) {
            return false;
        }
        NetworkSavedData saved = NetworkSavedData.get(actor.server.overworld());
        return forceAgreement(actor.server, saved, networkA, networkB, perMinute, perText, perMb);
    }

    public static boolean forceAgreement(MinecraftServer server, NetworkSavedData saved,
                                         String networkA, String networkB,
                                         long perMinute, long perText, long perMb) {
        if (perMinute < 0L || perText < 0L || perMb < 0L
                || !validNetworks(saved, networkA, networkB)) {
            return false;
        }
        long now = System.currentTimeMillis();
        setAgreement(saved, networkA, networkB, perMinute, perText, perMb, now);
        syncAgreementOwners(server, saved, networkA);
        syncAgreementOwners(server, saved, networkB);
        notifyOwners(server, saved, networkA, networkB,
                "Roaming agreement activated between " + networkName(saved, networkA)
                        + " and " + networkName(saved, networkB) + ".");
        return true;
    }

    public static boolean revokeAgreement(ServerPlayer actor, String networkId, String otherNetworkId) {
        NetworkSavedData saved = NetworkSavedData.get(actor.server.overworld());
        if (!canManage(actor, saved, networkId) || !validNetworks(saved, networkId, otherNetworkId)) {
            return false;
        }
        return revokeAgreement(actor.server, saved, networkId, otherNetworkId);
    }

    public static boolean revokeAgreement(MinecraftServer server, NetworkSavedData saved,
                                          String networkId, String otherNetworkId) {
        if (!validNetworks(saved, networkId, otherNetworkId)) {
            return false;
        }
        NetworkSettings first = saved.getNetworkSettings(networkId);
        NetworkSettings second = saved.getNetworkSettings(otherNetworkId);
        boolean removed = first.getAgreements().remove(otherNetworkId) != null;
        removed |= second.getAgreements().remove(networkId) != null;
        if (!removed) {
            return false;
        }
        first.getOutgoingProposals().remove(otherNetworkId);
        first.getIncomingProposals().remove(otherNetworkId);
        second.getOutgoingProposals().remove(networkId);
        second.getIncomingProposals().remove(networkId);
        saved.removeRoamingAgreement(networkId, otherNetworkId);
        saved.removeRoamingAgreement(otherNetworkId, networkId);
        saved.setDirty();
        syncAgreementOwners(server, saved, networkId);
        syncAgreementOwners(server, saved, otherNetworkId);
        notifyOwners(server, saved, networkId, otherNetworkId,
                "Roaming agreement between " + networkName(saved, networkId)
                        + " and " + networkName(saved, otherNetworkId) + " was revoked.");
        return true;
    }

    public static boolean forcePropose(MinecraftServer server, NetworkSavedData saved,
                                       String fromId, String toId,
                                       long perMinute, long perText, long perMb) {
        return validProposal(saved, fromId, toId, perMinute, perText, perMb)
                && createProposal(saved, fromId, toId, perMinute, perText, perMb, server);
    }

    public static boolean respondToProposal(ServerPlayer responder, String proposalId,
                                            ProposalResponse response,
                                            long counterPerMinute, long counterPerText, long counterPerMb) {
        if (response == null || (response == ProposalResponse.COUNTER
                && (counterPerMinute < 0L || counterPerText < 0L || counterPerMb < 0L))) {
            return false;
        }
        NetworkSavedData saved = NetworkSavedData.get(responder.server.overworld());
        ProposalLocation location = findProposal(saved, proposalId);
        if (location == null || !canManage(responder, saved, location.proposal().toNetworkId())) {
            return false;
        }
        RoamingProposal proposal = location.proposal();
        NetworkSettings fromSettings = saved.getNetworkSettings(proposal.fromNetworkId());
        NetworkSettings toSettings = saved.getNetworkSettings(proposal.toNetworkId());
        if (fromSettings == null || toSettings == null || proposal.state() != ProposalState.PENDING) {
            return false;
        }

        fromSettings.getOutgoingProposals().remove(proposal.toNetworkId());
        toSettings.getIncomingProposals().remove(proposal.fromNetworkId());
        long now = System.currentTimeMillis();
        switch (response) {
            case ACCEPT -> {
                setAgreement(saved, proposal.fromNetworkId(), proposal.toNetworkId(),
                        proposal.ratePerMinute(), proposal.ratePerText(), proposal.ratePerMb(), now);
                dispatchProposal(responder.server, saved,
                        proposal.withState(ProposalState.ACCEPTED, "Agreement accepted"));
                notifyOwners(responder, saved, proposal.fromNetworkId(), proposal.toNetworkId(),
                        "Roaming agreement accepted between " + networkName(saved, proposal.fromNetworkId())
                                + " and " + networkName(saved, proposal.toNetworkId()) + ".");
                syncAgreementOwners(responder.server, saved, proposal.fromNetworkId());
                syncAgreementOwners(responder.server, saved, proposal.toNetworkId());
            }
            case REJECT -> {
                dispatchProposal(responder.server, saved,
                        proposal.withState(ProposalState.DECLINED, "Proposal rejected"));
                notifyOwners(responder, saved, proposal.fromNetworkId(), proposal.toNetworkId(),
                        "Roaming proposal between " + networkName(saved, proposal.fromNetworkId())
                                + " and " + networkName(saved, proposal.toNetworkId()) + " was rejected.");
            }
            case COUNTER -> {
                RoamingProposal counter = new RoamingProposal(UUID.randomUUID().toString(),
                        proposal.toNetworkId(), proposal.fromNetworkId(),
                        counterPerMinute, counterPerText, counterPerMb, ProposalState.PENDING,
                        "Counter proposal", now);
                toSettings.getOutgoingProposals().put(proposal.fromNetworkId(), counter);
                fromSettings.getIncomingProposals().put(proposal.toNetworkId(), counter);
                dispatchProposal(responder.server, saved, counter);
                notifyOwners(responder, saved, proposal.fromNetworkId(), proposal.toNetworkId(),
                        "A roaming counter-proposal was sent between "
                                + networkName(saved, proposal.fromNetworkId()) + " and "
                                + networkName(saved, proposal.toNetworkId()) + ".");
            }
        }
        saved.setDirty();
        return true;
    }

    public static List<AgreementEntry> activeAgreements(NetworkSavedData saved) {
        List<AgreementEntry> agreements = new ArrayList<>();
        saved.getNetworks().keySet().stream().sorted().forEach(networkId -> {
            NetworkSettings settings = saved.getNetworkSettings(networkId);
            if (settings == null) {
                return;
            }
            settings.getAgreements().forEach((otherId, agreement) -> {
                if (networkId.compareTo(otherId) < 0) {
                    agreements.add(new AgreementEntry(networkId, otherId, agreement));
                }
            });
        });
        return List.copyOf(agreements);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % AUTO_REQUEST_INTERVAL_TICKS != 0) {
            return;
        }
        NetworkSavedData saved = NetworkSavedData.get(event.getServer().overworld());
        for (Map.Entry<String, NetworkSavedData.NetworkRecord> entry : saved.getNetworks().entrySet()) {
            String fromId = entry.getKey();
            NetworkSettings settings = saved.getNetworkSettings(fromId);
            if (settings == null || !settings.isAutoSendRoamingRequests()) {
                continue;
            }
            for (String toId : saved.getNetworks().keySet()) {
                if (validProposal(saved, fromId, toId, settings.getDefaultRatePerMinute(),
                        settings.getDefaultRatePerText(), settings.getDefaultRatePerMb())) {
                    createProposal(saved, fromId, toId, settings.getDefaultRatePerMinute(),
                            settings.getDefaultRatePerText(), settings.getDefaultRatePerMb(), event.getServer());
                }
            }
        }
    }

    public static void notifyRoaming(ServerPlayer player, boolean roaming, String hostNetworkName) {
        if (!roaming) {
            CURRENTLY_ROAMING.remove(player.getUUID());
            return;
        }
        boolean enteredRoaming = CURRENTLY_ROAMING.add(player.getUUID());
        long now = System.currentTimeMillis();
        long cooldown = MobileCellNetworkConfig.ROAMING_NOTIFICATION_COOLDOWN_SECONDS.get() * 1_000L;
        Long previous = LAST_ROAMING_NOTIFICATION.get(player.getUUID());
        if (!enteredRoaming && (cooldown == 0L
                || previous != null && now - previous < cooldown)) {
            return;
        }
        if (enteredRoaming && previous != null && now - previous < cooldown) {
            return;
        }
        LAST_ROAMING_NOTIFICATION.put(player.getUUID(), now);
        String text = "Roaming on " + hostNetworkName + ". Charges may apply.";
        PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket(
                "PLAN_WARNING", "Roaming", text, false));
        player.displayClientMessage(Component.literal(text), true);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        LAST_ROAMING_NOTIFICATION.remove(player.getUUID());
        CURRENTLY_ROAMING.remove(player.getUUID());
    }

    private static boolean createProposal(NetworkSavedData saved, String fromId, String toId,
                                          long perMinute, long perText, long perMb,
                                          MinecraftServer server) {
        if (!validProposal(saved, fromId, toId, perMinute, perText, perMb)) {
            return false;
        }
        NetworkSettings from = saved.getNetworkSettings(fromId);
        NetworkSettings to = saved.getNetworkSettings(toId);
        if (from.getAgreements().containsKey(toId) || to.getAgreements().containsKey(fromId)
                || from.getOutgoingProposals().containsKey(toId)
                || to.getIncomingProposals().containsKey(fromId)
                || from.getIncomingProposals().containsKey(toId)
                || to.getOutgoingProposals().containsKey(fromId)) {
            return false;
        }
        RoamingProposal proposal = new RoamingProposal(UUID.randomUUID().toString(), fromId, toId,
                perMinute, perText, perMb, ProposalState.PENDING, "Proposal", System.currentTimeMillis());
        from.getOutgoingProposals().put(toId, proposal);
        to.getIncomingProposals().put(fromId, proposal);
        saved.setDirty();
        dispatchProposal(server, saved, proposal);
        notifyOwners(server, saved, fromId, toId,
                "Roaming proposal sent from " + networkName(saved, fromId)
                        + " to " + networkName(saved, toId) + ".");
        return true;
    }

    private static void dispatchProposal(MinecraftServer server, NetworkSavedData saved, RoamingProposal proposal) {
        sendProposalToOwner(server, saved.getNetworkOwnerUUID(proposal.fromNetworkId()), proposal.toNetworkId(), proposal);
        sendProposalToOwner(server, saved.getNetworkOwnerUUID(proposal.toNetworkId()), proposal.fromNetworkId(), proposal);
    }

    private static void sendProposalToOwner(MinecraftServer server, UUID ownerId, String targetNetworkId,
                                            RoamingProposal proposal) {
        ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            PacketDistributor.sendToPlayer(owner, new RoamingProposalPacket(
                    targetNetworkId,
                    String.valueOf(proposal.ratePerMinute()),
                    String.valueOf(proposal.ratePerText()),
                    String.valueOf(proposal.ratePerMb()),
                    proposal.lastMessage()));
        }
    }

    private static void setAgreement(NetworkSavedData saved, String networkA, String networkB,
                                     long perMinute, long perText, long perMb, long timestamp) {
        NetworkSettings first = saved.getNetworkSettings(networkA);
        NetworkSettings second = saved.getNetworkSettings(networkB);
        first.getOutgoingProposals().remove(networkB);
        first.getIncomingProposals().remove(networkB);
        second.getOutgoingProposals().remove(networkA);
        second.getIncomingProposals().remove(networkA);
        first.getAgreements().put(networkB,
                new RoamingAgreement(networkB, perMinute, perText, perMb, timestamp));
        second.getAgreements().put(networkA,
                new RoamingAgreement(networkA, perMinute, perText, perMb, timestamp));
        saved.setDirty();
    }

    private static void syncAgreementOwners(MinecraftServer server, NetworkSavedData saved, String networkId) {
        UUID ownerId = saved.getNetworkOwnerUUID(networkId);
        ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
        NetworkSettings settings = saved.getNetworkSettings(networkId);
        if (owner != null && settings != null) {
            PacketDistributor.sendToPlayer(owner, new RoamingAgreementSyncPacket(
                    networkId, List.copyOf(settings.getAgreements().values())));
        }
    }

    private static void notifyOwners(ServerPlayer source, NetworkSavedData saved,
                                     String networkA, String networkB, String message) {
        notifyOwners(source.server, saved, networkA, networkB, message);
    }

    private static void notifyOwners(MinecraftServer server, NetworkSavedData saved,
                                     String networkA, String networkB, String message) {
        Set<UUID> owners = new LinkedHashSet<>();
        UUID first = saved.getNetworkOwnerUUID(networkA);
        UUID second = saved.getNetworkOwnerUUID(networkB);
        if (first != null) owners.add(first);
        if (second != null) owners.add(second);
        ServerPlayer source = server.getPlayerList().getPlayers().stream().findFirst().orElse(null);
        if (source != null) {
            NetworkHandler.deliverPrivilegedAlert(source, saved, owners, "NETWORK", message);
        } else {
            long now = System.currentTimeMillis();
            for (UUID ownerId : owners) {
                String number = saved.getNumberFor(ownerId);
                if (number != null) {
                    saved.addPendingSms(ownerId, new com.example.mobilecellnetwork.capabilities.PhoneData.Message(
                            "NETWORK", number, message, now, false));
                }
            }
        }
        for (UUID ownerId : owners) {
            ServerPlayer online = server.getPlayerList().getPlayer(ownerId);
            if (online != null) {
                PacketDistributor.sendToPlayer(online, new PhoneNotificationPacket(
                        "PLAN_WARNING", "Roaming update", message, false));
            }
        }
    }

    private static ProposalLocation findProposal(NetworkSavedData saved, String proposalId) {
        for (String networkId : saved.getNetworks().keySet()) {
            NetworkSettings settings = saved.getNetworkSettings(networkId);
            if (settings == null) {
                continue;
            }
            for (RoamingProposal proposal : settings.getIncomingProposals().values()) {
                if (proposal.proposalId().equals(proposalId)) {
                    return new ProposalLocation(proposal);
                }
            }
        }
        return null;
    }

    private static boolean canManage(ServerPlayer player, NetworkSavedData saved, String networkId) {
        return player.hasPermissions(2) || player.getUUID().equals(saved.getNetworkOwnerUUID(networkId));
    }

    private static boolean validProposal(NetworkSavedData saved, String fromId, String toId,
                                         long perMinute, long perText, long perMb) {
        return perMinute >= 0L && perText >= 0L && perMb >= 0L && validNetworks(saved, fromId, toId);
    }

    private static boolean validNetworks(NetworkSavedData saved, String fromId, String toId) {
        return fromId != null && toId != null && !fromId.equals(toId)
                && saved.getNetwork(fromId) != null && saved.getNetwork(toId) != null;
    }

    private static String networkName(NetworkSavedData saved, String networkId) {
        NetworkSavedData.NetworkRecord network = saved.getNetwork(networkId);
        return network == null ? networkId : network.name();
    }

    private record ProposalLocation(RoamingProposal proposal) { }

    public record AgreementEntry(String networkA, String networkB, RoamingAgreement agreement) { }
}
