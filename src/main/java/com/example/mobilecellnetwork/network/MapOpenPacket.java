package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapOpenPacket() implements CustomPacketPayload {
    public static final Type<MapOpenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "map_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapOpenPacket> STREAM_CODEC =
            StreamCodec.unit(new MapOpenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
