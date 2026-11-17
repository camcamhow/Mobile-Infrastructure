package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BankTransferPacket(String recipientNumber, long amount) implements CustomPacketPayload {
    public static final Type<BankTransferPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "bank_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankTransferPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BankTransferPacket::recipientNumber,
                    ByteBufCodecs.VAR_LONG, BankTransferPacket::amount, BankTransferPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
