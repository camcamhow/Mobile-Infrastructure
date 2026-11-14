package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneSetupPacket(UUID phoneId, String name, String appearance, String pin, String photo)
        implements CustomPacketPayload {
    public static final Type<PhoneSetupPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_setup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneSetupPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeUUID(packet.phoneId());
                buffer.writeUtf(packet.name(), 32);
                buffer.writeUtf(packet.appearance(), 16);
                buffer.writeUtf(packet.pin(), 12);
                buffer.writeUtf(packet.photo(), 128);
            }, buffer -> new PhoneSetupPacket(buffer.readUUID(), buffer.readUtf(32), buffer.readUtf(16),
                    buffer.readUtf(12), buffer.readUtf(128)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
