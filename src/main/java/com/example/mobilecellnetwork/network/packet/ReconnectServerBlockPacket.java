package com.example.mobilecellnetwork.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReconnectServerBlockPacket(String networkId, String password, BlockPos blockPos)
        implements CustomPacketPayload {
    public static final Type<ReconnectServerBlockPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "reconnect_server_block"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReconnectServerBlockPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.password());
                buffer.writeBlockPos(packet.blockPos());
            }, buffer -> new ReconnectServerBlockPacket(ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readBlockPos()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
