package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AntennaConfigSyncPacket(BlockPos position, List<NetworkOption> ownedNetworks,
                                      List<NetworkOption> allNetworks, String networkId,
                                      String peerNetworkId, long feePerMinute, boolean editable,
                                      boolean agreementMatches)
        implements CustomPacketPayload {
    public static final Type<AntennaConfigSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "antenna_config_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AntennaConfigSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBlockPos(packet.position());
                writeOptions(buffer, packet.ownedNetworks());
                writeOptions(buffer, packet.allNetworks());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.peerNetworkId());
                buffer.writeLong(packet.feePerMinute());
                buffer.writeBoolean(packet.editable());
                buffer.writeBoolean(packet.agreementMatches());
            }, buffer -> new AntennaConfigSyncPacket(
                    buffer.readBlockPos(),
                    readOptions(buffer),
                    readOptions(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readLong(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));

    public AntennaConfigSyncPacket {
        ownedNetworks = List.copyOf(ownedNetworks);
        allNetworks = List.copyOf(allNetworks);
    }

    private static void writeOptions(RegistryFriendlyByteBuf buffer, List<NetworkOption> options) {
        List<NetworkOption> limited = options.stream().limit(256).toList();
        buffer.writeVarInt(limited.size());
        limited.forEach(option -> {
            ByteBufCodecs.STRING_UTF8.encode(buffer, option.id());
            ByteBufCodecs.STRING_UTF8.encode(buffer, option.name());
        });
    }

    private static List<NetworkOption> readOptions(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 256);
        List<NetworkOption> options = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            options.add(new NetworkOption(ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));
        }
        return options;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record NetworkOption(String id, String name) {
    }
}
