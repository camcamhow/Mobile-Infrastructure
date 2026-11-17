package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallStatePacket(
        String sessionId,
        String state,
        String partnerName,
        String reason,
        long startedAtMillis,
        boolean muted,
        boolean incoming
) implements CustomPacketPayload {
    public static final Type<CallStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "call_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallStatePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.sessionId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.state());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.partnerName());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.reason());
                buffer.writeLong(packet.startedAtMillis());
                ByteBufCodecs.BOOL.encode(buffer, packet.muted());
                ByteBufCodecs.BOOL.encode(buffer, packet.incoming());
            },
            buffer -> new CallStatePacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readLong(),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
