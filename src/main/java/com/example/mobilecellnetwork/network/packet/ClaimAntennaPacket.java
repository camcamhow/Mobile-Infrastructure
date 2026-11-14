package com.example.mobilecellnetwork.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClaimAntennaPacket(BlockPos antennaPos, String networkId, String password)
        implements CustomPacketPayload {
    public static final Type<ClaimAntennaPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "claim_antenna"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimAntennaPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBlockPos(packet.antennaPos());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.password());
            }, buffer -> new ClaimAntennaPacket(buffer.readBlockPos(),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
