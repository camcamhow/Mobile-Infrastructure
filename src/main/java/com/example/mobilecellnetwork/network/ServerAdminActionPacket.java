package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerAdminActionPacket(String tab, String action, String target, String value)
        implements CustomPacketPayload {
    public static final Type<ServerAdminActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "admin_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerAdminActionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ServerAdminActionPacket::tab,
                    ByteBufCodecs.STRING_UTF8, ServerAdminActionPacket::action,
                    ByteBufCodecs.STRING_UTF8, ServerAdminActionPacket::target,
                    ByteBufCodecs.STRING_UTF8, ServerAdminActionPacket::value,
                    ServerAdminActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
