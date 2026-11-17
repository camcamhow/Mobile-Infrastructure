package com.example.mobilecellnetwork.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Chat {
    private final UUID id;
    private final ChatType type;
    private String displayName;
    private final List<String> memberNumbers;
    private final List<Message> messages;
    private long lastReadTimestamp;
    private boolean pinnedToTop;

    public Chat(UUID id, ChatType type, String displayName, List<String> memberNumbers,
                List<Message> messages, long lastReadTimestamp, boolean pinnedToTop) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.type = type == null ? ChatType.DIRECT : type;
        this.displayName = displayName == null ? "" : displayName;
        this.memberNumbers = new ArrayList<>(memberNumbers == null ? List.of() : memberNumbers);
        this.messages = new ArrayList<>(messages == null ? List.of() : messages);
        this.lastReadTimestamp = lastReadTimestamp;
        this.pinnedToTop = pinnedToTop;
    }

    public Chat(UUID id, ChatType type, String displayName, List<String> memberNumbers) {
        this(id, type, displayName, memberNumbers, List.of(), 0L, false);
    }

    public UUID id() { return id; }
    public ChatType type() { return type; }
    public String displayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName == null ? "" : displayName; }
    public List<String> memberNumbers() { return memberNumbers; }
    public List<Message> messages() { return messages; }
    public long lastReadTimestamp() { return lastReadTimestamp; }
    public void setLastReadTimestamp(long timestamp) { lastReadTimestamp = timestamp; }
    public boolean pinnedToTop() { return pinnedToTop; }
    public void setPinnedToTop(boolean pinned) { pinnedToTop = pinned; }

    public long latestTimestamp() {
        return messages.stream().mapToLong(Message::timestamp).max().orElse(0L);
    }

    public int unreadCount() {
        return (int) messages.stream().filter(message -> !message.read()
                && message.timestamp() > lastReadTimestamp && !"system".equals(message.senderNumber())).count();
    }

    public void addMessage(Message message) {
        messages.add(message);
    }
}
