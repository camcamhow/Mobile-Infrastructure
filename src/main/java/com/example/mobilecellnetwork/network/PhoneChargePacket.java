package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneChargePacket(int charge) implements CustomPacketPayload {
    public static final Type<PhoneChargePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_charge_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneChargePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PhoneChargePacket::charge, PhoneChargePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
