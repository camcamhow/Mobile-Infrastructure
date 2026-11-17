package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FlashlightPacket(boolean on) implements CustomPacketPayload {
    public static final Type<FlashlightPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "flashlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashlightPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, FlashlightPacket::on, FlashlightPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
