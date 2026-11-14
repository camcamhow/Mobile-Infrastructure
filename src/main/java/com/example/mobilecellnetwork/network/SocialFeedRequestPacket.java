package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialFeedRequestPacket(String filter) implements CustomPacketPayload {
    public static final Type<SocialFeedRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_feed_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialFeedRequestPacket> STREAM_CODEC =
            StreamCodec.composite(StreamCodec.of(
                            (buffer, value) -> net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8
                                    .encode(buffer, value),
                            buffer -> net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    SocialFeedRequestPacket::filter, SocialFeedRequestPacket::new);

    public SocialFeedRequestPacket() {
        this("ALL");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
