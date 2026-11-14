package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CallControlPacket(Action action, boolean muted) implements CustomPacketPayload {
    public static final Type<CallControlPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "call_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallControlPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, packet -> packet.action().name(),
                    ByteBufCodecs.BOOL, CallControlPacket::muted,
                    (action, muted) -> new CallControlPacket(Action.valueOf(action), muted));

    public enum Action {
        ACCEPT,
        DECLINE,
        HANG_UP,
        MUTE,
        TOGGLE_RINGTONE
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
