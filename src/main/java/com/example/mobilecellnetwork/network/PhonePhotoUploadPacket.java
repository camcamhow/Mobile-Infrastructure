package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhonePhotoUploadPacket(UUID phoneId, String name, byte[] image) implements CustomPacketPayload {
    public static final Type<PhonePhotoUploadPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_photo_upload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhonePhotoUploadPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeUUID(packet.phoneId());
                buffer.writeUtf(packet.name());
                SocialProfilePacket.writeImage(buffer, packet.image(), PhonePhotoPayloadPacket.MAX_IMAGE_BYTES);
            }, buffer -> new PhonePhotoUploadPacket(buffer.readUUID(), buffer.readUtf(128),
                    SocialProfilePacket.readImage(buffer, PhonePhotoPayloadPacket.MAX_IMAGE_BYTES)));

    public PhonePhotoUploadPacket {
        image = image == null ? new byte[0] : image.clone();
    }

    @Override
    public byte[] image() { return image.clone(); }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
