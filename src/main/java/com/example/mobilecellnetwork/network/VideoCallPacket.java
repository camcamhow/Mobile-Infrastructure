package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VideoCallPacket(UUID session, boolean enabled, boolean self, byte[] image) implements CustomPacketPayload {
    public static final int MAX_BYTES = 48_000;
    public static final Type<VideoCallPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "video_call"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VideoCallPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeUUID(packet.session());
                buffer.writeBoolean(packet.enabled());
                buffer.writeBoolean(packet.self());
                SocialProfilePacket.writeImage(buffer, packet.image, MAX_BYTES);
            }, buffer -> new VideoCallPacket(buffer.readUUID(), buffer.readBoolean(), buffer.readBoolean(),
                    SocialProfilePacket.readImage(buffer, MAX_BYTES)));

    public VideoCallPacket {
        image = image.clone();
    }

    @Override
    public byte[] image() {
        return image.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
