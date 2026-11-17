package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhonePhotoDeletePacket(UUID phoneId, String name) implements CustomPacketPayload {
    public static final Type<PhonePhotoDeletePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_photo_delete"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhonePhotoDeletePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeUUID(packet.phoneId());
                buffer.writeUtf(packet.name(), 128);
            }, buffer -> new PhonePhotoDeletePacket(buffer.readUUID(), buffer.readUtf(128)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
