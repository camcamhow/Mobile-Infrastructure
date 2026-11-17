package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class AntennaClaimHandler {
    public enum ClaimResult { SUCCESS, WRONG_PASSWORD, NETWORK_NOT_FOUND, ANTENNA_ALREADY_CLAIMED, ANTENNA_NOT_FOUND }

    public enum UnclaimResult { SUCCESS, NOT_OWNER, ANTENNA_NOT_FOUND }

    public static final String UNCLAIMED = "default";

    private AntennaClaimHandler() {
    }

    public static ClaimResult claim(ServerPlayer player, BlockPos antennaPos, String networkId, String password) {
        if (!(player.serverLevel().getBlockEntity(antennaPos) instanceof AntennaBlockEntity antenna)) {
            return ClaimResult.ANTENNA_NOT_FOUND;
        }
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        if (networkId == null || data.getNetwork(networkId) == null) {
            return ClaimResult.NETWORK_NOT_FOUND;
        }
        var settings = data.getNetworkSettings(networkId);
        if (!PasswordUtil.verify(password, settings.getPasswordHash(), settings.getPasswordSalt())) {
            return ClaimResult.WRONG_PASSWORD;
        }
        String current = antenna.getNetworkId();
        if (!current.equals(networkId) && data.getNetwork(current) != null) {
            return ClaimResult.ANTENNA_ALREADY_CLAIMED;
        }
        antenna.setNetworkId(networkId);
        if (antenna.getOwnerUUID() == null) {
            antenna.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
        data.registerAntenna(key(player, antennaPos), networkId);
        return ClaimResult.SUCCESS;
    }

    public static UnclaimResult unclaim(ServerPlayer player, BlockPos antennaPos) {
        if (!(player.serverLevel().getBlockEntity(antennaPos) instanceof AntennaBlockEntity antenna)) {
            return UnclaimResult.ANTENNA_NOT_FOUND;
        }
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        var network = data.getNetwork(antenna.getNetworkId());
        if (network == null
                || !(player.hasPermissions(2) || player.getUUID().equals(network.ownerUUID()))) {
            return UnclaimResult.NOT_OWNER;
        }
        antenna.setNetworkId(UNCLAIMED);
        antenna.clearShares();
        data.registerAntenna(key(player, antennaPos), UNCLAIMED);
        return UnclaimResult.SUCCESS;
    }

    private static String key(ServerPlayer player, BlockPos pos) {
        return player.level().dimension().location() + "@" + pos.asLong();
    }
}
