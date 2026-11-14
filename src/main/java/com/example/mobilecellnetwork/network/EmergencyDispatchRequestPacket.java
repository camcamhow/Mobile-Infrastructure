package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EmergencyDispatchRequestPacket() implements CustomPacketPayload {
    public static final Type<EmergencyDispatchRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "emergency_dispatch_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmergencyDispatchRequestPacket> STREAM_CODEC =
            StreamCodec.unit(new EmergencyDispatchRequestPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
