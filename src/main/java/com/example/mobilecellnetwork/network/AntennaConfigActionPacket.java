package com.example.mobilecellnetwork.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AntennaConfigActionPacket(String action, BlockPos position, String networkId,
                                        String peerNetworkId, long feePerMinute)
        implements CustomPacketPayload {
    public static final Type<AntennaConfigActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "antenna_config_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AntennaConfigActionPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.action());
                buffer.writeBlockPos(packet.position());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.peerNetworkId());
                buffer.writeLong(packet.feePerMinute());
            }, buffer -> new AntennaConfigActionPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readBlockPos(),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
