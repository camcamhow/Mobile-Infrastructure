package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.gui.PhoneStyle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneStyleSyncPacket(PhoneStyle style) implements CustomPacketPayload {
    public static final Type<PhoneStyleSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_style_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneStyleSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) ->
                            ByteBufCodecs.STRING_UTF8.encode(buffer, packet.style().name()),
                    buffer -> new PhoneStyleSyncPacket(PhoneStyle.fromName(
                            ByteBufCodecs.STRING_UTF8.decode(buffer))));

    public PhoneStyleSyncPacket {
        if (style == null) {
            style = PhoneStyle.DEFAULT;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
