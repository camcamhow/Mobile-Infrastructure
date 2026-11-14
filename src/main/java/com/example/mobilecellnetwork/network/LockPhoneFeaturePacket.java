package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LockPhoneFeaturePacket(UUID targetPhoneId, String feature, boolean lock)
        implements CustomPacketPayload {
    public static final Type<LockPhoneFeaturePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "lock_phone_feature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockPhoneFeaturePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, packet -> packet.targetPhoneId().toString(),
                    ByteBufCodecs.STRING_UTF8, LockPhoneFeaturePacket::feature,
                    ByteBufCodecs.BOOL, LockPhoneFeaturePacket::lock,
                    (id, feature, lock) -> new LockPhoneFeaturePacket(UUID.fromString(id), feature, lock));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
