package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.MapPointOfInterest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MapPointSyncPacket(List<MapPointOfInterest> points) implements CustomPacketPayload {
    public static final Type<MapPointSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "map_point_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapPointSyncPacket> STREAM_CODEC =
            StreamCodec.of(MapPointSyncPacket::encode, MapPointSyncPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, MapPointSyncPacket packet) {
        buffer.writeVarInt(Math.min(packet.points().size(), 512));
        packet.points().stream().limit(512).forEach(point -> {
            buffer.writeUUID(point.id());
            buffer.writeUtf(point.name(), 64);
            buffer.writeUtf(point.dimension(), 128);
            buffer.writeInt(point.x());
            buffer.writeInt(point.y());
            buffer.writeInt(point.z());
            buffer.writeBoolean(point.ownerUUID() != null);
            if (point.ownerUUID() != null) {
                buffer.writeUUID(point.ownerUUID());
            }
            buffer.writeUtf(point.ownerName(), 64);
            buffer.writeUtf(point.visibility().name(), 16);
        });
    }

    private static MapPointSyncPacket decode(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 512);
        List<MapPointOfInterest> points = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            UUID id = buffer.readUUID();
            String name = buffer.readUtf(64);
            String dimension = buffer.readUtf(128);
            int x = buffer.readInt();
            int y = buffer.readInt();
            int z = buffer.readInt();
            UUID owner = buffer.readBoolean() ? buffer.readUUID() : null;
            String ownerName = buffer.readUtf(64);
            MapPointOfInterest.Visibility visibility;
            try {
                visibility = MapPointOfInterest.Visibility.valueOf(buffer.readUtf(16));
            } catch (IllegalArgumentException exception) {
                visibility = MapPointOfInterest.Visibility.PRIVATE;
            }
            points.add(new MapPointOfInterest(id, name, dimension, x, y, z, owner, ownerName, visibility));
        }
        return new MapPointSyncPacket(points);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
