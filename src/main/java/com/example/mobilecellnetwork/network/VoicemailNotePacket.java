package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VoicemailNotePacket(int index, String note) implements CustomPacketPayload {
    public static final Type<VoicemailNotePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "voicemail_note"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoicemailNotePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, VoicemailNotePacket::index,
                    ByteBufCodecs.STRING_UTF8, VoicemailNotePacket::note, VoicemailNotePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
