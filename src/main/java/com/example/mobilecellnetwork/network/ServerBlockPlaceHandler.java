package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.NetworkSavedData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ServerBlockPlaceHandler {
    private ServerBlockPlaceHandler() {
    }

    /** Restores a matching server, assigns an unambiguous network, or creates one if needed. */
    public static String assignOrCreate(ServerPlayer player, BlockPos blockPos) {
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        List<String> unassigned = data.getNetworks().entrySet().stream()
                .filter(entry -> player.getUUID().equals(entry.getValue().ownerUUID()))
                .map(Map.Entry::getKey)
                .filter(id -> !data.getNetworkSettings(id).hasServerBlock())
                .toList();
        List<String> matching = unassigned.stream()
                .filter(id -> data.getNetworkSettings(id).wasServerAt(
                        player.level().dimension().location().toString(), blockPos.asLong()))
                .toList();
        if (!matching.isEmpty()) {
            for (String id : matching) {
                assign(player, data, id, blockPos);
                if (player.serverLevel().getBlockEntity(blockPos)
                        instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity serverBlock) {
                    serverBlock.addManagedNetwork(id);
                }
            }
            return matching.getFirst();
        }
        if (unassigned.size() == 1) {
            String id = unassigned.getFirst();
            assign(player, data, id, blockPos);
            return id;
        }
        if (!unassigned.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "Choose a network to restore in this server's Networks menu."));
            return null;
        }
        return createNewNetwork(player, blockPos, "", "");
    }

    public static String createNewNetwork(ServerPlayer player, BlockPos blockPos, String name, String password) {
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        String id = UUID.randomUUID().toString();
        String networkName = name == null || name.isBlank()
                ? player.getGameProfile().getName() + "'s Network" : name.trim();
        data.putNetwork(id, new NetworkSavedData.NetworkRecord(networkName, true, List.of(), player.getUUID(),
                player.getGameProfile().getName(),
                com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                        .DEFAULT_NETWORK_ROAMING_POLICY.get()));
        var settings = data.getNetworkSettings(id);
        if (password != null && !password.isEmpty()) {
            settings.setPasswordHash(PasswordUtil.hash(password, settings.getPasswordSalt()));
        }
        assign(player, data, id, blockPos);
        return id;
    }

    private static void assign(ServerPlayer player, NetworkSavedData data, String networkId, BlockPos pos) {
        var settings = data.getNetworkSettings(networkId);
        settings.setServerBlock(player.level().dimension().location().toString(), pos.asLong());
        settings.setServerOnline(true);
        data.setDirty();
        player.displayClientMessage(Component.literal("Server block assigned to network "
                + data.getNetwork(networkId).name() + "."), true);
    }

    public static boolean restore(ServerPlayer player, BlockPos pos, String networkId) {
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        var network = data.getNetwork(networkId);
        var settings = data.getNetworkSettings(networkId);
        if (network == null || !player.getUUID().equals(network.ownerUUID())
                || settings.hasServerBlock()
                || !(player.serverLevel().getBlockEntity(pos)
                instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity serverBlock)
                || !serverBlock.getManagedNetworks().isEmpty()) {
            return false;
        }
        assign(player, data, networkId, pos);
        serverBlock.addManagedNetwork(networkId);
        return true;
    }

    /** Called when a server block is removed: the network goes offline but nothing is deleted. */
    public static void onServerBlockBroken(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        var server = level.getServer();
        NetworkSavedData data = NetworkSavedData.get(server.overworld());
        String dimension = level.dimension().location().toString();
        for (Map.Entry<String, NetworkSavedData.NetworkRecord> entry : data.getNetworks().entrySet()) {
            var settings = data.getNetworkSettings(entry.getKey());
            if (settings.hasServerBlock() && settings.getServerBlockPos() == pos.asLong()
                    && dimension.equals(settings.getServerBlockDimension())) {
                settings.clearServerBlock();
                data.setDirty();
                UUID owner = entry.getValue().ownerUUID();
                ServerPlayer ownerPlayer = owner == null ? null : server.getPlayerList().getPlayer(owner);
                if (ownerPlayer != null) {
                    ownerPlayer.sendSystemMessage(Component.literal("Your network " + entry.getValue().name()
                            + " is now offline: its server block was removed."));
                }
            }
        }
    }
}
