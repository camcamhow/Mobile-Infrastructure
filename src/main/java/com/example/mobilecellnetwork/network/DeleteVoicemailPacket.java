package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteVoicemailPacket(int index) implements CustomPacketPayload {
    public static final Type<DeleteVoicemailPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "delete_voicemail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteVoicemailPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> buffer.writeVarInt(packet.index()), buffer -> new DeleteVoicemailPacket(buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
