package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record KnownPhoneDevicesPacket(List<Device> devices) implements CustomPacketPayload {
    public static final Type<KnownPhoneDevicesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "known_phone_devices"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KnownPhoneDevicesPacket> STREAM_CODEC =
            StreamCodec.of(KnownPhoneDevicesPacket::write, KnownPhoneDevicesPacket::read);

    public KnownPhoneDevicesPacket {
        devices = List.copyOf(devices);
    }

    private static void write(RegistryFriendlyByteBuf buffer, KnownPhoneDevicesPacket packet) {
        List<Device> limited = packet.devices().stream().limit(128).toList();
        buffer.writeVarInt(limited.size());
        limited.forEach(device -> {
            buffer.writeUUID(device.id());
            buffer.writeLong(device.lastSeen());
            buffer.writeBoolean(device.current());
            buffer.writeUtf(device.label());
            buffer.writeBoolean(device.bankLocked());
            buffer.writeBoolean(device.allLocked());
        });
    }

    private static KnownPhoneDevicesPacket read(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 128);
        List<Device> devices = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            devices.add(new Device(buffer.readUUID(), buffer.readLong(), buffer.readBoolean(),
                    buffer.readUtf(64), buffer.readBoolean(), buffer.readBoolean()));
        }
        return new KnownPhoneDevicesPacket(devices);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Device(UUID id, long lastSeen, boolean current, String label,
                         boolean bankLocked, boolean allLocked) {
    }
}
