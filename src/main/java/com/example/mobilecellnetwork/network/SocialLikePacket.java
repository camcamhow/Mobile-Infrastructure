package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialLikePacket(String postId, String filter) implements CustomPacketPayload {
    public static final Type<SocialLikePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_like"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialLikePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SocialLikePacket::postId,
                    ByteBufCodecs.STRING_UTF8, SocialLikePacket::filter, SocialLikePacket::new);

    public SocialLikePacket(String postId) {
        this(postId, "ALL");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
