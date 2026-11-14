package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhonePhotoListPacket(UUID phoneId, List<String> names) implements CustomPacketPayload {
    public static final Type<PhonePhotoListPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_photo_list"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhonePhotoListPacket> STREAM_CODEC =
            StreamCodec.of(PhonePhotoListPacket::write, PhonePhotoListPacket::read);

    public PhonePhotoListPacket {
        names = List.copyOf(names);
    }

    private static void write(RegistryFriendlyByteBuf buffer, PhonePhotoListPacket packet) {
        buffer.writeUUID(packet.phoneId());
        List<String> names = packet.names().stream().limit(24).toList();
        buffer.writeVarInt(names.size());
        names.forEach(buffer::writeUtf);
    }

    private static PhonePhotoListPacket read(RegistryFriendlyByteBuf buffer) {
        UUID phoneId = buffer.readUUID();
        int count = Math.clamp(buffer.readVarInt(), 0, 24);
        List<String> names = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            names.add(buffer.readUtf(128));
        }
        return new PhonePhotoListPacket(phoneId, names);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
