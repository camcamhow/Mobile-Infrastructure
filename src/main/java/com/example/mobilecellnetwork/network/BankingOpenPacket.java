package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BankingOpenPacket() implements CustomPacketPayload {
    public static final Type<BankingOpenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "banking_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankingOpenPacket> STREAM_CODEC =
            StreamCodec.unit(new BankingOpenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
