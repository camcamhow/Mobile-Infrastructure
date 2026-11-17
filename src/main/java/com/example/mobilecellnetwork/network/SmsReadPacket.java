package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SmsReadPacket(String number) implements CustomPacketPayload {
    public static final Type<SmsReadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "sms_read"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SmsReadPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SmsReadPacket::number, SmsReadPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
