package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CreateGroupChatPacket(String groupName, List<String> memberNumbers) implements CustomPacketPayload {
    public static final Type<CreateGroupChatPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "create_group_chat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateGroupChatPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.groupName());
                buffer.writeVarInt(Math.min(64, packet.memberNumbers().size()));
                packet.memberNumbers().stream().limit(64)
                        .forEach(number -> ByteBufCodecs.STRING_UTF8.encode(buffer, number));
            }, buffer -> {
                String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
                int count = Math.clamp(buffer.readVarInt(), 0, 64);
                List<String> members = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    members.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                }
                return new CreateGroupChatPacket(name, members);
            });

    public CreateGroupChatPacket {
        groupName = groupName == null ? "" : groupName;
        memberNumbers = List.copyOf(memberNumbers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
