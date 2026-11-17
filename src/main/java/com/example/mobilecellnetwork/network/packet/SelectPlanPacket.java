package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectPlanPacket(String planId) implements CustomPacketPayload {
    public static final Type<SelectPlanPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "select_plan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectPlanPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SelectPlanPacket::planId, SelectPlanPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
