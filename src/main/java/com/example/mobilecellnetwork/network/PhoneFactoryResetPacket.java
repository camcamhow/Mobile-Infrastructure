package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneFactoryResetPacket(UUID phoneId) implements CustomPacketPayload {
    public static final Type<PhoneFactoryResetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_factory_reset"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneFactoryResetPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeUUID(packet.phoneId()),
                    buffer -> new PhoneFactoryResetPacket(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
