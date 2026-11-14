package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.gui.AntennaMenu;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AntennaConfigHandler {
    private AntennaConfigHandler() {
    }

    public static void handle(net.minecraft.world.entity.player.Player sender,
                              AntennaConfigActionPacket packet) {
        if (!(sender instanceof ServerPlayer player)
                || !(player.containerMenu instanceof AntennaMenu menu)
                || !menu.stillValid(player) || !menu.getBlockPos().equals(packet.position())
                || packet.action().length() > 12 || packet.networkId().length() > 128
                || packet.peerNetworkId().length() > 128) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(packet.position());
        if (!(blockEntity instanceof AntennaBlockEntity antenna)) {
            return;
        }
        NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        if ("request".equals(packet.action())) {
            sync(player, antenna, data, packet.networkId(), packet.peerNetworkId());
            return;
        }
        if (!"save".equals(packet.action()) && !"withdraw".equals(packet.action())) {
            return;
        }
        if (!canEditAntenna(player, antenna)) {
            notify(player, "Only the antenna owner can configure this antenna.");
            return;
        }
        NetworkSavedData.NetworkRecord selectedNetwork = data.getNetwork(packet.networkId());
        if (selectedNetwork == null || !ownsNetwork(player, selectedNetwork)) {
            notify(player, "Select a network you own.");
            sync(player, antenna, data, antenna.getNetworkId(), "");
            return;
        }
        if ("save".equals(packet.action())) {
            if (packet.feePerMinute() < 0L) {
                notify(player, "The roaming fee must be zero or greater.");
                return;
            }
            if (!packet.peerNetworkId().isBlank()) {
                NetworkSavedData.NetworkRecord peer = data.getNetwork(packet.peerNetworkId());
                if (peer == null || packet.peerNetworkId().equals(packet.networkId())) {
                    notify(player, "Select a different, existing carrier for the agreement.");
                    return;
                }
                if (!ownsNetwork(player, selectedNetwork)) {
                    notify(player, "Only the selected network's owner can approve its side of an agreement.");
                    return;
                }
                data.setRoamingAgreement(packet.networkId(), packet.peerNetworkId(), packet.feePerMinute());
            }
            antenna.setNetworkIdAndSync(packet.networkId());
            data.registerAntenna(antenna.getLevel().dimension().location() + "@"
                    + antenna.getBlockPos().asLong(), antenna.getNetworkId());
            notify(player, "Antenna network settings saved.");
        } else {
            if (packet.peerNetworkId().isBlank() || !ownsNetwork(player, selectedNetwork)) {
                notify(player, "Only the selected network's owner can withdraw an agreement.");
                return;
            }
            data.removeRoamingAgreement(packet.networkId(), packet.peerNetworkId());
            notify(player, "Roaming approval withdrawn. The agreement is inactive until both carriers match.");
        }
        sync(player, antenna, data, packet.networkId(), packet.peerNetworkId());
    }

    private static void sync(ServerPlayer player, AntennaBlockEntity antenna, NetworkSavedData data,
                             String requestedNetworkId, String requestedPeerNetworkId) {
        boolean canEdit = canEditAntenna(player, antenna);
        boolean operator = player.hasPermissions(2);
        List<AntennaConfigSyncPacket.NetworkOption> allNetworks = data.getNetworks().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().name(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(entry -> new AntennaConfigSyncPacket.NetworkOption(entry.getKey(), entry.getValue().name()))
                .toList();
        List<AntennaConfigSyncPacket.NetworkOption> ownedNetworks = allNetworks.stream()
                .filter(option -> {
                    NetworkSavedData.NetworkRecord network = data.getNetwork(option.id());
                    return network != null && (operator || ownsNetwork(player, network));
                }).toList();
        if (!canEdit) {
            allNetworks = List.of();
        }
        String selectedNetwork = antenna.getNetworkId();
        String peerNetwork = requestedPeerNetworkId;
        long fee = 5L;
        boolean matches = false;
        if (!requestedNetworkId.isBlank() && ownedNetworks.stream()
                .anyMatch(option -> option.id().equals(requestedNetworkId))) {
            selectedNetwork = requestedNetworkId;
        }
        String networkCandidate = selectedNetwork;
        if (!ownedNetworks.isEmpty() && ownedNetworks.stream()
                .noneMatch(option -> option.id().equals(networkCandidate))) {
            selectedNetwork = ownedNetworks.getFirst().id();
        }
        String peerCandidate = peerNetwork;
        Map.Entry<String, Long> currentAgreement = data.getRoamingAgreements(selectedNetwork).entrySet().stream()
                .filter(entry -> peerCandidate.isBlank() || entry.getKey().equals(peerCandidate))
                .findFirst().orElse(null);
        if (currentAgreement != null) {
            peerNetwork = currentAgreement.getKey();
            fee = currentAgreement.getValue();
            Long otherApproval = data.getRoamingAgreementFee(peerNetwork, selectedNetwork);
            matches = Long.valueOf(fee).equals(otherApproval);
        }
        PacketDistributor.sendToPlayer(player, new AntennaConfigSyncPacket(
                antenna.getBlockPos(), ownedNetworks, allNetworks, selectedNetwork,
                peerNetwork, fee, canEdit, matches));
    }

    private static boolean canEditAntenna(ServerPlayer player, AntennaBlockEntity antenna) {
        return player.hasPermissions(2) || player.getUUID().equals(antenna.getOwnerUUID());
    }

    private static boolean ownsNetwork(ServerPlayer player, NetworkSavedData.NetworkRecord network) {
        return player.hasPermissions(2) || player.getUUID().equals(network.ownerUUID());
    }

    private static void notify(ServerPlayer player, String text) {
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
    }
}
