package com.example.mobilecellnetwork.infrastructure;

public final class BoosterReception {
    private BoosterReception() {}

    public static double strength(double upstream, double distance, double range, double obstruction) {
        if (upstream <= 0 || range <= 0 || distance >= range) return 0;
        return Math.clamp(1 - distance / range - obstruction, 0, 1);
    }
}
