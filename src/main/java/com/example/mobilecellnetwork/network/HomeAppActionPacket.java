package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HomeAppActionPacket(String action, String appId, List<String> order) implements CustomPacketPayload {
    public static final Type<HomeAppActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "home_app_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HomeAppActionPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeUtf(packet.action, 16);
                buffer.writeUtf(packet.appId, 32);
                buffer.writeVarInt(packet.order.size());
                packet.order.forEach(id -> buffer.writeUtf(id, 32));
            }, buffer -> {
                String action = buffer.readUtf(16);
                String appId = buffer.readUtf(32);
                int count = buffer.readVarInt();
                if (count < 0 || count > 32) throw new IllegalArgumentException("Invalid home app count");
                List<String> order = new ArrayList<>(count);
                for (int i = 0; i < count; i++) order.add(buffer.readUtf(32));
                return new HomeAppActionPacket(action, appId, order);
            });

    public HomeAppActionPacket {
        order = List.copyOf(order);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
