package com.example.mobilecellnetwork.infrastructure;

import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerTapStorage extends EnergyStorage {
    public static final int CAPACITY = 100_000;
    public static final int RATE = 1024;
    private final Runnable changed;

    public PowerTapStorage(Runnable changed) {
        super(CAPACITY, RATE, RATE);
        this.changed = changed;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int accepted = super.receiveEnergy(Math.max(0, amount), simulate);
        if (!simulate && accepted > 0) changed.run();
        return accepted;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = super.extractEnergy(Math.max(0, amount), simulate);
        if (!simulate && extracted > 0) changed.run();
        return extracted;
    }

    public void load(int stored) {
        energy = Math.clamp(stored, 0, CAPACITY);
    }

    public IEnergyStorage port(boolean extracting) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                return extracting ? 0 : PowerTapStorage.this.receiveEnergy(amount, simulate);
            }
            @Override public int extractEnergy(int amount, boolean simulate) {
                return extracting ? PowerTapStorage.this.extractEnergy(amount, simulate) : 0;
            }
            @Override public int getEnergyStored() { return PowerTapStorage.this.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return CAPACITY; }
            @Override public boolean canExtract() { return extracting; }
            @Override public boolean canReceive() { return !extracting; }
        };
    }
}
