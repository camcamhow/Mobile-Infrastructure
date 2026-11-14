package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetPhonePinPacket(String pin) implements CustomPacketPayload {
    public static final Type<SetPhonePinPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "set_phone_pin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPhonePinPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SetPhonePinPacket::pin, SetPhonePinPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
