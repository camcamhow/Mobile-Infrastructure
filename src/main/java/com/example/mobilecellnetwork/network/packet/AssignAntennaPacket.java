package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AssignAntennaPacket(String antennaPos, String networkId)
        implements CustomPacketPayload {
    public static final Type<AssignAntennaPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "assign_antenna"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssignAntennaPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.antennaPos());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
            }, buffer -> new AssignAntennaPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
