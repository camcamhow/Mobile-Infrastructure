package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MarkChatReadPacket(UUID chatId) implements CustomPacketPayload {
    public static final Type<MarkChatReadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "mark_chat_read"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MarkChatReadPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeUUID(packet.chatId()),
                    buffer -> new MarkChatReadPacket(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
