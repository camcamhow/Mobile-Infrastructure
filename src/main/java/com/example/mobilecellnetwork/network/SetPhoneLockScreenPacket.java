package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetPhoneLockScreenPacket(boolean enabled) implements CustomPacketPayload {
    public static final Type<SetPhoneLockScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "set_phone_lock_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPhoneLockScreenPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SetPhoneLockScreenPacket::enabled,
                    SetPhoneLockScreenPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
