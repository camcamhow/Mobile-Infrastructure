package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhonePinResultPacket(boolean accepted)
        implements CustomPacketPayload {
    public static final Type<PhonePinResultPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_pin_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhonePinResultPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, PhonePinResultPacket::accepted,
                    PhonePinResultPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
