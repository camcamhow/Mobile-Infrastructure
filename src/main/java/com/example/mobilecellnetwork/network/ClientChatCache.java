package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.Chat;
import com.example.mobilecellnetwork.data.ChatType;
import com.example.mobilecellnetwork.data.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientChatCache {
    private static volatile List<Chat> chats = List.of();

    private ClientChatCache() {
    }

    public static void update(ChatListSyncPacket packet) {
        chats = List.copyOf(packet.chats());
    }

    public static List<Chat> chats() {
        return chats;
    }

    public static void clearSensitiveData() {
        chats = List.of();
    }

    public static Chat get(UUID id) {
        return chats.stream().filter(chat -> chat.id().equals(id)).findFirst().orElse(null);
    }

    public static void receive(UUID chatId, Message message) {
        append(chatId, message);
    }

    public static void addLocalMessage(UUID chatId, Message message) {
        append(chatId, message);
    }

    private static void append(UUID chatId, Message message) {
        List<Chat> updated = new ArrayList<>(chats);
        for (int index = 0; index < updated.size(); index++) {
            Chat existing = updated.get(index);
            if (existing.id().equals(chatId)) {
                Chat copy = new Chat(existing.id(), existing.type(), existing.displayName(),
                        existing.memberNumbers(), existing.messages(), existing.lastReadTimestamp(),
                        existing.pinnedToTop());
                copy.addMessage(message);
                updated.set(index, copy);
                chats = List.copyOf(updated);
                return;
            }
        }
        if (new UUID(0L, 0L).equals(chatId)) {
            Chat global = globalChat();
            global.addMessage(message);
            updated.add(global);
            chats = List.copyOf(updated);
        }
    }

    public static Chat globalChat() {
        return new Chat(new UUID(0L, 0L), ChatType.GLOBAL, "Global", List.of());
    }
}
