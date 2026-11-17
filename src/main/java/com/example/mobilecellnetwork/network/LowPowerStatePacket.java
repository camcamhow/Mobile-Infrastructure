package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LowPowerStatePacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<LowPowerStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "low_power_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LowPowerStatePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, LowPowerStatePacket::enabled, LowPowerStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
