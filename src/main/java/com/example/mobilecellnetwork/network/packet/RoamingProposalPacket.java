package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RoamingProposalPacket(String targetNetworkId, String rateMin, String rateText, String rateMb, String message)
        implements CustomPacketPayload {
    public static final Type<RoamingProposalPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "roaming_proposal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RoamingProposalPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.targetNetworkId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.rateMin());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.rateText());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.rateMb());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.message());
            }, buffer -> new RoamingProposalPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
