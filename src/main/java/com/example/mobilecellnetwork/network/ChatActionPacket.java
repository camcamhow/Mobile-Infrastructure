package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatActionPacket(String action, UUID chatId, String value) implements CustomPacketPayload {
    public static final Type<ChatActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "chat_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatActionPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.action());
                buffer.writeUUID(packet.chatId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.value());
            }, buffer -> new ChatActionPacket(ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readUUID(),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
