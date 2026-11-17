package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AntennaCachePacket(UUID playerUuid, String dimension, List<Antenna> antennas)
        implements CustomPacketPayload {
    private static final int MAX_ANTENNAS = 1024;
    public static final Type<AntennaCachePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "antenna_cache"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AntennaCachePacket> STREAM_CODEC =
            StreamCodec.of(AntennaCachePacket::write, AntennaCachePacket::read);

    public AntennaCachePacket {
        antennas = List.copyOf(antennas);
        if (antennas.size() > MAX_ANTENNAS) {
            throw new IllegalArgumentException("Antenna cache exceeds the packet limit.");
        }
    }

    private static void write(RegistryFriendlyByteBuf buffer, AntennaCachePacket packet) {
        buffer.writeUUID(packet.playerUuid());
        buffer.writeUtf(packet.dimension());
        buffer.writeVarInt(packet.antennas().size());
        for (Antenna antenna : packet.antennas()) {
            buffer.writeVarInt(antenna.x());
            buffer.writeVarInt(antenna.y());
            buffer.writeVarInt(antenna.z());
            buffer.writeDouble(antenna.range());
            buffer.writeUtf(antenna.maxSignalTier());
        }
    }

    private static AntennaCachePacket read(RegistryFriendlyByteBuf buffer) {
        UUID playerUuid = buffer.readUUID();
        String dimension = buffer.readUtf(256);
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ANTENNAS) {
            throw new IllegalArgumentException("Invalid antenna cache entry count: " + count);
        }
        List<Antenna> antennas = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            antennas.add(new Antenna(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readDouble(), buffer.readUtf(32)));
        }
        return new AntennaCachePacket(playerUuid, dimension, antennas);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Antenna(int x, int y, int z, double range, String maxSignalTier) {
    }
}
