package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialPostPacket(String action, String postId, String text, String attachment, byte[] image)
        implements CustomPacketPayload {
    public static final int MAX_IMAGE_BYTES = 1_000_000;
    public static final Type<SocialPostPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_post"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialPostPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.action());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.postId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.text());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.attachment());
                SocialProfilePacket.writeImage(buffer, packet.image(), MAX_IMAGE_BYTES);
            }, buffer -> new SocialPostPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    SocialProfilePacket.readImage(buffer, MAX_IMAGE_BYTES)));

    public SocialPostPacket(String text, String attachment) {
        this("post", "", text, attachment, new byte[0]);
    }

    public SocialPostPacket(String action, String postId, String text, String attachment) {
        this(action, postId, text, attachment, new byte[0]);
    }

    public SocialPostPacket {
        image = image == null ? new byte[0] : image.clone();
    }

    @Override
    public byte[] image() {
        return image.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
