package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import net.minecraft.server.level.ServerPlayer;

public final class ServerBlockPasswordHandler {
    private ServerBlockPasswordHandler() {
    }

    public static boolean changePassword(ServerPlayer player, String networkId, String newPassword) {
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        var network = networkId == null ? null : data.getNetwork(networkId);
        int minimumLength = MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        if (network == null || newPassword == null || newPassword.length() < minimumLength
                || newPassword.length() > 128) {
            return false;
        }
        if (!player.hasPermissions(2) && !player.getUUID().equals(network.ownerUUID())) {
            return false;
        }
        var settings = data.getNetworkSettings(networkId);
        String salt = PasswordUtil.generateSalt();
        settings.setPasswordSalt(salt);
        settings.setPasswordHash(PasswordUtil.hash(newPassword, salt));
        data.setDirty();
        return true;
    }

    public static boolean clearPassword(ServerPlayer player, String networkId) {
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        var network = networkId == null ? null : data.getNetwork(networkId);
        if (network == null || !player.hasPermissions(2)) {
            return false;
        }
        data.getNetworkSettings(networkId).clearPassword();
        data.setDirty();
        return true;
    }
}
