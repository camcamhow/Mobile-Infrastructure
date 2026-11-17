package com.example.mobilecellnetwork.infrastructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerWire {
    public static final int TRANSFER_PER_TICK = 256;

    private PowerWire() {}

    public static boolean canConnect(BlockPos source, BlockPos target) {
        return !source.equals(target)
                && source.distSqr(target) <= Installation.MAX_WIRE_LENGTH * Installation.MAX_WIRE_LENGTH;
    }

    public static int cableUnits(BlockPos source, BlockPos target) {
        return (int) Math.ceil(Math.sqrt(source.distSqr(target)));
    }

    public static boolean canConnect(Installation installation, BlockPos target) {
        return installation.source() != null && canConnect(installation.source(), target)
                && (installation.sourcePoint() == null || installation.targetPoint() == null
                || WirePath.valid(installation.sourcePoint(), installation.snaps(), installation.targetPoint()));
    }

    public static void tick(BlockEntity target, Installation installation, IEnergyStorage storage) {
        var level = target.getLevel();
        var source = installation.source();
        if (level == null || level.isClientSide || source == null || !level.hasChunkAt(source)
                || !canConnect(installation, target.getBlockPos())) {
            return;
        }
        IEnergyStorage supply = level.getCapability(Capabilities.EnergyStorage.BLOCK, source,
                installation.sourceFace());
        if (supply == null || !supply.canExtract()) return;
        transfer(supply, storage);
    }

    public static int transfer(IEnergyStorage supply, IEnergyStorage target) {
        int requested = target.receiveEnergy(TRANSFER_PER_TICK, true);
        int available = supply.extractEnergy(requested, true);
        if (available <= 0) return 0;
        return target.receiveEnergy(supply.extractEnergy(available, false), false);
    }
}
