package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AppInstallPacket(String appId) implements CustomPacketPayload {
    public static final Type<AppInstallPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "app_install"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AppInstallPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, AppInstallPacket::appId, AppInstallPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
