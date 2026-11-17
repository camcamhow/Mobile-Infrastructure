package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneDiedPacket() implements CustomPacketPayload {
    public static final Type<PhoneDiedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_died"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneDiedPacket> STREAM_CODEC =
            StreamCodec.unit(new PhoneDiedPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
