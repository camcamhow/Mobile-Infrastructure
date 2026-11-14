package com.example.mobilecellnetwork.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record NetworkCreationResultPacket(boolean success, String message) implements CustomPacketPayload {
    public static final Type<NetworkCreationResultPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "network_creation_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NetworkCreationResultPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, NetworkCreationResultPacket::success,
                    ByteBufCodecs.STRING_UTF8, NetworkCreationResultPacket::message,
                    NetworkCreationResultPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
