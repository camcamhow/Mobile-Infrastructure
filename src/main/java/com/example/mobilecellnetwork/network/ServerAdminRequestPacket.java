package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerAdminRequestPacket(String tab) implements CustomPacketPayload {
    public static final Type<ServerAdminRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "admin_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerAdminRequestPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerAdminRequestPacket::tab,
                    ServerAdminRequestPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
