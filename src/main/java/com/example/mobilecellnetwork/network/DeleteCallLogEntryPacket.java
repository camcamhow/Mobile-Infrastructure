package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteCallLogEntryPacket(UUID entryId) implements CustomPacketPayload {
    public static final Type<DeleteCallLogEntryPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "delete_call_log_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteCallLogEntryPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> buffer.writeUUID(packet.entryId()), buffer -> new DeleteCallLogEntryPacket(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
