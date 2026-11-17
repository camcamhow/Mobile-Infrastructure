package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanSelectPacket(String planId) implements CustomPacketPayload {
    public static final Type<PlanSelectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "plan_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlanSelectPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PlanSelectPacket::planId, PlanSelectPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
