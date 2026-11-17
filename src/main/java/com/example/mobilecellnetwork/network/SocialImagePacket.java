package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialImagePacket(String postId, byte[] image) implements CustomPacketPayload {
    public static final Type<SocialImagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialImagePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buffer, packet.postId());
                SocialProfilePacket.writeImage(buffer, packet.image());
            }, buffer -> new SocialImagePacket(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer),
                    SocialProfilePacket.readImage(buffer)));

    public SocialImagePacket {
        image = image == null ? new byte[0] : image.clone();
    }

    @Override
    public byte[] image() {
        return image.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
