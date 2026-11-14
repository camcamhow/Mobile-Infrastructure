package com.example.mobilecellnetwork.network;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ClientSignalCache {
    private static volatile SignalTier tier = SignalTier.NO_SERVICE;
    private static volatile int bars;
    private static volatile double strength;
    private static volatile String networkName = "";
    private static volatile String activeNetworkId = "";
    private static volatile boolean roaming;
    private static volatile long roamingFeesThisSession;
    private static volatile int charge = -1;
    private static volatile UUID antennaCachePlayer;
    private static volatile String antennaCacheDimension = "";
    private static volatile List<AntennaInfo> antennas = List.of();
    private static volatile long antennaRevision;

    public static void setCharge(int value) {
        charge = value;
    }

    public static int charge() {
        return charge;
    }

    private ClientSignalCache() {
    }

    public static void update(SignalUpdatePacket packet) {
        tier = packet.signalTier();
        bars = Math.clamp(packet.signalBars(), 0, 4);
        strength = packet.finalStrength();
        networkName = packet.activeNetworkName();
        activeNetworkId = packet.activeNetworkId();
        roaming = packet.roaming();
        roamingFeesThisSession = Math.max(0L, packet.roamingFeesThisSession());
    }

    public static SignalTier tier() {
        return tier;
    }

    public static int bars() {
        return bars;
    }

    public static double strength() {
        return strength;
    }

    public static String networkName() {
        return networkName;
    }

    public static String activeNetworkId() {
        return activeNetworkId;
    }

    public static void clear() {
        tier = SignalTier.NO_SERVICE;
        bars = 0;
        strength = 0.0D;
        networkName = "";
        activeNetworkId = "";
        roaming = false;
        roamingFeesThisSession = 0L;
    }

    public static boolean roaming() {
        return roaming;
    }

    public static long roamingFeesThisSession() {
        return roamingFeesThisSession;
    }

    public static void updateAntennas(AntennaCachePacket packet) {
        List<AntennaInfo> updated = packet.antennas().stream()
                .map(antenna -> new AntennaInfo(antenna.x(), antenna.y(), antenna.z(),
                        antenna.range(), antenna.maxSignalTier()))
                .toList();
        if (!Objects.equals(antennaCachePlayer, packet.playerUuid())
                || !antennaCacheDimension.equals(packet.dimension())
                || !antennas.equals(updated)) {
            antennaRevision++;
        }
        antennaCachePlayer = packet.playerUuid();
        antennaCacheDimension = packet.dimension();
        antennas = updated;
    }

    public static List<AntennaInfo> antennas(UUID playerUuid, String dimension) {
        if (!Objects.equals(antennaCachePlayer, playerUuid) || !antennaCacheDimension.equals(dimension)) {
            return List.of();
        }
        return antennas;
    }

    public static long antennaRevision() {
        return antennaRevision;
    }

    public record AntennaInfo(int x, int y, int z, double range, String maxSignalTier) {
    }
}
