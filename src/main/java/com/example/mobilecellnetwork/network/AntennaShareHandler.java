package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class AntennaShareHandler {
    private AntennaShareHandler() {
    }

    public static boolean addShare(ServerPlayer player, BlockPos antennaPos, String networkId) {
        AntennaBlockEntity antenna = ownedAntenna(player, antennaPos);
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        if (antenna == null || networkId == null || data.getNetwork(networkId) == null
                || networkId.equals(antenna.getNetworkId())) {
            return false;
        }
        antenna.addShare(networkId);
        return true;
    }

    public static boolean removeShare(ServerPlayer player, BlockPos antennaPos, String networkId) {
        AntennaBlockEntity antenna = ownedAntenna(player, antennaPos);
        if (antenna == null) {
            return false;
        }
        antenna.removeShare(networkId);
        return true;
    }

    /** Returns the antenna only if the player owns (or is op for) the antenna's network. */
    private static AntennaBlockEntity ownedAntenna(ServerPlayer player, BlockPos pos) {
        if (!(player.serverLevel().getBlockEntity(pos) instanceof AntennaBlockEntity antenna)) {
            return null;
        }
        var network = NetworkSavedData.get(player.server.overworld()).getNetwork(antenna.getNetworkId());
        if (network == null || !(player.hasPermissions(2) || player.getUUID().equals(network.ownerUUID()))) {
            return null;
        }
        return antenna;
    }
}
