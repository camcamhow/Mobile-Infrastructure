package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.Chat;
import com.example.mobilecellnetwork.data.ChatType;
import com.example.mobilecellnetwork.data.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatListSyncPacket(List<Chat> chats) implements CustomPacketPayload {
    public static final Type<ChatListSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "chat_list_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatListSyncPacket> STREAM_CODEC =
            StreamCodec.of(ChatListSyncPacket::encode, ChatListSyncPacket::decode);

    public ChatListSyncPacket {
        chats = List.copyOf(chats);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ChatListSyncPacket packet) {
        buffer.writeVarInt(Math.min(packet.chats().size(), 512));
        packet.chats().stream().limit(512).forEach(chat -> {
            buffer.writeUUID(chat.id());
            ByteBufCodecs.STRING_UTF8.encode(buffer, chat.type().name());
            ByteBufCodecs.STRING_UTF8.encode(buffer, chat.displayName());
            writeStrings(buffer, chat.memberNumbers(), 64);
            buffer.writeLong(chat.lastReadTimestamp());
            buffer.writeBoolean(chat.pinnedToTop());
            buffer.writeVarInt(Math.min(chat.messages().size(), 1000));
            chat.messages().stream().limit(1000).forEach(message -> {
                buffer.writeUUID(message.id());
                ByteBufCodecs.STRING_UTF8.encode(buffer, message.senderNumber());
                ByteBufCodecs.STRING_UTF8.encode(buffer, message.body());
                buffer.writeLong(message.timestamp());
                buffer.writeBoolean(message.read());
            });
        });
    }

    private static ChatListSyncPacket decode(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 512);
        List<Chat> chats = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buffer.readUUID();
            ChatType type;
            try {
                type = ChatType.valueOf(ByteBufCodecs.STRING_UTF8.decode(buffer));
            } catch (IllegalArgumentException exception) {
                type = ChatType.DIRECT;
            }
            String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
            List<String> members = readStrings(buffer, 64);
            long lastRead = buffer.readLong();
            boolean pinned = buffer.readBoolean();
            int messageCount = Math.clamp(buffer.readVarInt(), 0, 1000);
            List<Message> messages = new ArrayList<>(messageCount);
            for (int messageIndex = 0; messageIndex < messageCount; messageIndex++) {
                messages.add(new Message(buffer.readUUID(), ByteBufCodecs.STRING_UTF8.decode(buffer),
                        ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readLong(), buffer.readBoolean()));
            }
            chats.add(new Chat(id, type, name, members, messages, lastRead, pinned));
        }
        return new ChatListSyncPacket(chats);
    }

    private static void writeStrings(RegistryFriendlyByteBuf buffer, List<String> values, int maximum) {
        buffer.writeVarInt(Math.min(maximum, values.size()));
        values.stream().limit(maximum).forEach(value -> ByteBufCodecs.STRING_UTF8.encode(buffer, value));
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buffer, int maximum) {
        int count = Math.clamp(buffer.readVarInt(), 0, maximum);
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
        }
        return values;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
