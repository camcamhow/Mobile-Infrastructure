package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class SignalBoosterBlockEntity extends BlockEntity implements IEnergyStorage {
    public static final int RANGE = 32;
    private static final int CAPACITY = 10000;
    private static final Set<SignalBoosterBlockEntity> LOADED = new HashSet<>();
    private int energy;
    private long poweredTick = Long.MIN_VALUE;
    private boolean displayPowered;
    private boolean lastDisplayPowered;
    private int displayIndicators;
    private int lastDisplayIndicators = -1;

    public SignalBoosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIGNAL_BOOSTER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) LOADED.add(this);
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        LOADED.remove(this);
        super.onChunkUnloaded();
    }

    public static List<SignalBoosterBlockEntity> nearby(Level level, Vec3 point) {
        return LOADED.stream().filter(booster -> booster.level == level && booster.isPowered()
                && booster.center().distanceToSqr(point) < RANGE * RANGE).toList();
    }

    public Vec3 center() {
        return Vec3.atCenterOf(worldPosition);
    }

    public void tick() {
        if (level != null && !level.isClientSide && !InfrastructurePower.bypassed(level) && energy > 0) {
            energy--;
            poweredTick = level.getGameTime();
            setChanged();
        }
        syncDisplay();
    }

    public boolean isPowered() {
        return level != null && (level.isClientSide ? displayPowered : (InfrastructurePower.bypassed(level) || energy > 0
                || poweredTick == level.getGameTime()));
    }

    private void syncDisplay() {
        if (level == null || level.isClientSide) return;
        boolean powered = isPowered();
        int indicators = indicators();
        if (powered != lastDisplayPowered || indicators != lastDisplayIndicators) {
            lastDisplayPowered = powered;
            lastDisplayIndicators = indicators;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private int indicators() {
        if (level == null || level.isClientSide || !isPowered()) return 0;
        boolean bypass = InfrastructurePower.bypassed(level);
        return 1 | (bypass ? 2 : 0) | (!bypass && energy < CAPACITY / 5 ? 4 : 0);
    }

    public boolean isBypassIndicator() { return (displayIndicators & 2) != 0; }
    public boolean isLowPowerIndicator() { return (displayIndicators & 4) != 0; }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("powered", isPowered());
        tag.putInt("indicators", indicators());
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        displayPowered = tag.getBoolean("powered");
        displayIndicators = tag.getInt("indicators");
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                                        HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), CAPACITY - energy);
        if (!simulate && accepted > 0) {
            energy += accepted;
            setChanged();
            syncDisplay();
        }
        return accepted;
    }

    @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
    @Override public int getEnergyStored() { return energy; }
    @Override public int getMaxEnergyStored() { return CAPACITY; }
    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return true; }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = Math.clamp(tag.getInt("energy"), 0, CAPACITY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
    }
}
