package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class LineOfSight {
    private LineOfSight() {
    }

    public static double computeAttenuation(ServerLevel level, Vec3 from, Vec3 to) {
        return compute(level, from, to).attenuation();
    }

    public static Result compute(Level level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        double interval = MobileCellNetworkConfig.LOS_SAMPLE_INTERVAL.get();
        if (distance <= interval) {
            return new Result(0.0D, 0);
        }

        int segmentCount = (int) Math.ceil(distance / interval);
        int totalInteriorSamples = Math.max(0, segmentCount - 1);
        int maxSamples = MobileCellNetworkConfig.LOS_MAX_SAMPLES.get();
        int sampleCount = Math.min(totalInteriorSamples, maxSamples);
        if (sampleCount == 0) {
            return new Result(0.0D, 0);
        }

        Vec3 direction = delta.scale(1.0D / distance);
        double sum = 0.0D;
        double sampleStep = distance / (sampleCount + 1);
        for (int sample = 1; sample <= sampleCount; sample++) {
            double distanceAlong = sample * sampleStep;
            Vec3 point = from.add(direction.scale(distanceAlong));
            Vec3 halfStep = direction.scale(sampleStep * 0.5D);
            Vec3 rayStart = point.subtract(halfStep);
            Vec3 rayEnd = point.add(halfStep);
            BlockHitResult hit = level.clip(new ClipContext(rayStart, rayEnd,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            BlockPos pos = hit.getType() == BlockHitResult.Type.BLOCK
                    ? hit.getBlockPos() : BlockPos.containing(point);
            BlockState state = level.getBlockState(pos);
            sum += SignalAttenuation.getAttenuation(state);
        }
        if (sampleCount < totalInteriorSamples) {
            sum *= (double) totalInteriorSamples / sampleCount;
        }
        return new Result(sum, sampleCount);
    }

    public record Result(double attenuation, int sampleCount) {
    }
}
