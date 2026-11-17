package com.example.mobilecellnetwork.infrastructure;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class Installation {
    public enum Status { READY, MISSING_MODULE, MISSING_WIRE, AWAITING_TEST }
    public static final int MAX_WIRE_LENGTH = 32;
    private boolean required;
    private boolean module;
    private boolean commissioned;
    private UUID owner;
    private BlockPos source;
    private Direction sourceFace = Direction.UP;
    private Vec3 sourcePoint;
    private Vec3 targetPoint;
    private List<Vec3> snaps = List.of();

    public void begin(UUID owner) {
        required = true;
        module = false;
        commissioned = false;
        this.owner = owner;
    }

    public boolean required() { return required; }
    public boolean hasModule() { return module; }
    public boolean commissioned() { return commissioned; }
    public UUID owner() { return owner; }
    public BlockPos source() { return source; }
    public Direction sourceFace() { return sourceFace; }
    public Vec3 sourcePoint() { return sourcePoint; }
    public Vec3 targetPoint() { return targetPoint; }
    public List<Vec3> snaps() { return snaps; }
    public boolean ready() { return !required || module && commissioned && source != null; }

    public Status status() {
        if (ready()) return Status.READY;
        if (!module) return Status.MISSING_MODULE;
        return source == null ? Status.MISSING_WIRE : Status.AWAITING_TEST;
    }

    public void installModule() {
        module = true;
        commissioned = false;
    }

    public void connect(BlockPos source, Direction face) {
        connect(source, face, null, List.of(), null);
    }

    public void connect(BlockPos source, Direction face, Vec3 from, List<Vec3> snaps, Vec3 to) {
        this.source = source.immutable();
        sourceFace = face;
        sourcePoint = from;
        targetPoint = to;
        this.snaps = List.copyOf(snaps);
        commissioned = false;
    }

    public void disconnect() {
        source = null;
        sourcePoint = null;
        targetPoint = null;
        snaps = List.of();
        commissioned = false;
    }

    public boolean commission(boolean powered) {
        if (!module || source == null || !powered) return false;
        commissioned = true;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("required", required);
        tag.putBoolean("module", module);
        tag.putBoolean("commissioned", commissioned);
        if (owner != null) tag.putUUID("owner", owner);
        if (source != null) {
            tag.putLong("source", source.asLong());
            tag.putInt("face", sourceFace.get3DDataValue());
            if (sourcePoint != null && targetPoint != null) {
                tag.put("sourcePoint", WirePath.savePoint(sourcePoint));
                tag.put("targetPoint", WirePath.savePoint(targetPoint));
                tag.put("snaps", WirePath.save(snaps));
            }
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        // Missing installation data identifies equipment placed before technician gameplay.
        required = tag.getBoolean("required");
        module = tag.getBoolean("module");
        commissioned = tag.getBoolean("commissioned");
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        source = tag.contains("source") ? BlockPos.of(tag.getLong("source")) : null;
        sourceFace = Direction.from3DDataValue(tag.getInt("face"));
        sourcePoint = tag.contains("sourcePoint") ? WirePath.loadPoint(tag.getCompound("sourcePoint")) : null;
        targetPoint = tag.contains("targetPoint") ? WirePath.loadPoint(tag.getCompound("targetPoint")) : null;
        snaps = WirePath.load(tag, "snaps");
    }
}
