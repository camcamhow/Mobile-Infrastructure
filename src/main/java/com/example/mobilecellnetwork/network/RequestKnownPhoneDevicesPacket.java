package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestKnownPhoneDevicesPacket() implements CustomPacketPayload {
    public static final Type<RequestKnownPhoneDevicesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "request_known_phone_devices"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestKnownPhoneDevicesPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestKnownPhoneDevicesPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
