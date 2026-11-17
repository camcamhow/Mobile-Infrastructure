package com.example.mobilecellnetwork.network;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneDeviceStatePacket(UUID phoneId, boolean lockScreenEnabled, boolean pinSet,
                                     boolean lockedAll, boolean lockedBank, boolean readOnlyBank,
                                     boolean hasUnreadNotifications, boolean clearSensitiveData,
                                     String appearance, boolean setupComplete)
        implements CustomPacketPayload {
    public static final Type<PhoneDeviceStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_device_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneDeviceStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeUUID(packet.phoneId());
                buffer.writeBoolean(packet.lockScreenEnabled());
                buffer.writeBoolean(packet.pinSet());
                buffer.writeBoolean(packet.lockedAll());
                buffer.writeBoolean(packet.lockedBank());
                buffer.writeBoolean(packet.readOnlyBank());
                buffer.writeBoolean(packet.hasUnreadNotifications());
                buffer.writeBoolean(packet.clearSensitiveData());
                buffer.writeUtf(packet.appearance(), 16);
                buffer.writeBoolean(packet.setupComplete());
            }, buffer -> new PhoneDeviceStatePacket(buffer.readUUID(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(16), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
