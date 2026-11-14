package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SocialProfilePacket(String username, int color, byte[] avatar) implements CustomPacketPayload {
    public static final int MAX_AVATAR_BYTES = 1_000_000;
    public static final Type<SocialProfilePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "social_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SocialProfilePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buffer, packet.username());
                buffer.writeInt(packet.color());
                writeImage(buffer, packet.avatar());
            }, buffer -> new SocialProfilePacket(
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readInt(), readImage(buffer)));

    public SocialProfilePacket {
        avatar = avatar == null ? new byte[0] : avatar.clone();
    }

    @Override
    public byte[] avatar() {
        return avatar.clone();
    }

    static void writeImage(RegistryFriendlyByteBuf buffer, byte[] image) {
        writeImage(buffer, image, MAX_AVATAR_BYTES);
    }

    static void writeImage(RegistryFriendlyByteBuf buffer, byte[] image, int maxBytes) {
        if (image.length > maxBytes) {
            throw new IllegalArgumentException("Social image exceeds 1 MiB.");
        }
        buffer.writeVarInt(image.length);
        buffer.writeBytes(image);
    }

    static byte[] readImage(RegistryFriendlyByteBuf buffer) {
        return readImage(buffer, MAX_AVATAR_BYTES);
    }

    static byte[] readImage(RegistryFriendlyByteBuf buffer, int maxBytes) {
        int length = buffer.readVarInt();
        if (length < 0 || length > maxBytes) {
            throw new IllegalArgumentException("Social image exceeds 1 MiB.");
        }
        byte[] image = new byte[length];
        buffer.readBytes(image);
        return image;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
