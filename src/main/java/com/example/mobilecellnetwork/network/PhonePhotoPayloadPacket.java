package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhonePhotoPayloadPacket(UUID phoneId, String name, byte[] image) implements CustomPacketPayload {
    public static final int MAX_IMAGE_BYTES = 1_000_000;
    public static final Type<PhonePhotoPayloadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_photo_payload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhonePhotoPayloadPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeUUID(packet.phoneId());
                buffer.writeUtf(packet.name());
                SocialProfilePacket.writeImage(buffer, packet.image(), MAX_IMAGE_BYTES);
            }, buffer -> new PhonePhotoPayloadPacket(buffer.readUUID(), buffer.readUtf(128),
                    SocialProfilePacket.readImage(buffer, MAX_IMAGE_BYTES)));

    public PhonePhotoPayloadPacket {
        image = image == null ? new byte[0] : image.clone();
    }

    @Override
    public byte[] image() { return image.clone(); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
