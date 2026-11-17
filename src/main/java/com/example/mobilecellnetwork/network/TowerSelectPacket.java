package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TowerSelectPacket(int action, long pos) implements CustomPacketPayload {
    public static final int REQUEST_LIST = 0;
    public static final int AUTOMATIC = 1;
    public static final int SELECT = 2;

    public static final Type<TowerSelectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "tower_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TowerSelectPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, TowerSelectPacket::action,
                    ByteBufCodecs.VAR_LONG, TowerSelectPacket::pos, TowerSelectPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
