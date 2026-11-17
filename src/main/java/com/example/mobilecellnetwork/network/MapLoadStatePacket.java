package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapLoadStatePacket(boolean ready, String message) implements CustomPacketPayload {
    public static final Type<MapLoadStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "map_load_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapLoadStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBoolean(packet.ready());
                buffer.writeUtf(packet.message(), 128);
            }, buffer -> new MapLoadStatePacket(buffer.readBoolean(), buffer.readUtf(128)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
