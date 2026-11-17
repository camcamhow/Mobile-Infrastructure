package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerAdminDataPacket(String tab, List<String> entries) implements CustomPacketPayload {
    public static final Type<ServerAdminDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "admin_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerAdminDataPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.tab());
                buffer.writeVarInt(Math.min(200, packet.entries().size()));
                packet.entries().stream().limit(200)
                        .forEach(entry -> ByteBufCodecs.STRING_UTF8.encode(buffer, entry));
            }, buffer -> {
                String tab = ByteBufCodecs.STRING_UTF8.decode(buffer);
                int count = Math.clamp(buffer.readVarInt(), 0, 200);
                List<String> entries = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    entries.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                }
                return new ServerAdminDataPacket(tab, entries);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
