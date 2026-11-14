package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RequestPhonePhotosPacket(UUID phoneId) implements CustomPacketPayload {
    public static final Type<RequestPhonePhotosPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "request_phone_photos"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestPhonePhotosPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, packet -> packet.phoneId().toString(),
                    id -> new RequestPhonePhotosPacket(UUID.fromString(id)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
