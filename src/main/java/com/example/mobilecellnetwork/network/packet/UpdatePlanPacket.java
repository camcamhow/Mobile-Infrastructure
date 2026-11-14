package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdatePlanPacket(String planId, String displayName, String price, String cycleDays,
                               String callMinutes, String textMessages, String dataAllowanceMb)
        implements CustomPacketPayload {
    public static final Type<UpdatePlanPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "update_plan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePlanPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.planId());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.displayName());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.price());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.cycleDays());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.callMinutes());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.textMessages());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.dataAllowanceMb());
            }, buffer -> new UpdatePlanPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
