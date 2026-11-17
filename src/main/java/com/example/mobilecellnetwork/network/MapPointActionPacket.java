package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapPointActionPacket(String action, String pointId, String name, String visibility,
                                   int x, int y, int z, String recipientNumber)
        implements CustomPacketPayload {
    public static final Type<MapPointActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "map_point_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapPointActionPacket> STREAM_CODEC =
            StreamCodec.of(MapPointActionPacket::encode, MapPointActionPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, MapPointActionPacket packet) {
        buffer.writeUtf(packet.action(), 16);
        buffer.writeUtf(packet.pointId(), 36);
        buffer.writeUtf(packet.name(), 64);
        buffer.writeUtf(packet.visibility(), 16);
        buffer.writeInt(packet.x());
        buffer.writeInt(packet.y());
        buffer.writeInt(packet.z());
        buffer.writeUtf(packet.recipientNumber(), 16);
    }

    private static MapPointActionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new MapPointActionPacket(buffer.readUtf(16), buffer.readUtf(36), buffer.readUtf(64),
                buffer.readUtf(16), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readUtf(16));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
