package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.data.MapPointOfInterest;
import com.example.mobilecellnetwork.network.MapInteractionPacket;
import com.example.mobilecellnetwork.network.MapLoadCache;
import com.example.mobilecellnetwork.network.MapOpenPacket;
import com.example.mobilecellnetwork.network.MapAntennaCacheRequestPacket;
import com.example.mobilecellnetwork.network.MapPointActionPacket;
import com.example.mobilecellnetwork.network.MapPointCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.gui.map.MapPalette;
import com.example.mobilecellnetwork.gui.map.SignalHeatmap;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class MapScreen extends Screen implements PhoneNavigableScreen {
    private static final int COLUMNS = 48;
    private static final int ROWS = 56;
    private static final int CELL_SIZE = 4;
    private static final int GRID_TOP = 82;
    private final PhoneMenu menu;
    private int resolution = 1;
    private UUID selectedPoint;
    private int mapCenterX = Integer.MIN_VALUE;
    private int mapCenterZ = Integer.MIN_VALUE;
    private int[] cachedTileColors;
    private int shiftPxX;
    private int shiftPxY;
    private long lastCacheTimeMillis = Long.MIN_VALUE;
    private int cachedCenterX = Integer.MIN_VALUE;
    private int cachedCenterZ = Integer.MIN_VALUE;
    private int cachedResolution = -1;
    private String cachedDimension = "";
    private boolean draggingMap;
    private double dragStartX;
    private double dragStartY;
    private int dragCenterX;
    private int dragCenterZ;
    private boolean signalOverlayEnabled;

    public MapScreen(PhoneMenu menu) {
        super(Component.literal("Map"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        MapLoadCache.begin();
        SignalHeatmap.setDragging(false);
        PacketDistributor.sendToServer(new MapOpenPacket());
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        int controlsY = top + PhoneFrame.getScreenH() - 47;
        addRenderableWidget(new PhoneActionButton(left + 152, top + 58, 20, 18, "Zoom out",
                button -> setResolution(resolution - 1)));
        addRenderableWidget(new PhoneActionButton(left + 176, top + 58, 20, 18, "Zoom in",
                button -> setResolution(resolution + 1)));
        addRenderableWidget(new PhoneActionButton(left + 8, controlsY, 42, 18, "Points",
                button -> focusNextPoint()));
        PhoneActionButton shareButton = new PhoneActionButton(left + 54, controlsY, 50, 18, "Share",
                button -> shareSelected());
        addRenderableWidget(shareButton);
        PhoneActionButton deleteButton = new PhoneActionButton(left + 108, controlsY, 48, 18, "Delete",
                button -> deleteSelected());
        addRenderableWidget(deleteButton);
        addRenderableWidget(new PhoneActionButton(left + 160, controlsY, 40, 18, "Signal",
                button -> toggleSignalOverlay()));
    }

    private void toggleSignalOverlay() {
        signalOverlayEnabled = !signalOverlayEnabled;
        if (signalOverlayEnabled) {
            requestAntennaCache();
        }
    }

    private void requestAntennaCache() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ensureMapCenter();
        PacketDistributor.sendToServer(new MapAntennaCacheRequestPacket(
                mapCenterX, mapCenterZ, resolution));
    }

    private void setResolution(int value) {
        int next = Math.clamp(value, 1, 4);
        if (next != resolution) {
            resolution = next;
            PacketDistributor.sendToServer(new MapInteractionPacket(resolution));
            if (signalOverlayEnabled) {
                requestAntennaCache();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No service gating: show overlay if no active plan and screen is not exempt
            if (ClientPhoneCache.planId().isBlank()) {
                NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                        PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(), PhoneFrame.getScreenH(), "Map", font);
                return;
            }
            PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(), left + 4, top + 62,
                PhoneFrame.getScreenW() - 8, PhoneFrame.getScreenH() - 105);
        PhoneFrame.renderHeader(graphics, this, "Map", true);
        var level = minecraft.level;
        var player = minecraft.player;
        PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(), left + 8, top + 58,
                138, 20, 8, PhoneColors.bgSecondary(ClientPhoneCache.phoneStyle()));
        graphics.drawString(font, resolution + " block" + (resolution == 1 ? "" : "s") + " / tile",
                left + 14, top + 64, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()), true);
        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        if (ClientSignalCache.tier() == com.example.mobilecellnetwork.network.SignalTier.NO_SERVICE) {
            graphics.drawCenteredString(font, "No Service - map cannot load",
                    left + PhoneFrame.getScreenW() / 2, top + 110, PhoneColors.ERROR_TEXT);
        } else if (!MapLoadCache.ready() && MapLoadCache.message().isEmpty()) {
            graphics.drawCenteredString(font, "Loading map...",
                    left + PhoneFrame.getScreenW() / 2, top + 110, PhoneColors.SECONDARY_TEXT);
        } else if (!MapLoadCache.message().isEmpty()) {
            graphics.drawCenteredString(font, fit(MapLoadCache.message(), PhoneFrame.getScreenW() - 20),
                    left + PhoneFrame.getScreenW() / 2, top + 110, PhoneColors.ERROR_TEXT);
        } else if (level != null && player != null) {
            BlockPos center = player.blockPosition();
            if (mapCenterX == Integer.MIN_VALUE) {
                mapCenterX = center.getX();
                mapCenterZ = center.getZ();
            }
            center = new BlockPos(mapCenterX, center.getY(), mapCenterZ);
            graphics.drawString(font, "X " + center.getX() + " Z " + center.getZ(),
                    left + 10, top + 72, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()), true);
            renderGrid(graphics, level, center);
            if (signalOverlayEnabled) {
                int gridX = left + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2;
                int gridY = top + GRID_TOP;
                SignalHeatmap.renderOverlay(graphics, level, gridX, gridY,
                        COLUMNS * CELL_SIZE, ROWS * CELL_SIZE,
                        mapCenterX, mapCenterZ, resolution, player.getUUID());
            }
            BlockPos drawCenter = new BlockPos(cachedCenterX, center.getY(), cachedCenterZ);
            renderAntennas(graphics, level, drawCenter);
            renderPoints(graphics, level.dimension().location().toString(), drawCenter, layoutMouseX, layoutMouseY);
            renderPlayerMarker(graphics, player.blockPosition(), player.getYRot(), drawCenter);
        } else {
            graphics.drawCenteredString(font, "Map unavailable", left + PhoneFrame.getScreenW() / 2,
                    top + 110, PhoneColors.ERROR_TEXT);
        }
        MapPointOfInterest selected = selectedPoint();
        String pointLabel = selected == null ? "Right-click map to add a point"
                : fit(selected.name(), 180) + " - " + selected.visibility().name().toLowerCase(java.util.Locale.ROOT);
        graphics.drawString(font, fit(pointLabel, PhoneFrame.getScreenW() - 20), left + 10,
                top + PhoneFrame.getScreenH() - 69,
                selected == null ? PhoneColors.SECONDARY_TEXT : PhoneColors.ACCENT_TEXT, true);
        int controlsY = top + PhoneFrame.getScreenH() - 47;
        drawActionButton(graphics, left + 152, top + 58, 20, 18, "-", true, layoutMouseX, layoutMouseY);
        drawActionButton(graphics, left + 176, top + 58, 20, 18, "+", true, layoutMouseX, layoutMouseY);
        drawActionButton(graphics, left + 8, controlsY, 42, 18, "POI", true, layoutMouseX, layoutMouseY);
        boolean ownsSelected = selected != null && minecraft != null && minecraft.player != null
                && selected.ownerUUID() != null && selected.ownerUUID().equals(minecraft.player.getUUID());
        drawActionButton(graphics, left + 54, controlsY, 50, 18, "Share",
                ownsSelected && selected.visibility() != MapPointOfInterest.Visibility.PUBLIC,
                layoutMouseX, layoutMouseY);
        drawActionButton(graphics, left + 108, controlsY, 48, 18, "Delete", ownsSelected,
                layoutMouseX, layoutMouseY);
        drawActionButton(graphics, left + 160, controlsY, 40, 18, "Signal",
                signalOverlayEnabled, layoutMouseX, layoutMouseY);
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void drawActionButton(GuiGraphics graphics, int x, int y, int width, int height,
                                  String label, boolean enabled, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        int color = enabled
                ? (hovered ? PhoneColors.ACCENT_TEXT : PhoneColors.bgSecondary(ClientPhoneCache.phoneStyle()))
                : PhoneColors.divider(ClientPhoneCache.phoneStyle());
        PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(), x, y, width, height, 6, color);
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2,
                PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
    }

    private void renderGrid(GuiGraphics graphics, net.minecraft.client.multiplayer.ClientLevel level,
                            BlockPos center) {
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        int gridX = left + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2;
        int gridY = top + GRID_TOP;
        int[] tileColors = cachedTileColors(level, center);
        // The cache may lag behind the live center; shift it so tiles stay aligned with markers.
        int shiftX = (int) Math.round((cachedCenterX - center.getX()) / (double) resolution * CELL_SIZE);
        int shiftY = (int) Math.round((cachedCenterZ - center.getZ()) / (double) resolution * CELL_SIZE);
        shiftPxX = shiftX;
        shiftPxY = shiftY;
        graphics.fill(gridX, gridY, gridX + COLUMNS * CELL_SIZE, gridY + ROWS * CELL_SIZE, 0xFF101820);
        for (int row = 0; row < ROWS; row++) {
            int y = gridY + shiftY + row * CELL_SIZE;
            int y1 = Math.max(y, gridY);
            int y2 = Math.min(y + CELL_SIZE, gridY + ROWS * CELL_SIZE);
            if (y1 >= y2) {
                continue;
            }
            int runStart = 0;
            for (int column = 1; column <= COLUMNS; column++) {
                int base = row * COLUMNS;
                if (column == COLUMNS || tileColors[base + column] != tileColors[base + runStart]) {
                    int x1 = Math.max(gridX + shiftX + runStart * CELL_SIZE, gridX);
                    int x2 = Math.min(gridX + shiftX + column * CELL_SIZE, gridX + COLUMNS * CELL_SIZE);
                    if (x1 < x2) {
                        graphics.fill(x1, y1, x2, y2, tileColors[base + runStart]);
                    }
                    runStart = column;
                }
            }
        }
    }
    private int[] cachedTileColors(net.minecraft.client.multiplayer.ClientLevel level, BlockPos center) {
        String dimension = level.dimension().location().toString();
        long now = System.currentTimeMillis();
        if (cachedTileColors != null && cachedCenterX == center.getX() && cachedCenterZ == center.getZ()
                && cachedResolution == resolution && cachedDimension.equals(dimension)
                && now - lastCacheTimeMillis < 1000L) {
            return cachedTileColors;
        }

        if (cachedTileColors != null && cachedResolution == resolution && cachedDimension.equals(dimension)
                && now - lastCacheTimeMillis < 120L) {
            return cachedTileColors;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX = center.getX() - COLUMNS / 2 * resolution;
        int startZ = center.getZ() - ROWS / 2 * resolution;
        int samplesPerTile = resolution * resolution;
        int[] sampledHeights = new int[COLUMNS * ROWS * samplesPerTile];
        java.util.Arrays.fill(sampledHeights, Integer.MIN_VALUE);
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                for (int dz = 0; dz < resolution; dz++) {
                    for (int dx = 0; dx < resolution; dx++) {
                        int sampleIndex = (row * COLUMNS + column) * samplesPerTile + dz * resolution + dx;
                        int worldX = startX + column * resolution + dx;
                        int worldZ = startZ + row * resolution + dz;
                        if (!level.hasChunkAt(pos.set(worldX, center.getY(), worldZ))) {
                            continue;
                        }
                        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                        if (surfaceY < level.getMinBuildHeight()) {
                            continue;
                        }
                        sampledHeights[sampleIndex] = surfaceY;
                        minY = Math.min(minY, surfaceY);
                        maxY = Math.max(maxY, surfaceY);
                    }
                }
            }
        }

        if (minY == Integer.MAX_VALUE) {
            minY = maxY = center.getY();
        }
        int[] colors = new int[COLUMNS * ROWS];
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                long red = 0;
                long green = 0;
                long blue = 0;
                int samples = 0;
                for (int dz = 0; dz < resolution; dz++) {
                    for (int dx = 0; dx < resolution; dx++) {
                        int sampleIndex = (row * COLUMNS + column) * samplesPerTile + dz * resolution + dx;
                        int surfaceY = sampledHeights[sampleIndex];
                        if (surfaceY == Integer.MIN_VALUE) {
                            continue;
                        }
                        int worldX = startX + column * resolution + dx;
                        int worldZ = startZ + row * resolution + dz;
                        var state = level.getBlockState(pos.set(worldX, surfaceY, worldZ));
                        int color = MapPalette.colorFor(state, worldX, surfaceY, worldZ, minY, maxY);
                        red += (color >> 16) & 0xFF;
                        green += (color >> 8) & 0xFF;
                        blue += color & 0xFF;
                        samples++;
                    }
                }
                colors[row * COLUMNS + column] = samples == 0 ? 0xFF101820
                        : 0xFF000000 | (int) (red / samples) << 16
                        | (int) (green / samples) << 8 | (int) (blue / samples);
            }
        }

        cachedTileColors = colors;
        cachedCenterX = center.getX();
        cachedCenterZ = center.getZ();
        cachedResolution = resolution;
        cachedDimension = dimension;
        lastCacheTimeMillis = now;
        return cachedTileColors;
    }

    private void renderAntennas(GuiGraphics graphics,
                                net.minecraft.client.multiplayer.ClientLevel level, BlockPos center) {
        int startX = center.getX() - COLUMNS / 2 * resolution;
        int startZ = center.getZ() - ROWS / 2 * resolution;
        int left = PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2;
        int top = PhoneFrame.getScreenY(this) + GRID_TOP;
        int minChunkX = startX >> 4;
        int maxChunkX = (startX + COLUMNS * resolution) >> 4;
        int minChunkZ = startZ >> 4;
        int maxChunkZ = (startZ + ROWS * resolution) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof AntennaBlockEntity antenna)) {
                        continue;
                    }
                    int column = Math.floorDiv(antenna.getBlockPos().getX() - startX, resolution);
                    int row = Math.floorDiv(antenna.getBlockPos().getZ() - startZ, resolution);
                    if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) {
                        continue;
                    }
                    int color = switch (antenna.getTier().maxSignalTier()) {
                        case FIVE_G_PLUS, FIVE_G -> 0xFF3CDD6B;
                        case FOUR_G_PLUS, FOUR_G, LTE -> 0xFFFFD447;
                        case THREE_G, H_PLUS, H -> 0xFFFF9638;
                        case TWO_G, E_PLUS, E, NO_SERVICE -> 0xFFFF4B4B;
                    };
                    int x = left + shiftPxX + column * CELL_SIZE;
                    int y = top + shiftPxY + row * CELL_SIZE;
                    if (x < left || y < top || x + 10 > left + COLUMNS * CELL_SIZE
                            || y + 10 > top + ROWS * CELL_SIZE) {
                        continue;
                    }
                    graphics.fill(x + 5, y + 5, x + 10, y + 10, color);
                }
            }
        }
    }

    private void renderPoints(GuiGraphics graphics, String dimension, BlockPos center, int mouseX, int mouseY) {
        int startX = center.getX() - COLUMNS / 2 * resolution;
        int startZ = center.getZ() - ROWS / 2 * resolution;
        int gridX = PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2;
        int gridY = PhoneFrame.getScreenY(this) + GRID_TOP;
        for (MapPointOfInterest point : MapPointCache.points()) {
            if (!point.dimension().equals(dimension)) {
                continue;
            }
            double worldScreenX = gridX + shiftPxX + ((point.x() - startX) / (double) resolution + 0.5D) * CELL_SIZE;
            double worldScreenY = gridY + shiftPxY + ((point.z() - startZ) / (double) resolution + 0.5D) * CELL_SIZE;
            MarkerPosition marker = clampMarker(worldScreenX, worldScreenY, gridX, gridY);
            int x = (int) Math.round(marker.x());
            int y = (int) Math.round(marker.y());
            int color = point.ownerUUID() == null ? 0xFFB77AFF : switch (point.visibility()) {
                case PERSONAL -> 0xFFB77AFF;
                case PRIVATE -> 0xFFFF7C7C;
                case PUBLIC -> 0xFF59D9E8;
            };
            if (marker.offscreen()) {
                drawTriangle(graphics, marker.x(), marker.y(), marker.directionX(), marker.directionY(),
                        0xFF000000, 5.0D);
                drawTriangle(graphics, marker.x(), marker.y(), marker.directionX(), marker.directionY(),
                        color, 3.5D);
            } else {
                drawCircle(graphics, x + 1, y + 2, 5, 0x80000000);
                drawCircle(graphics, x, y, 5, 0xFFFFFFFF);
                drawCircle(graphics, x, y, 4, color);
            }
            String label = fit(point.name(), 72);
            int labelX = marker.offscreen()
                    ? Math.max(gridX + 2, Math.min(gridX + COLUMNS * CELL_SIZE - font.width(label) - 3,
                    x - font.width(label) - 8))
                    : Math.min(gridX + COLUMNS * CELL_SIZE - font.width(label) - 2, x + 7);
            int labelY = Math.clamp(y - font.lineHeight / 2, gridY + 1,
                    gridY + ROWS * CELL_SIZE - font.lineHeight - 1);
            graphics.drawString(font, label, labelX + 1, labelY + 1, 0xB8000000, false);
            graphics.drawString(font, label, labelX, labelY, 0xFFFFFFFF, true);
        }
    }

    private void renderPlayerMarker(GuiGraphics graphics, BlockPos playerPos, float yaw, BlockPos center) {
        int gridX = PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2;
        int gridY = PhoneFrame.getScreenY(this) + GRID_TOP;
        int startX = center.getX() - COLUMNS / 2 * resolution;
        int startZ = center.getZ() - ROWS / 2 * resolution;
        double worldScreenX = gridX + shiftPxX + ((playerPos.getX() - startX) / (double) resolution + 0.5D) * CELL_SIZE;
        double worldScreenY = gridY + shiftPxY + ((playerPos.getZ() - startZ) / (double) resolution + 0.5D) * CELL_SIZE;
        MarkerPosition marker = clampMarker(worldScreenX, worldScreenY, gridX, gridY);
        if (marker.offscreen()) {
            drawTriangle(graphics, marker.x(), marker.y(), marker.directionX(), marker.directionY(),
                    0xFF000000, 6.0D);
            drawTriangle(graphics, marker.x(), marker.y(), marker.directionX(), marker.directionY(),
                    PhoneColors.accent(ClientPhoneCache.phoneStyle()), 4.5D);
            return;
        }

        int x = (int) Math.round(marker.x());
        int y = (int) Math.round(marker.y());
        drawCircle(graphics, x + 1, y + 2, 6, 0x80000000);
        drawCircle(graphics, x, y, 5, PhoneColors.accent(ClientPhoneCache.phoneStyle()));
        drawCircle(graphics, x, y, 3, 0xFFFFFFFF);
        double radians = Math.toRadians(yaw);
        double directionX = -Math.sin(radians);
        double directionY = Math.cos(radians);
        drawTriangle(graphics, x + directionX * 7.0D, y + directionY * 7.0D,
                directionX, directionY, PhoneColors.accent(ClientPhoneCache.phoneStyle()), 4.0D);
    }

    private MarkerPosition clampMarker(double x, double y, int gridX, int gridY) {
        int right = gridX + COLUMNS * CELL_SIZE;
        int bottom = gridY + ROWS * CELL_SIZE;
        double inset = 8.0D;
        double clampedX = Math.clamp(x, gridX + inset, right - inset);
        double clampedY = Math.clamp(y, gridY + inset, bottom - inset);
        double directionX = x - clampedX;
        double directionY = y - clampedY;
        boolean offscreen = x < gridX || x >= right || y < gridY || y >= bottom;
        double length = Math.hypot(directionX, directionY);
        if (length > 0.0D) {
            directionX /= length;
            directionY /= length;
        }
        return new MarkerPosition(clampedX, clampedY, directionX, directionY, offscreen);
    }

    private void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radiusSquared) {
                    graphics.fill(centerX + dx, centerY + dy,
                            centerX + dx + 1, centerY + dy + 1, color);
                }
            }
        }
    }

    private void drawTriangle(GuiGraphics graphics, double centerX, double centerY,
                              double directionX, double directionY, int color, double size) {
        double perpendicularX = -directionY;
        double perpendicularY = directionX;
        double tipX = centerX + directionX * size;
        double tipY = centerY + directionY * size;
        double baseX = centerX - directionX * size * 0.65D;
        double baseY = centerY - directionY * size * 0.65D;
        double halfWidth = size * 0.65D;
        double leftX = baseX + perpendicularX * halfWidth;
        double leftY = baseY + perpendicularY * halfWidth;
        double rightX = baseX - perpendicularX * halfWidth;
        double rightY = baseY - perpendicularY * halfWidth;
        int minX = (int) Math.floor(Math.min(tipX, Math.min(leftX, rightX)));
        int maxX = (int) Math.ceil(Math.max(tipX, Math.max(leftX, rightX)));
        int minY = (int) Math.floor(Math.min(tipY, Math.min(leftY, rightY)));
        int maxY = (int) Math.ceil(Math.max(tipY, Math.max(leftY, rightY)));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (insideTriangle(x + 0.5D, y + 0.5D, tipX, tipY, leftX, leftY, rightX, rightY)) {
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private boolean insideTriangle(double px, double py, double ax, double ay,
                                   double bx, double by, double cx, double cy) {
        double first = (px - bx) * (ay - by) - (ax - bx) * (py - by);
        double second = (px - cx) * (by - cy) - (bx - cx) * (py - cy);
        double third = (px - ax) * (cy - ay) - (cx - ax) * (py - ay);
        boolean negative = first < 0.0D || second < 0.0D || third < 0.0D;
        boolean positive = first > 0.0D || second > 0.0D || third > 0.0D;
        return !(negative && positive);
    }

    private record MarkerPosition(double x, double y, double directionX, double directionY, boolean offscreen) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = PhoneFrame.layoutMouseX(this, mouseX);
        mouseY = PhoneFrame.layoutMouseY(this, mouseY);
        if (!MapLoadCache.ready() || !MapLoadCache.message().isEmpty()
                || ClientSignalCache.tier() == com.example.mobilecellnetwork.network.SignalTier.NO_SERVICE) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (minecraft != null && minecraft.player != null && minecraft.level != null
                && isInsideMap(mouseX, mouseY)) {
            if (button == 0) {
                ensureMapCenter();
                draggingMap = true;
                SignalHeatmap.setDragging(true);
                dragStartX = mouseX;
                dragStartY = mouseY;
                dragCenterX = mapCenterX;
                dragCenterZ = mapCenterZ;
                return true;
            }
            if (button == 1) {
                handleMapPointClick(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        mouseX = PhoneFrame.layoutMouseX(this, mouseX);
        mouseY = PhoneFrame.layoutMouseY(this, mouseY);
        dragX = PhoneFrame.layoutMouseDeltaX(this, dragX);
        dragY = PhoneFrame.layoutMouseDeltaY(this, dragY);
        if (draggingMap && button == 0) {
            int blockDeltaX = (int) Math.round((dragStartX - mouseX) / CELL_SIZE * resolution);
            int blockDeltaZ = (int) Math.round((dragStartY - mouseY) / CELL_SIZE * resolution);
            mapCenterX = dragCenterX + blockDeltaX;
            mapCenterZ = dragCenterZ + blockDeltaZ;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX = PhoneFrame.layoutMouseX(this, mouseX);
        mouseY = PhoneFrame.layoutMouseY(this, mouseY);
        if (button == 0 && draggingMap) {
            draggingMap = false;
            SignalHeatmap.setDragging(false);
            boolean clicked = Math.abs(mouseX - dragStartX) < 4 && Math.abs(mouseY - dragStartY) < 4;
            if (clicked) {
                selectPointAt(mouseX, mouseY);
            } else if (signalOverlayEnabled) {
                requestAntennaCache();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        mouseX = PhoneFrame.layoutMouseX(this, mouseX);
        mouseY = PhoneFrame.layoutMouseY(this, mouseY);
        if (isInsideMap(mouseX, mouseY)) {
            setResolution(resolution + (scrollY < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY));
    }

    private boolean isInsideMap(double mouseX, double mouseY) {
        int gridLeft = PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2;
        int gridTop = PhoneFrame.getScreenY(this) + GRID_TOP;
        return mouseX >= gridLeft && mouseX < gridLeft + COLUMNS * CELL_SIZE
                && mouseY >= gridTop && mouseY < gridTop + ROWS * CELL_SIZE;
    }

    private void ensureMapCenter() {
        if (mapCenterX == Integer.MIN_VALUE && minecraft.player != null) {
            mapCenterX = minecraft.player.blockPosition().getX();
            mapCenterZ = minecraft.player.blockPosition().getZ();
        }
    }

    private void selectPointAt(double mouseX, double mouseY) {
        int column = ((int) mouseX - (PhoneFrame.getScreenX(this)
                + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2)) / CELL_SIZE;
        int row = ((int) mouseY - (PhoneFrame.getScreenY(this) + GRID_TOP)) / CELL_SIZE;
        int x = mapCenterX - COLUMNS / 2 * resolution + column * resolution + resolution / 2;
        int z = mapCenterZ - ROWS / 2 * resolution + row * resolution + resolution / 2;
        MapPointOfInterest clicked = pointAt(x, z, minecraft.level.dimension().location().toString());
        selectedPoint = clicked == null ? null : clicked.id();
    }

    private void handleMapPointClick(double mouseX, double mouseY) {
        ensureMapCenter();
        int column = ((int) mouseX - (PhoneFrame.getScreenX(this)
                + (PhoneFrame.getScreenW() - COLUMNS * CELL_SIZE) / 2)) / CELL_SIZE;
        int row = ((int) mouseY - (PhoneFrame.getScreenY(this) + GRID_TOP)) / CELL_SIZE;
        int x = mapCenterX - COLUMNS / 2 * resolution + column * resolution + resolution / 2;
        int z = mapCenterZ - ROWS / 2 * resolution + row * resolution + resolution / 2;
        BlockPos cellPos = new BlockPos(x, minecraft.player.blockPosition().getY(), z);
        if (!minecraft.level.hasChunkAt(cellPos)) {
            return;
        }
        int y = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        MapPointOfInterest clicked = pointAt(x, z, minecraft.level.dimension().location().toString());
        if (clicked != null) {
            selectedPoint = clicked.id();
            return;
        }
        selectedPoint = null;
        minecraft.setScreen(new MapPointCreateModal(this, (name, visibility) ->
                PacketDistributor.sendToServer(new MapPointActionPacket("create", "", name,
                        visibility.name(), x, y, z, ""))));
    }

    private MapPointOfInterest pointAt(int x, int z, String dimension) {
        int startX = mapCenterX - COLUMNS / 2 * resolution;
        int startZ = mapCenterZ - ROWS / 2 * resolution;
        int selectedColumn = Math.floorDiv(x - startX, resolution);
        int selectedRow = Math.floorDiv(z - startZ, resolution);
        return MapPointCache.points().stream().filter(point -> point.dimension().equals(dimension)
                && Math.floorDiv(point.x() - startX, resolution) == selectedColumn
                && Math.floorDiv(point.z() - startZ, resolution) == selectedRow)
                .findFirst().orElse(null);
    }

    private MapPointOfInterest selectedPoint() {
        if (selectedPoint == null) {
            return null;
        }
        return MapPointCache.points().stream().filter(point -> point.id().equals(selectedPoint))
                .findFirst().orElse(null);
    }

    private void shareSelected() {
        MapPointOfInterest selected = selectedPoint();
        if (selected == null ||         selected.visibility() == MapPointOfInterest.Visibility.PUBLIC
                || selected.ownerUUID() == null || minecraft.player == null
                || !selected.ownerUUID().equals(minecraft.player.getUUID())) {
            return;
        }
        minecraft.setScreen(new MapPointShareModal(this, number -> PacketDistributor.sendToServer(
                new MapPointActionPacket("share", selected.id().toString(), "", "", 0, 0, 0, number))));
    }

    private void focusNextPoint() {
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        String dimension = minecraft.level.dimension().location().toString();
        List<MapPointOfInterest> available = MapPointCache.points().stream()
                .filter(point -> point.dimension().equals(dimension)).toList();
        if (available.isEmpty()) {
            return;
        }
        int selectedIndex = -1;
        for (int index = 0; index < available.size(); index++) {
            if (available.get(index).id().equals(selectedPoint)) {
                selectedIndex = index;
                break;
            }
        }
        MapPointOfInterest next = available.get((selectedIndex + 1) % available.size());
        selectedPoint = next.id();
        mapCenterX = next.x();
        mapCenterZ = next.z();
        PacketDistributor.sendToServer(new MapInteractionPacket(resolution));
        if (signalOverlayEnabled) {
            requestAntennaCache();
        }
    }

    private void deleteSelected() {
        MapPointOfInterest selected = selectedPoint();
        if (selected == null || minecraft.player == null || selected.ownerUUID() == null
                || !selected.ownerUUID().equals(minecraft.player.getUUID())) {
            return;
        }
        PacketDistributor.sendToServer(new MapPointActionPacket("delete", selected.id().toString(),
                "", "", 0, 0, 0, ""));
        selectedPoint = null;
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(suffix))) + suffix;
    }

    private void back() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.setScreen(new PhoneScreen(menu, minecraft.player.getInventory(), Component.literal("Phone")));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void removed() {
        SignalHeatmap.setDragging(false);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
