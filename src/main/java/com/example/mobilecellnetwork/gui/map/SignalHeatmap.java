package com.example.mobilecellnetwork.gui.map;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.LineOfSight;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SignalHeatmap {
    private static final int COLUMNS = 48;
    private static final int ROWS = 56;
    private static final long CACHE_DURATION_MILLIS = 1_000L;
    private static final long DRAG_RECOMPUTE_INTERVAL_MILLIS = 250L;
    private static final int MAX_CACHED_TILES = 10_000;
    private static final Map<Long, CachedStrength> TILE_STRENGTHS =
            new LinkedHashMap<>(256, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, CachedStrength> eldest) {
                    return size() > MAX_CACHED_TILES;
                }
            };
    private static TileColors lastTileColors;
    private static long antennaRevision = Long.MIN_VALUE;
    private static long lastFullRecomputeTime;
    private static String cacheDimension = "";
    private static UUID cachePlayerUuid;
    private static boolean dragging;

    private SignalHeatmap() {
    }

    public static void setDragging(boolean value) {
        dragging = value;
    }

    public static int colorForStrength(double strength) {
        if (!Double.isFinite(strength)) {
            return 0;
        }
        double boundedStrength = Math.clamp(strength, 0.0D, 1.0D);
        if (boundedStrength > 0.75D) {
            return 0x8000FF00;
        }
        if (boundedStrength >= 0.50D) {
            return 0x80FFFF00;
        }
        if (boundedStrength >= 0.25D) {
            return 0x80FF8000;
        }
        if (boundedStrength >= 0.08D) {
            return 0x80FF0000;
        }
        return 0;
    }

    public static double getStrengthAt(ClientLevel level, int worldX, int worldZ, UUID playerUuid) {
        String dimension = level.dimension().location().toString();
        List<ClientSignalCache.AntennaInfo> antennas = ClientSignalCache.antennas(playerUuid, dimension);
        if (antennas.isEmpty()) {
            return 0.0D;
        }
        BlockPos sample = new BlockPos(worldX, level.getMinBuildHeight(), worldZ);
        if (!level.hasChunkAt(sample)) {
            return 0.0D;
        }
        int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                worldX, worldZ);
        if (surfaceY <= level.getMinBuildHeight()) {
            return 0.0D;
        }
        Vec3 samplePosition = new Vec3(worldX + 0.5D, surfaceY, worldZ + 0.5D);
        double bestStrength = 0.0D;
        double maxRangeMultiplier = MobileCellNetworkConfig.INVERSE_MAX_RANGE_MULTIPLIER.get();
        for (ClientSignalCache.AntennaInfo antenna : antennas) {
            if (!Double.isFinite(antenna.range()) || antenna.range() <= 0.0D) {
                continue;
            }
            Vec3 antennaPosition = new Vec3(antenna.x() + 0.5D, antenna.y() + 0.5D, antenna.z() + 0.5D);
            double distance = samplePosition.distanceTo(antennaPosition);
            if (distance > antenna.range() * maxRangeMultiplier) {
                continue;
            }
            double ratio = distance / antenna.range();
            double baseStrength = switch (MobileCellNetworkConfig.SIGNAL_FALLOFF_MODE.get()) {
                case INVERSE -> 1.0D / (1.0D + MobileCellNetworkConfig.INVERSE_FALLOFF_K.get() * ratio);
                case LINEAR -> Math.max(0.0D, 1.0D - ratio);
                case QUADRATIC -> Math.max(0.0D, 1.0D - ratio * ratio);
                case STEPPED -> Math.floor(Math.max(0.0D, 1.0D - ratio) * 10.0D) / 10.0D;
            };
            if (baseStrength <= bestStrength) {
                continue;
            }
            double attenuation = LineOfSight.compute(level, samplePosition, antennaPosition).attenuation();
            double strength = Math.max(0.0D,
                    baseStrength - attenuation * MobileCellNetworkConfig.LOS_WEIGHT.get());
            bestStrength = Math.max(bestStrength, Math.min(1.0D, strength));
        }
        return bestStrength;
    }

    public static void renderOverlay(GuiGraphics graphics, ClientLevel level,
                                     int mapX, int mapY, int mapW, int mapH,
                                     int centerX, int centerZ, int pixelsPerBlock,
                                     UUID playerUuid) {
        if (mapW <= 0 || mapH <= 0 || pixelsPerBlock <= 0 || playerUuid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        String dimension = level.dimension().location().toString();
        long currentAntennaRevision = ClientSignalCache.antennaRevision();
        if (!dimension.equals(cacheDimension) || !playerUuid.equals(cachePlayerUuid)
                || antennaRevision != currentAntennaRevision) {
            TILE_STRENGTHS.clear();
            lastTileColors = null;
            cacheDimension = dimension;
            cachePlayerUuid = playerUuid;
            antennaRevision = currentAntennaRevision;
        }
        TileColors tileColors = lastTileColors;
        int shiftX = 0;
        int shiftZ = 0;
        if (dragging && tileColors != null && tileColors.mapWidth() == mapW && tileColors.mapHeight() == mapH
                && tileColors.blocksPerTile() == pixelsPerBlock
                && now - lastFullRecomputeTime < DRAG_RECOMPUTE_INTERVAL_MILLIS) {
            int tileWidthForShift = Math.max(1, mapW / COLUMNS);
            int tileHeightForShift = Math.max(1, mapH / ROWS);
            shiftX = (int) Math.round((tileColors.centerX() - centerX)
                    / (double) pixelsPerBlock * tileWidthForShift);
            shiftZ = (int) Math.round((tileColors.centerZ() - centerZ)
                    / (double) pixelsPerBlock * tileHeightForShift);
        } else {
            tileColors = computeColors(level, mapW, mapH, centerX, centerZ, pixelsPerBlock, playerUuid, now);
            lastTileColors = tileColors;
            lastFullRecomputeTime = now;
        }
        int tileWidth = Math.max(1, mapW / COLUMNS);
        int tileHeight = Math.max(1, mapH / ROWS);
        graphics.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        for (int row = 0; row < ROWS; row++) {
            int y = mapY + row * tileHeight + shiftZ;
            for (int column = 0; column < COLUMNS; column++) {
                int color = tileColors.colors()[row * COLUMNS + column];
                if (color == 0) {
                    continue;
                }
                int x = mapX + column * tileWidth + shiftX;
                graphics.fill(x, y, x + tileWidth, y + tileHeight, color);
            }
        }
        graphics.disableScissor();
    }

    private static TileColors computeColors(ClientLevel level, int mapW, int mapH,
                                            int centerX, int centerZ, int blocksPerTile,
                                            UUID playerUuid, long now) {
        int[] colors = new int[COLUMNS * ROWS];
        int startX = centerX - COLUMNS / 2 * blocksPerTile;
        int startZ = centerZ - ROWS / 2 * blocksPerTile;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int worldX = startX + column * blocksPerTile + blocksPerTile / 2;
                int worldZ = startZ + row * blocksPerTile + blocksPerTile / 2;
                long key = tileKey(worldX, worldZ);
                CachedStrength cached = TILE_STRENGTHS.get(key);
                if (cached == null || cached.antennaRevision() != antennaRevision
                        || now - cached.cachedAtMillis() >= CACHE_DURATION_MILLIS) {
                    cached = new CachedStrength(getStrengthAt(level, worldX, worldZ, playerUuid),
                            now, antennaRevision);
                    TILE_STRENGTHS.put(key, cached);
                }
                colors[row * COLUMNS + column] = colorForStrength(cached.strength());
            }
        }
        return new TileColors(colors, centerX, centerZ, blocksPerTile, mapW, mapH);
    }

    private static long tileKey(int worldX, int worldZ) {
        return ((long) worldX << 32) ^ (worldZ & 0xFFFF_FFFFL);
    }

    private record CachedStrength(double strength, long cachedAtMillis, long antennaRevision) {
    }

    private record TileColors(int[] colors, int centerX, int centerZ, int blocksPerTile,
                              int mapWidth, int mapHeight) {
    }
}
