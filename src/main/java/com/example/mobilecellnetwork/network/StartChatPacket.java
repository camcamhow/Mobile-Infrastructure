package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StartChatPacket(String recipientNumber) implements CustomPacketPayload {
    public static final Type<StartChatPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "start_chat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartChatPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, StartChatPacket::recipientNumber, StartChatPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
