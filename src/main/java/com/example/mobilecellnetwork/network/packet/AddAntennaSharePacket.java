package com.example.mobilecellnetwork.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AddAntennaSharePacket(BlockPos antennaPos, String networkId) implements CustomPacketPayload {
    public static final Type<AddAntennaSharePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "add_antenna_share"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AddAntennaSharePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBlockPos(packet.antennaPos());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
            }, buffer -> new AddAntennaSharePacket(buffer.readBlockPos(), ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
