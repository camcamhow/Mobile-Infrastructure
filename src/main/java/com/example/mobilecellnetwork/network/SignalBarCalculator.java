package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;

public final class SignalBarCalculator {
    private SignalBarCalculator() {
    }

    public static int computeBars(double finalStrength, SignalTier tier) {
        if (tier == SignalTier.NO_SERVICE || finalStrength <= 0.0D) {
            return 0;
        }
        var tierThresholds = CoverageEngine.configuredSignalThresholds();
        double lowerBound = tierThresholds.getOrDefault(tier.name(), 0.0D);
        double upperBound = tier == SignalTier.FIVE_G_PLUS
                ? 1.0D
                : tierThresholds.getOrDefault(SignalTier.values()[tier.ordinal() - 1].name(), lowerBound);
        double tierWidth = upperBound - lowerBound;
        if (tierWidth <= 0.0D || finalStrength >= upperBound) {
            return 4;
        }
        double tierStrength = Math.clamp((finalStrength - lowerBound) / tierWidth, 0.0D, 1.0D);
        if (tierStrength >= MobileCellNetworkConfig.SIGNAL_BARS_4_THRESHOLD.get()) {
            return 4;
        }
        if (tierStrength >= MobileCellNetworkConfig.SIGNAL_BARS_3_THRESHOLD.get()) {
            return 3;
        }
        if (tierStrength >= MobileCellNetworkConfig.SIGNAL_BARS_2_THRESHOLD.get()) {
            return 2;
        }
        if (tierStrength >= MobileCellNetworkConfig.SIGNAL_BARS_1_THRESHOLD.get()) {
            return 1;
        }
        return 0;
    }
}
