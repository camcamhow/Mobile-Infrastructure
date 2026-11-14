package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetLowPowerPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetLowPowerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "set_low_power"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetLowPowerPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SetLowPowerPacket::enabled, SetLowPowerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
