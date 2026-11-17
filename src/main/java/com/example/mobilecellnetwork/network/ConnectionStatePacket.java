package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ConnectionStatePacket(boolean connecting, int estimatedTicks) implements CustomPacketPayload {
    public static final Type<ConnectionStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "connection_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectionStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBoolean(packet.connecting());
                buffer.writeVarInt(packet.estimatedTicks());
            }, buffer -> new ConnectionStatePacket(buffer.readBoolean(), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
