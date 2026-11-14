package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockPhonePacket(String pin) implements CustomPacketPayload {
    public static final Type<UnlockPhonePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "unlock_phone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockPhonePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, UnlockPhonePacket::pin, UnlockPhonePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
