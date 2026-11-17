package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.CallLogEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallLogEntryPacket(CallLogEntry entry) implements CustomPacketPayload {
    public static final Type<CallLogEntryPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "call_log_entry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallLogEntryPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> packet.entry().write(buffer),
            buffer -> new CallLogEntryPacket(CallLogEntry.read(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
