package com.example.mobilecellnetwork.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetPlayerNumberPacket(String playerUuid, String number)
        implements CustomPacketPayload {
    public static final Type<SetPlayerNumberPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "set_player_number"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPlayerNumberPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.playerUuid());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.number());
            }, buffer -> new SetPlayerNumberPacket(
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
