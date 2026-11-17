package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoveKnownPhonePacket(UUID phoneId) implements CustomPacketPayload {
    public static final Type<RemoveKnownPhonePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "remove_known_phone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveKnownPhonePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, packet -> packet.phoneId().toString(),
                    id -> new RemoveKnownPhonePacket(UUID.fromString(id)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
