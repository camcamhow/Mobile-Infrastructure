package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapInteractionPacket(int resolution) implements CustomPacketPayload {
    public static final Type<MapInteractionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "map_interaction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapInteractionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, MapInteractionPacket::resolution,
                    MapInteractionPacket::new);

    public MapInteractionPacket() {
        this(1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
