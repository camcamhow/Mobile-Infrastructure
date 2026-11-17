package com.example.mobilecellnetwork.data;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record CallLogEntry(UUID id, String otherNumber, String otherName, CallDirection direction,
                           long timestamp, int durationSeconds) {
    public CallLogEntry {
        otherNumber = otherNumber == null ? "" : otherNumber;
    }

    public static CallLogEntry create(CallDirection direction, String otherNumber, String otherName,
                                      int durationSeconds) {
        return new CallLogEntry(UUID.randomUUID(), otherNumber, otherName, direction,
                System.currentTimeMillis(), Math.max(0, durationSeconds));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("number", otherNumber);
        if (otherName != null) {
            tag.putString("name", otherName);
        }
        tag.putString("direction", direction.name());
        tag.putLong("timestamp", timestamp);
        tag.putInt("duration", durationSeconds);
        return tag;
    }

    public static CallLogEntry fromTag(CompoundTag tag) {
        CallDirection direction;
        try {
            direction = CallDirection.valueOf(tag.getString("direction"));
        } catch (IllegalArgumentException exception) {
            direction = CallDirection.MISSED;
        }
        return new CallLogEntry(tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID(),
                tag.getString("number"), tag.contains("name") ? tag.getString("name") : null,
                direction, tag.getLong("timestamp"), tag.getInt("duration"));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(id);
        buffer.writeUtf(otherNumber, 32);
        buffer.writeBoolean(otherName != null);
        if (otherName != null) {
            buffer.writeUtf(otherName, 64);
        }
        buffer.writeEnum(direction);
        buffer.writeLong(timestamp);
        buffer.writeVarInt(durationSeconds);
    }

    public static CallLogEntry read(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        String number = buffer.readUtf(32);
        String name = buffer.readBoolean() ? buffer.readUtf(64) : null;
        CallDirection direction = buffer.readEnum(CallDirection.class);
        return new CallLogEntry(id, number, name, direction, buffer.readLong(), buffer.readVarInt());
    }
}
