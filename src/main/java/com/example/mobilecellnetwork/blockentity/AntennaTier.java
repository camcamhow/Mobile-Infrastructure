package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;

public enum AntennaTier {
    COPPER(64, SignalTier.THREE_G, 64),
    IRON(128, SignalTier.FOUR_G_PLUS, 128),
    DIAMOND(256, SignalTier.FIVE_G_PLUS, 256);

    private final int defaultRange;
    private final SignalTier maxSignalTier;
    private final int meshRange;

    AntennaTier(int defaultRange, SignalTier maxSignalTier, int meshRange) {
        this.defaultRange = defaultRange;
        this.maxSignalTier = maxSignalTier;
        this.meshRange = meshRange;
    }

    public int range() {
        return switch (this) {
            case COPPER -> MobileCellNetworkConfig.COPPER_RANGE.get();
            case IRON -> MobileCellNetworkConfig.IRON_RANGE.get();
            case DIAMOND -> MobileCellNetworkConfig.DIAMOND_RANGE.get();
        };
    }

    public int defaultRange() {
        return defaultRange;
    }

    public SignalTier maxSignalTier() {
        return maxSignalTier;
    }

    public int meshRange() {
        return meshRange;
    }

    public int capacity() {
        return switch (this) {
            case COPPER -> MobileCellNetworkConfig.ANTENNA_CAPACITY_COPPER.get();
            case IRON -> MobileCellNetworkConfig.ANTENNA_CAPACITY_IRON.get();
            case DIAMOND -> MobileCellNetworkConfig.ANTENNA_CAPACITY_DIAMOND.get();
        };
    }

    public int maxReceivePerTick() {
        return switch (this) {
            case COPPER -> MobileCellNetworkConfig.ANTENNA_MAX_RECEIVE_COPPER.get();
            case IRON -> MobileCellNetworkConfig.ANTENNA_MAX_RECEIVE_IRON.get();
            case DIAMOND -> MobileCellNetworkConfig.ANTENNA_MAX_RECEIVE_DIAMOND.get();
        };
    }

    public int drawPerSecond() {
        return switch (this) {
            case COPPER -> MobileCellNetworkConfig.ANTENNA_DRAW_COPPER.get();
            case IRON -> MobileCellNetworkConfig.ANTENNA_DRAW_IRON.get();
            case DIAMOND -> MobileCellNetworkConfig.ANTENNA_DRAW_DIAMOND.get();
        };
    }
}
