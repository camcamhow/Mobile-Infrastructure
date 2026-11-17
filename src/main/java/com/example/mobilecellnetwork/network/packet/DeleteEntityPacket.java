package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeleteEntityPacket(String entityId, String entityType)
        implements CustomPacketPayload {
    public static final Type<DeleteEntityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "delete_entity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteEntityPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.entityId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.entityType());
            }, buffer -> new DeleteEntityPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
