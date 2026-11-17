package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RoamingProposalResponsePacket(String proposalId, ProposalResponse response,
                                            long counterPerMinute, long counterPerText, long counterPerMb)
        implements CustomPacketPayload {
    public static final Type<RoamingProposalResponsePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "roaming_proposal_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RoamingProposalResponsePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.proposalId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.response().name());
                buffer.writeLong(packet.counterPerMinute());
                buffer.writeLong(packet.counterPerText());
                buffer.writeLong(packet.counterPerMb());
            }, buffer -> new RoamingProposalResponsePacket(ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ProposalResponse.valueOf(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    buffer.readLong(), buffer.readLong(), buffer.readLong()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
