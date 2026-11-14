package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.NetworkSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public final class ServerBlockReconnectHandler {
    public enum ReconnectResult { SUCCESS, WRONG_PASSWORD, NETWORK_ALREADY_HAS_SERVER, NETWORK_NOT_FOUND }

    private ServerBlockReconnectHandler() {
    }

    public static ReconnectResult reconnect(ServerPlayer player, BlockPos blockPos, String networkId,
                                            String password) {
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        if (networkId == null || data.getNetwork(networkId) == null) {
            return ReconnectResult.NETWORK_NOT_FOUND;
        }
        var settings = data.getNetworkSettings(networkId);
        if (!PasswordUtil.verify(password, settings.getPasswordHash(), settings.getPasswordSalt())) {
            return ReconnectResult.WRONG_PASSWORD;
        }
        if (settings.hasServerBlock()) {
            return ReconnectResult.NETWORK_ALREADY_HAS_SERVER;
        }
        settings.setServerBlock(player.level().dimension().location().toString(), blockPos.asLong());
        settings.setServerOnline(true);
        data.setDirty();
        return ReconnectResult.SUCCESS;
    }
}
