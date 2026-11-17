package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WipePhonePacket(UUID targetPhoneId) implements CustomPacketPayload {
    public static final Type<WipePhonePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "wipe_phone"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WipePhonePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, packet -> packet.targetPhoneId().toString(),
                    id -> new WipePhonePacket(UUID.fromString(id)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
