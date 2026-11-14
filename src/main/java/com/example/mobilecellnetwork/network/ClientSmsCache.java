package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.capabilities.PhoneData;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientSmsCache {
    private static volatile List<PhoneData.Message> inbox = List.of();
    private static volatile List<PhoneData.Message> sent = List.of();
    private static volatile List<SmsDataSyncPacket.VoicemailEntry> voicemail = List.of();
    private static volatile int missedCallCount;
    private static final CopyOnWriteArrayList<ToastEntry> TOASTS = new CopyOnWriteArrayList<>();

    private ClientSmsCache() {
    }

    public static void update(SmsDataSyncPacket packet) {
        inbox = packet.inbox().stream().map(SmsDataSyncPacket.MessageEntry::toMessage).toList();
        sent = packet.sent().stream().map(SmsDataSyncPacket.MessageEntry::toMessage).toList();
        voicemail = List.copyOf(packet.voicemail());
        missedCallCount = packet.missedCallCount();
    }

    public static void receive(SmsDataSyncPacket.MessageEntry entry) {
        java.util.ArrayList<PhoneData.Message> messages = new java.util.ArrayList<>(inbox);
        messages.add(entry.toMessage());
        inbox = List.copyOf(messages);
    }

    public static void sent(SmsDataSyncPacket.MessageEntry entry) {
        java.util.ArrayList<PhoneData.Message> messages = new java.util.ArrayList<>(sent);
        messages.add(entry.toMessage());
        sent = List.copyOf(messages);
    }

    public static List<PhoneData.Message> inbox() { return inbox; }
    public static List<PhoneData.Message> sent() { return sent; }
    public static List<SmsDataSyncPacket.VoicemailEntry> voicemail() { return voicemail; }
    public static int missedCallCount() { return missedCallCount; }

    public static void clearSensitiveData() {
        inbox = List.of();
        sent = List.of();
        voicemail = List.of();
        missedCallCount = 0;
        TOASTS.clear();
    }

    public static void addToast(String title, String message) {
        TOASTS.add(new ToastEntry(title, message, System.currentTimeMillis()));
    }

    public static List<ToastEntry> activeToasts(long now) {
        TOASTS.removeIf(entry -> now - entry.createdAt() >= 5000L);
        return List.copyOf(TOASTS);
    }

    public record ToastEntry(String title, String message, long createdAt) {
    }
}
