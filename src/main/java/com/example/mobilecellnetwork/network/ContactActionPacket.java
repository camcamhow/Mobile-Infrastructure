package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ContactActionPacket(String name, String number) implements CustomPacketPayload {
    public static final Type<ContactActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "contact_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContactActionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ContactActionPacket::name,
                    ByteBufCodecs.STRING_UTF8, ContactActionPacket::number, ContactActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
