package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneNotificationPacket(String category, String title, String message, boolean lowPowerPrompt)
        implements CustomPacketPayload {
    public static final Type<PhoneNotificationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_notification"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneNotificationPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.category());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.title());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.message());
                ByteBufCodecs.BOOL.encode(buffer, packet.lowPowerPrompt());
            }, buffer -> new PhoneNotificationPacket(ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)));

    public PhoneNotificationPacket(String title, String message, boolean lowPowerPrompt) {
        this("SMS", title, message, lowPowerPrompt);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
