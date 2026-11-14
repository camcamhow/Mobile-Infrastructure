package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenHeldPhonePacket() implements CustomPacketPayload {
    public static final Type<OpenHeldPhonePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "open_held_phone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenHeldPhonePacket> STREAM_CODEC =
            StreamCodec.unit(new OpenHeldPhonePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
