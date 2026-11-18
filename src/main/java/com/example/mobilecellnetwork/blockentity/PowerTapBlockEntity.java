package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.infrastructure.PowerTapStorage;
import com.example.mobilecellnetwork.infrastructure.WiredDevice;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerTapBlockEntity extends BlockEntity {
    private final PowerTapStorage storage = new PowerTapStorage(this::setChanged);
    private final IEnergyStorage input = storage.port(false);
    private final IEnergyStorage output = storage.port(true);

    public PowerTapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_TAP.get(), pos, state);
    }

    public IEnergyStorage port(Direction side) { return side == Direction.UP ? output : input; }
    public int energy() { return storage.getEnergyStored(); }

    public void tick() {
        if (level == null || level.isClientSide) return;
        // Only pull from external supplies, never other taps or cellular equipment.
        for (Direction side : Direction.values()) {
            if (side == Direction.UP) continue;
            BlockPos neighbor = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbor)) continue;
            BlockEntity block = level.getBlockEntity(neighbor);
            if (block instanceof PowerTapBlockEntity || block instanceof WiredDevice) continue;
            IEnergyStorage supply = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, side.getOpposite());
            if (supply == null || !supply.canExtract()) continue;
            int requested = input.receiveEnergy(PowerTapStorage.RATE, true);
            int available = supply.extractEnergy(requested, true);
            if (available > 0) input.receiveEnergy(supply.extractEnergy(available, false), false);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storage.load(tag.getInt("energy"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy());
    }

}
