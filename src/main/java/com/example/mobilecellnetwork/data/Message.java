package com.example.mobilecellnetwork.data;

import java.util.UUID;

public record Message(UUID id, String senderNumber, String body, long timestamp, boolean read) {
    public Message {
        id = id == null ? UUID.randomUUID() : id;
        senderNumber = senderNumber == null ? "" : senderNumber;
        body = body == null ? "" : body;
    }

    public Message(String senderNumber, String body, long timestamp, boolean read) {
        this(UUID.randomUUID(), senderNumber, body, timestamp, read);
    }

    public Message markRead() {
        return new Message(id, senderNumber, body, timestamp, true);
    }
}
