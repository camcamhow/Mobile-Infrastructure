package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CancelPlanPacket() implements CustomPacketPayload {
    public static final Type<CancelPlanPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "cancel_plan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelPlanPacket> STREAM_CODEC =
            StreamCodec.unit(new CancelPlanPacket());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
