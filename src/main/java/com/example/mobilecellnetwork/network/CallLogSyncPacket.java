package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.CallLogEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallLogSyncPacket(List<CallLogEntry> entries) implements CustomPacketPayload {
    public static final Type<CallLogSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "call_log_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallLogSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.entries().size());
                packet.entries().forEach(entry -> entry.write(buffer));
            },
            buffer -> {
                int size = Math.min(buffer.readVarInt(), 200);
                List<CallLogEntry> entries = new ArrayList<>();
                for (int index = 0; index < size; index++) {
                    entries.add(CallLogEntry.read(buffer));
                }
                return new CallLogSyncPacket(List.copyOf(entries));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
