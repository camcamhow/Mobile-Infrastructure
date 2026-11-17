package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapAntennaCacheRequestPacket(int centerX, int centerZ, int resolution)
        implements CustomPacketPayload {
    public static final Type<MapAntennaCacheRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "map_antenna_cache_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapAntennaCacheRequestPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, MapAntennaCacheRequestPacket::centerX,
                    ByteBufCodecs.VAR_INT, MapAntennaCacheRequestPacket::centerZ,
                    ByteBufCodecs.VAR_INT, MapAntennaCacheRequestPacket::resolution,
                    MapAntennaCacheRequestPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
