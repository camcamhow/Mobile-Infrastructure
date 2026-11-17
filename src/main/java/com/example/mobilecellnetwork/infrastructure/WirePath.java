package com.example.mobilecellnetwork.infrastructure;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

public final class WirePath {
    public static final int MAX_SNAPS = 16;
    private WirePath() {}

    public static CompoundTag savePoint(Vec3 point) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", point.x);
        tag.putDouble("y", point.y);
        tag.putDouble("z", point.z);
        return tag;
    }

    public static Vec3 loadPoint(CompoundTag tag) {
        return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
    }

    public static ListTag save(List<Vec3> points) {
        ListTag tag = new ListTag();
        for (Vec3 point : points) tag.add(savePoint(point));
        return tag;
    }

    public static List<Vec3> load(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) points.add(loadPoint(list.getCompound(i)));
        return List.copyOf(points);
    }

    public static double length(Vec3 source, List<Vec3> snaps, Vec3 target) {
        double length = 0;
        Vec3 previous = source;
        for (Vec3 point : snaps) {
            length += previous.distanceTo(point);
            previous = point;
        }
        return length + previous.distanceTo(target);
    }

    public static boolean valid(Vec3 source, List<Vec3> snaps, Vec3 target) {
        double length = length(source, snaps, target);
        return snaps.size() <= MAX_SNAPS && Double.isFinite(length)
                && length > 0.001 && length <= Installation.MAX_WIRE_LENGTH;
    }
}
