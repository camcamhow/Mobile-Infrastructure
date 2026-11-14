package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetNetworkPasswordPacket(String networkId, String newPassword) implements CustomPacketPayload {
    public static final Type<SetNetworkPasswordPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "set_network_password"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetNetworkPasswordPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetNetworkPasswordPacket::networkId,
                    ByteBufCodecs.STRING_UTF8, SetNetworkPasswordPacket::newPassword,
                    SetNetworkPasswordPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
