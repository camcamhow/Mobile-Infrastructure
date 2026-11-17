package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EmergencyDispatchDataPacket(List<Entry> messages) implements CustomPacketPayload {
    public static final Type<EmergencyDispatchDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "emergency_dispatch_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmergencyDispatchDataPacket> STREAM_CODEC =
            StreamCodec.of(EmergencyDispatchDataPacket::encode, EmergencyDispatchDataPacket::decode);

    public EmergencyDispatchDataPacket {
        messages = List.copyOf(messages);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, EmergencyDispatchDataPacket packet) {
        int count = Math.min(500, packet.messages().size());
        buffer.writeVarInt(count);
        packet.messages().stream().limit(count).forEach(message -> {
            buffer.writeUUID(message.id());
            buffer.writeUUID(message.senderId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, message.senderName());
            ByteBufCodecs.STRING_UTF8.encode(buffer, message.senderNumber());
            ByteBufCodecs.STRING_UTF8.encode(buffer, message.body());
            ByteBufCodecs.STRING_UTF8.encode(buffer, message.dimension());
            buffer.writeInt(message.x());
            buffer.writeInt(message.y());
            buffer.writeInt(message.z());
            buffer.writeLong(message.timestamp());
        });
    }

    private static EmergencyDispatchDataPacket decode(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 500);
        List<Entry> messages = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            messages.add(new Entry(buffer.readUUID(), buffer.readUUID(),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readLong()));
        }
        return new EmergencyDispatchDataPacket(messages);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(UUID id, UUID senderId, String senderName, String senderNumber, String body,
                        String dimension, int x, int y, int z, long timestamp) {
        public Entry {
            senderName = senderName == null ? "" : senderName;
            senderNumber = senderNumber == null ? "" : senderNumber;
            body = body == null ? "" : body;
            dimension = dimension == null ? "" : dimension;
        }
    }
}
