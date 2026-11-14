package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.CallLogEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientCallLogCache {
    private static volatile List<CallLogEntry> entries = List.of();

    private ClientCallLogCache() {
    }

    public static void update(CallLogSyncPacket packet) {
        entries = List.copyOf(packet.entries());
    }

    public static void add(CallLogEntry entry) {
        List<CallLogEntry> updated = new ArrayList<>(entries);
        updated.add(entry);
        while (updated.size() > 200) {
            updated.remove(0);
        }
        entries = List.copyOf(updated);
    }

    public static void remove(java.util.UUID id) {
        entries = entries.stream().filter(entry -> !entry.id().equals(id)).toList();
    }

    public static List<CallLogEntry> entriesNewestFirst() {
        return entries.stream().sorted(Comparator.comparingLong(CallLogEntry::timestamp).reversed()).toList();
    }

    public static void clearSensitiveData() {
        entries = List.of();
    }
}
