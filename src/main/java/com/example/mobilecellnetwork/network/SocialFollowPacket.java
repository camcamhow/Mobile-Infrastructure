package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialFollowPacket(String playerId) implements CustomPacketPayload {
    public static final Type<SocialFollowPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_follow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialFollowPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SocialFollowPacket::playerId, SocialFollowPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
