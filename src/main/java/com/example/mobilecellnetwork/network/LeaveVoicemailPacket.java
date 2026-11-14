package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LeaveVoicemailPacket(String number, String message) implements CustomPacketPayload {
    public static final Type<LeaveVoicemailPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "leave_voicemail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LeaveVoicemailPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, LeaveVoicemailPacket::number,
                    ByteBufCodecs.STRING_UTF8, LeaveVoicemailPacket::message, LeaveVoicemailPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
