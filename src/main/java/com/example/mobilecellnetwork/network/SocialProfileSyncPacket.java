package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialProfileSyncPacket(String username, int color, byte[] avatar) implements CustomPacketPayload {
    public static final Type<SocialProfileSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_profile_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialProfileSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buffer, packet.username());
                buffer.writeInt(packet.color());
                SocialProfilePacket.writeImage(buffer, packet.avatar());
            }, buffer -> new SocialProfileSyncPacket(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readInt(), SocialProfilePacket.readImage(buffer)));

    public SocialProfileSyncPacket {
        avatar = avatar == null ? new byte[0] : avatar.clone();
    }

    @Override
    public byte[] avatar() {
        return avatar.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
