package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DismissLowPowerPromptPacket() implements CustomPacketPayload {
    public static final Type<DismissLowPowerPromptPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "dismiss_low_power_prompt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DismissLowPowerPromptPacket> STREAM_CODEC =
            StreamCodec.unit(new DismissLowPowerPromptPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
