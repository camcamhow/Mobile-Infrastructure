package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EmergencyMessagePacket(String body) implements CustomPacketPayload {
    public static final Type<EmergencyMessagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "emergency_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmergencyMessagePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, EmergencyMessagePacket::body,
                    EmergencyMessagePacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
