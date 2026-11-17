package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestPhoneDataPacket() implements CustomPacketPayload {
    public static final Type<RequestPhoneDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "request_phone_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPhoneDataPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestPhoneDataPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
