package com.example.mobilecellnetwork.infrastructure;

import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public record MeterReading(Mode mode, int energy, int capacity, int signal, String tier,
                           String status, int output, long sampledAt) {
    public static final String DATA_KEY = "technicianMeter";
    public enum Mode { IDLE, SURVEY, TOWER, BOOSTER, TAP, POWER, UNAVAILABLE }

    public static MeterReading idle() {
        return new MeterReading(Mode.IDLE, 0, 0, -1, "", "AIM", -1, -1);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", mode.name());
        tag.putInt("energy", energy);
        tag.putInt("capacity", capacity);
        tag.putInt("signal", signal);
        tag.putString("tier", tier);
        tag.putString("status", status);
        tag.putInt("output", output);
        tag.putLong("sampledAt", sampledAt);
        return tag;
    }

    public static MeterReading load(CompoundTag tag) {
        if (tag.isEmpty()) return idle();
        Mode mode = java.util.Arrays.stream(Mode.values())
                .filter(value -> value.name().equals(tag.getString("mode"))).findFirst().orElse(Mode.UNAVAILABLE);
        if (!mode.name().equals(tag.getString("mode"))) {
            return new MeterReading(Mode.UNAVAILABLE, 0, 0, -1, "", "INVALID_READING", -1, -1);
        }
        int capacity = Math.max(0, tag.getInt("capacity"));
        return new MeterReading(mode, Math.clamp(tag.getInt("energy"), 0, capacity), capacity,
                Math.clamp(tag.getInt("signal"), -1, 100), tag.getString("tier"), tag.getString("status"),
                Math.max(-1, tag.getInt("output")), tag.getLong("sampledAt"));
    }

    public boolean stale(long now) {
        return sampledAt < 0 || now < sampledAt || now - sampledAt > 40;
    }

    public List<Component> lines(long now) {
        if (mode == Mode.IDLE) {
            return List.of(text("title"), text("idle"), text("aim"), text("use"), text("units"), text("radio"));
        }
        return List.of(
                text("mode." + mode.name().toLowerCase(Locale.ROOT)),
                capacity > 0 ? text("energy", energy, capacity) : text("no_energy"),
                signal >= 0 ? text("signal", signal,
                        com.example.mobilecellnetwork.network.SignalTier.displayName(tier)) : text("no_signal"),
                text("status." + status.toLowerCase(Locale.ROOT)),
                output >= 0 ? text("output", output) : text("units"),
                text(stale(now) ? "stale" : "live"));
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("meter.mobilecellnetwork." + key, args);
    }
}
