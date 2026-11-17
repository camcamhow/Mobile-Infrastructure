package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnclaimAntennaPacket(String antennaPos) implements CustomPacketPayload {
    public static final Type<UnclaimAntennaPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "unclaim_antenna"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnclaimAntennaPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.antennaPos());
            }, buffer -> new UnclaimAntennaPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
