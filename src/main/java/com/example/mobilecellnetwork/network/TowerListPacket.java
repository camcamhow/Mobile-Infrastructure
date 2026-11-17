package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TowerListPacket(List<Entry> towers, long connectedPos, long preferredPos)
        implements CustomPacketPayload {
    public static final Type<TowerListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "tower_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TowerListPacket> STREAM_CODEC =
            StreamCodec.of(TowerListPacket::write, TowerListPacket::read);

    public record Entry(long pos, String name, String tier, int strengthPercent, boolean roaming) {
    }

    private static void write(RegistryFriendlyByteBuf buf, TowerListPacket packet) {
        buf.writeVarInt(packet.towers.size());
        for (Entry entry : packet.towers) {
            buf.writeLong(entry.pos);
            buf.writeUtf(entry.name);
            buf.writeUtf(entry.tier);
            buf.writeVarInt(entry.strengthPercent);
            buf.writeBoolean(entry.roaming);
        }
        buf.writeLong(packet.connectedPos);
        buf.writeLong(packet.preferredPos);
    }

    private static TowerListPacket read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Entry(buf.readLong(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));
        }
        return new TowerListPacket(list, buf.readLong(), buf.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
