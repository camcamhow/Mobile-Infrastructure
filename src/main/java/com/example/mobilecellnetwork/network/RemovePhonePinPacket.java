package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemovePhonePinPacket(String currentPin) implements CustomPacketPayload {
    public static final Type<RemovePhonePinPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "remove_phone_pin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemovePhonePinPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RemovePhonePinPacket::currentPin,
                    RemovePhonePinPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
