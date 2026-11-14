package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SmsSendPacket(String chatId, String body) implements CustomPacketPayload {
    public static final Type<SmsSendPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "sms_send"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SmsSendPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SmsSendPacket::chatId,
                    ByteBufCodecs.STRING_UTF8, SmsSendPacket::body, SmsSendPacket::new);

    public String number() { return chatId; }
    public String message() { return body; }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
