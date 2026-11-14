package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SettingsUpdatePacket(String setting, String value) implements CustomPacketPayload {
    public static final Type<SettingsUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "settings_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsUpdatePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SettingsUpdatePacket::setting,
                    ByteBufCodecs.STRING_UTF8, SettingsUpdatePacket::value, SettingsUpdatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
