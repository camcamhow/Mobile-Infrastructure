package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallRequestPacket(String number, boolean videoCall) implements CustomPacketPayload {
    public CallRequestPacket(String number) {
        this(number, false);
    }

    public static final Type<CallRequestPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "call_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallRequestPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, CallRequestPacket::number,
                    ByteBufCodecs.BOOL, CallRequestPacket::videoCall, CallRequestPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
