package com.example.mobilecellnetwork.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TestTowerGeometry {
    public static final int RADIUS = 2;
    public static final int WIDTH = RADIUS * 2 + 1;
    public static final float FOUNDATION_TOP = TowerDimensions.FOUNDATION_TOP;
    public static final float LADDER_Z = -1.5F;
    public static final float PLATFORM_HALF_WIDTH = 2.0F;
    public static final float DECK_THICKNESS = TowerDimensions.DECK_THICKNESS;
    public static final float RAIL_HEIGHT = TowerDimensions.RAIL_HEIGHT;
    public static final List<Integer> PLATFORM_LEVELS = List.of(TestTowerBlock.HEIGHT - 6, TestTowerBlock.HEIGHT - 3);
    public static final List<Float> CAGE_LEVELS = cageLevels();
    public static final List<Beam> PANEL_MOUNTS = panelMounts();
    public static final List<Part> PARTS = createParts();
    public static final List<Surface> SURFACES = createSurfaces();
    public static final List<Beam> BEAMS = createBeams();
    private static final List<Part> COMPUTER_DECK_PARTS = computerDeckParts();
    private static final List<Surface> COMPUTER_DECK_SURFACES = createSurfaces(COMPUTER_DECK_PARTS);
    private static final List<Beam> COMPUTER_DECK_BEAMS = BEAMS.stream()
            .filter(beam -> !PANEL_MOUNTS.contains(beam) || beam.y1() > 17).toList();
    private static final VoxelShape[] SHAPES = createShapes();

    private TestTowerGeometry() {
    }

    public static float mastHalfWidth(float y) {
        return TowerDimensions.mastHalfWidth(y);
    }

    public static VoxelShape shape(int x, int y, int z) {
        return SHAPES[(y * WIDTH + x + RADIUS) * WIDTH + z + RADIUS];
    }

    public static void prepare() {
        for (VoxelShape shape : SHAPES) shape.toAabbs();
        VariantShapes.CACHE.size();
        MonopoleGeometry.prepare();
    }

    public static VoxelShape shape(TowerVariant variant, int x, int y, int z) {
        if (variant.monopole()) return MonopoleGeometry.shape(x, y, z);
        return variant == TowerVariant.TIER_3 ? shape(x, y, z)
                : VariantShapes.CACHE.get(variant)[(y * WIDTH + x + RADIUS) * WIDTH + z + RADIUS];
    }

    private static final class VariantShapes {
        private static final java.util.Map<TowerVariant, VoxelShape[]> CACHE = create();

        private static java.util.Map<TowerVariant, VoxelShape[]> create() {
            var cache = new java.util.EnumMap<TowerVariant, VoxelShape[]>(TowerVariant.class);
            for (TowerVariant variant : new TowerVariant[] {TowerVariant.TIER_1, TowerVariant.TIER_2}) {
                cache.put(variant, createShapes(variant));
            }
            return java.util.Map.copyOf(cache);
        }
    }

    public static boolean onLadder(double x, double y, double z) {
        return x >= 0.05 && x <= 0.95 && y >= 0 && y < TestTowerBlock.HEIGHT - 0.4
                && z >= -2.0 && z < LADDER_Z;
    }

    public static boolean onLegacyLadder(double x, double z) {
        return x >= 0.15 && x <= 0.85 && z >= 0 && z < 7.0 / 16.0;
    }

    public enum Material {
        CONCRETE, STEEL, DARK, CABINET, TERMINAL, LAMP, LADDER
    }

    public record Part(Material material, AABB bounds) {
    }

    public record Surface(Material material, Direction direction, AABB bounds) {
    }

    private record Rectangle(double minU, double minV, double maxU, double maxV) {
    }

    private static boolean staticMaterial(Material material) {
        return material == Material.CONCRETE || material == Material.STEEL
                || material == Material.DARK || material == Material.LADDER;
    }

    private static List<Surface> createSurfaces() {
        return createSurfaces(PARTS);
    }

    public static List<Part> parts(TowerVariant variant) {
        return variant.separateComputerDeck() ? COMPUTER_DECK_PARTS : PARTS;
    }

    public static List<Surface> surfaces(TowerVariant variant) {
        return variant.separateComputerDeck() ? COMPUTER_DECK_SURFACES : SURFACES;
    }

    public static List<Beam> beams(TowerVariant variant) {
        return variant.separateComputerDeck() ? COMPUTER_DECK_BEAMS : BEAMS;
    }

    private static List<Part> computerDeckParts() {
        var parts = new ArrayList<Part>();
        for (Part part : PARTS) {
            AABB b = part.bounds();
            if (part.material() == Material.TERMINAL && b.equals(TowerCabinets.TERMINALS.get(2))) continue;
            boolean computerSupport = part.material() == Material.DARK
                    && b.equals(terminalSupport(2));
            boolean lowerRadioBranch = part.material() == Material.DARK && b.minY == 14 - 0.08
                    && b.maxY <= 14 + 0.32
                    || part.material() == Material.STEEL && b.minX == 2.29 && b.minY == 14 + 0.26;
            if (!computerSupport && !lowerRadioBranch) parts.add(part);
        }
        return List.copyOf(parts);
    }

    private static AABB terminalSupport(int index) {
        AABB cabinet = TowerCabinets.BOUNDS.get(index);
        return TowerCabinets.localBounds(index,
                new AABB(cabinet.getXsize() / 2 - 0.03, cabinet.getYsize(), 0.06,
                        cabinet.getXsize() / 2 + 0.03, cabinet.getYsize() + 0.1, 0.12));
    }

    private static List<Surface> createSurfaces(List<Part> parts) {
        List<Surface> surfaces = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            Part part = parts.get(index);
            if (!staticMaterial(part.material)) continue;
            for (Direction direction : Direction.values()) {
                int axis = direction.getAxis().ordinal();
                int u = axis == 0 ? 1 : 0;
                int v = axis == 2 ? 1 : 2;
                boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                double plane = coordinate(part.bounds, axis, positive);
                List<Rectangle> visible = new ArrayList<>();
                visible.add(new Rectangle(coordinate(part.bounds, u, false), coordinate(part.bounds, v, false),
                        coordinate(part.bounds, u, true), coordinate(part.bounds, v, true)));
                for (int otherIndex = 0; otherIndex < parts.size() && !visible.isEmpty(); otherIndex++) {
                    if (index == otherIndex) continue;
                    Part other = parts.get(otherIndex);
                    if (!staticMaterial(other.material)) continue;
                    double min = coordinate(other.bounds, axis, false);
                    double max = coordinate(other.bounds, axis, true);
                    // Remove buried/contact faces; give coincident outer faces a single owner.
                    boolean covered = positive
                            ? min <= plane + 0.000001 && (max > plane + 0.000001
                                    || Math.abs(max - plane) < 0.000001 && otherIndex < index)
                            : max >= plane - 0.000001 && (min < plane - 0.000001
                                    || Math.abs(min - plane) < 0.000001 && otherIndex < index);
                    if (!covered) continue;
                    Rectangle cut = new Rectangle(coordinate(other.bounds, u, false), coordinate(other.bounds, v, false),
                            coordinate(other.bounds, u, true), coordinate(other.bounds, v, true));
                    List<Rectangle> remaining = new ArrayList<>();
                    for (Rectangle face : visible) subtract(face, cut, remaining);
                    visible = remaining;
                }
                for (Rectangle face : visible) {
                    double[] min = new double[3];
                    double[] max = new double[3];
                    min[axis] = max[axis] = plane;
                    min[u] = face.minU;
                    max[u] = face.maxU;
                    min[v] = face.minV;
                    max[v] = face.maxV;
                    surfaces.add(new Surface(part.material, direction,
                            new AABB(min[0], min[1], min[2], max[0], max[1], max[2])));
                }
            }
        }
        return List.copyOf(surfaces);
    }

    private static double coordinate(AABB box, int axis, boolean max) {
        return axis == 0 ? (max ? box.maxX : box.minX)
                : axis == 1 ? (max ? box.maxY : box.minY) : (max ? box.maxZ : box.minZ);
    }

    private static void subtract(Rectangle face, Rectangle cut, List<Rectangle> result) {
        double minU = Math.max(face.minU, cut.minU);
        double maxU = Math.min(face.maxU, cut.maxU);
        double minV = Math.max(face.minV, cut.minV);
        double maxV = Math.min(face.maxV, cut.maxV);
        if (minU >= maxU || minV >= maxV) {
            result.add(face);
            return;
        }
        if (face.minU < minU) result.add(new Rectangle(face.minU, face.minV, minU, face.maxV));
        if (maxU < face.maxU) result.add(new Rectangle(maxU, face.minV, face.maxU, face.maxV));
        if (face.minV < minV) result.add(new Rectangle(minU, face.minV, maxU, minV));
        if (maxV < face.maxV) result.add(new Rectangle(minU, maxV, maxU, face.maxV));
    }

    public record Beam(float x1, float y1, float z1, float x2, float y2, float z2, float thickness) {
    }

    private static List<Beam> createBeams() {
        List<Beam> beams = new ArrayList<>();
        float top = TestTowerBlock.HEIGHT - 0.6F;
        for (float y = FOUNDATION_TOP; y < top; y += 1.5F) {
            float next = Math.min(y + 1.5F, top);
            float lower = mastHalfWidth(y);
            float upper = mastHalfWidth(next);
            for (int x : new int[] {-1, 1}) {
                for (int z : new int[] {-1, 1}) {
                    beams.add(new Beam(0.5F + x * lower, y, 0.5F + z * lower,
                            0.5F + x * upper, next, 0.5F + z * upper, 0.12F));
                }
            }
            for (int side = 0; side < 4; side++) {
                addRotatedBeam(beams, side, -lower, y, -lower, upper, next, -upper, 0.045F);
                // Offset the crossing diagonal in depth instead of overlapping its broad faces.
                addRotatedBeam(beams, side, lower, y, -lower + 0.07F, -upper, next, -upper + 0.07F, 0.045F);
                addRotatedBeam(beams, side, -upper, next, -upper, upper, next, -upper, 0.07F);
            }
        }
        beams.addAll(PANEL_MOUNTS);
        float doorY = FOUNDATION_TOP + 2.45F;
        float doorHalf = mastHalfWidth(doorY);
        for (float z : new float[] {-1.05F, 0.5F}) {
            float legZ = z < 0 ? 0.5F - doorHalf : 0.5F + doorHalf;
            beams.add(new Beam(2.05F, doorY, z, 0.5F + doorHalf, doorY, legZ, 0.075F));
        }
        for (int deck : PLATFORM_LEVELS) {
            float y = deck + 2.25F;
            float half = mastHalfWidth(y);
            beams.add(new Beam(0.5F - half, y, 0.5F - half,
                    0.5F + half, y, 0.5F - half, 0.07F));
            beams.add(new Beam(0.5F + half, y, 0.5F - half,
                    0.5F + half, y, 0.5F + half, 0.07F));
        }
        for (float y = 2; y < top; y += 3) {
            float wallZ = 0.5F - mastHalfWidth(y);
            float half = mastHalfWidth(y);
            // Standoffs need a real crossmember, not just the mast's tapered outline.
            beams.add(new Beam(0.5F - half, y, wallZ, 0.5F + half, y, wallZ, 0.075F));
            for (float x : new float[] {0.25F, 0.75F}) {
                beams.add(new Beam(x, y, LADDER_Z + 0.05F, x, y, wallZ, 0.05F));
            }
        }
        for (float y : CAGE_LEVELS) {
            float half = mastHalfWidth(y);
            for (int side : new int[] {-1, 1}) {
                beams.add(new Beam(0.5F + side * 0.6F, y, -1.4F,
                        0.5F + side * half, y, 0.5F - half, 0.04F));
            }
        }
        return List.copyOf(beams);
    }

    private static List<Beam> panelMounts() {
        List<Beam> beams = new ArrayList<>();
        for (int sector = 0; sector < 3; sector++) {
            double angle = Math.toRadians(sector * 120);
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            for (int deck : PLATFORM_LEVELS) {
                for (float y : new float[] {deck + 0.75F, deck + 1.05F}) {
                    beams.add(new Beam(0.5F, y, 0.5F, 0.5F + sin * 1.11F, y,
                            0.5F + cos * 1.11F, 0.075F));
                    beams.add(new Beam(0.5F - cos * 0.35F + sin * 1.11F, y,
                            0.5F + sin * 0.35F + cos * 1.11F,
                            0.5F + cos * 0.35F + sin * 1.11F, y,
                            0.5F - sin * 0.35F + cos * 1.11F, 0.07F));
                }
            }
        }
        return List.copyOf(beams);
    }

    private static List<Float> cageLevels() {
        List<Float> levels = new ArrayList<>();
        for (float y = 3; y < TestTowerBlock.HEIGHT - 0.6F; y += 1.25F) {
            boolean landing = false;
            for (int deck : PLATFORM_LEVELS) {
                if (y > deck - 0.2F && y < deck + 2.3F) landing = true;
            }
            if (!landing) levels.add(y);
        }
        return List.copyOf(levels);
    }

    private static void addRotatedBeam(List<Beam> beams, int side, float x1, float y1, float z1,
                                       float x2, float y2, float z2, float thickness) {
        float cos = side == 0 ? 1 : side == 2 ? -1 : 0;
        float sin = side == 1 ? 1 : side == 3 ? -1 : 0;
        // Omit an obstructing brace as a whole; clipping it left unsupported stubs.
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt((x2 - x1) * (x2 - x1)
                + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1)) / 0.1));
        Beam run = null;
        for (int i = 0; i < steps; i++) {
            float a = (float) i / steps;
            float b = (float) (i + 1) / steps;
            float ax = x1 + (x2 - x1) * a;
            float az = z1 + (z2 - z1) * a;
            float bx = x1 + (x2 - x1) * b;
            float bz = z1 + (z2 - z1) * b;
            Beam beam = new Beam(0.5F + ax * cos + az * sin, y1 + (y2 - y1) * a,
                    0.5F - ax * sin + az * cos, 0.5F + bx * cos + bz * sin,
                    y1 + (y2 - y1) * b, 0.5F - bx * sin + bz * cos, thickness);
            if (!inAccessPortal(beam)) {
                run = run == null ? beam : new Beam(run.x1, run.y1, run.z1,
                        beam.x2, beam.y2, beam.z2, thickness);
            } else return;
        }
        if (run != null) beams.add(run);
    }

    private static boolean inAccessPortal(Beam beam) {
        if (Math.max(beam.y1, beam.y2) >= FOUNDATION_TOP
                && Math.min(beam.y1, beam.y2) < FOUNDATION_TOP + 2.4
                && Math.max(beam.x1, beam.x2) > 1.8
                && Math.abs(beam.z2 - beam.z1) > Math.abs(beam.x2 - beam.x1)
                && Math.max(beam.z1, beam.z2) > -1.05 && Math.min(beam.z1, beam.z2) < 0.45) return true;
        for (int y : PLATFORM_LEVELS) {
            if (Math.max(beam.y1, beam.y2) >= y + DECK_THICKNESS - 0.05
                    && Math.min(beam.y1, beam.y2) < y + DECK_THICKNESS + 2.0
                    && Math.max(beam.x1, beam.x2) > -0.1 && Math.min(beam.x1, beam.x2) < 1.8
                    && Math.min(beam.z1, beam.z2) < 0.3) return true;
        }
        return false;
    }

    private static List<Part> createParts() {
        List<Part> parts = new ArrayList<>();
        add(parts, Material.CONCRETE, -2, 0, -2, 3, FOUNDATION_TOP, 3);
        for (double x : new double[] {0.25, 0.75}) {
            add(parts, Material.LADDER, x - 0.025, FOUNDATION_TOP, LADDER_Z, x + 0.025, 19.6, LADDER_Z + 0.05);
        }
        for (double y = 0.5; y < 19.6; y += 0.28) {
            add(parts, Material.LADDER, 0.25, y - 0.015, LADDER_Z, 0.75, y + 0.015, LADDER_Z + 0.05);
        }
        // A framed east-side service doorway preserves the front lattice around the ladder.
        for (double z : new double[] {-1.1, 0.45}) {
            add(parts, Material.STEEL, 2.0, FOUNDATION_TOP, z, 2.1, FOUNDATION_TOP + 2.5, z + 0.1);
            add(parts, Material.STEEL, 1.9, FOUNDATION_TOP, z - 0.05,
                    2.25, FOUNDATION_TOP + 0.04, z + 0.15);
        }
        add(parts, Material.STEEL, 2.0, FOUNDATION_TOP + 2.4, -1.1, 2.1, FOUNDATION_TOP + 2.5, 0.55);
        add(parts, Material.STEEL, 1.95, FOUNDATION_TOP, -1.15, 2.25, FOUNDATION_TOP + 0.025, 0.65);
        for (AABB cabinet : TowerCabinets.BOUNDS) parts.add(new Part(Material.CABINET, cabinet));
        for (int i = 0; i < TowerCabinets.TERMINALS.size(); i++) {
            AABB terminal = TowerCabinets.TERMINALS.get(i);
            parts.add(new Part(Material.TERMINAL, terminal));
            parts.add(new Part(Material.DARK, terminalSupport(i)));
        }
        for (TowerWorkLights.Fixture fixture : TowerWorkLights.FIXTURES) {
            parts.add(new Part(Material.LAMP, fixture.housing()));
            parts.add(new Part(Material.LAMP, fixture.lens()));
        }
        for (int y : PLATFORM_LEVELS) {
            add(parts, Material.DARK, -1.5, y, -1.5, 2.5, y + DECK_THICKNESS, 2.5);
            add(parts, Material.DARK, 1.0, y, -2, 1.8, y + DECK_THICKNESS, -1.45);
            // Side-step right off the continuous ladder onto a railed transfer landing.
            for (double[] rail : new double[][] {
                    {-1.5, -1.5, -0.1, -1.45}, {1.8, -1.5, 2.5, -1.45},
                    {1.75, -1.95, 1.8, -1.5},
                    {-1.5, 2.45, 2.5, 2.5}, {-1.5, -1.5, -1.45, 2.5},
                    {2.45, -1.5, 2.5, 2.5}}) {
                for (double railY : new double[] {y + 0.6, y + DECK_THICKNESS + RAIL_HEIGHT}) {
                    add(parts, Material.STEEL, rail[0], railY - 0.05, rail[1],
                            rail[2], railY, rail[3]);
                }
                add(parts, Material.STEEL, rail[0], y + DECK_THICKNESS, rail[1],
                        rail[0] + 0.05, y + DECK_THICKNESS + RAIL_HEIGHT, rail[1] + 0.05);
                add(parts, Material.STEEL, rail[2] - 0.05, y + DECK_THICKNESS, rail[3] - 0.05,
                        rail[2], y + DECK_THICKNESS + RAIL_HEIGHT, rail[3]);
            }
        }
        for (int x : new int[] {-1, 1}) {
            for (int z : new int[] {-1, 1}) {
                double cx = 0.5 + x * mastHalfWidth(FOUNDATION_TOP);
                double cz = 0.5 + z * mastHalfWidth(FOUNDATION_TOP);
                add(parts, Material.STEEL, cx - 0.22, FOUNDATION_TOP, cz - 0.22,
                        cx + 0.22, FOUNDATION_TOP + 0.07, cz + 0.22);
                for (double dx : new double[] {-0.15, 0.15}) {
                    for (double dz : new double[] {-0.15, 0.15}) {
                        add(parts, Material.DARK, cx + dx - 0.035, FOUNDATION_TOP + 0.07,
                                cz + dz - 0.035, cx + dx + 0.035, FOUNDATION_TOP + 0.13, cz + dz + 0.035);
                    }
                }
            }
        }
        for (double x : new double[] {0.92, 1.02, 1.12}) {
            add(parts, Material.DARK, x, 0.95, 0.65, x + 0.045, 19.0, 0.695);
        }
        add(parts, Material.DARK, 0.8, 0.95, 0.65, 1.165, 0.995, 0.695);
        add(parts, Material.STEEL, 0.81, 0.91, 0.61, 0.87, 1.04, 0.74);
        add(parts, Material.DARK, 0.88, 18.9, 0.6, 1.22, 19.2, 0.78);
        for (int y = 2; y < 19; y += 2) {
            add(parts, Material.STEEL, 0.9, y, 0.62, 1.2, y + 0.06, 0.73);
        }
        for (int y : PLATFORM_LEVELS) {
            add(parts, Material.DARK, 1.12, y - 0.08, -0.97, 1.165, y - 0.035, 0.695);
            add(parts, Material.DARK, 1.12, y - 0.08, -0.97, 2.37, y - 0.035, -0.925);
            add(parts, Material.DARK, 2.325, y - 0.08, -0.97, 2.37, y + 0.32, -0.925);
            add(parts, Material.STEEL, 2.29, y + 0.26, -0.99, 2.42, y + 0.39, -0.86);
        }
        return List.copyOf(parts);
    }

    private static void add(List<Part> parts, Material material,
                            double x1, double y1, double z1, double x2, double y2, double z2) {
        parts.add(new Part(material, new AABB(x1, y1, z1, x2, y2, z2)));
    }

    private static VoxelShape[] createShapes() {
        return createShapes(TowerVariant.TIER_3);
    }

    private static VoxelShape[] createShapes(TowerVariant variant) {
        VoxelShape[] shapes = new VoxelShape[variant.height() * WIDTH * WIDTH];
        List<AABB> collision = new ArrayList<>(parts(variant).stream().map(Part::bounds).toList());
        collision.add(new AABB(0.225, FOUNDATION_TOP, LADDER_Z, 0.775, 19.6, LADDER_Z + 0.05));
        collision.add(new AABB(0.46, 1.4, 0.46, 0.54, TestTowerBlock.HEIGHT - 0.4, 0.54));
        for (Beam beam : beams(variant)) {
            double length = Math.sqrt(Math.pow(beam.x2 - beam.x1, 2) + Math.pow(beam.y2 - beam.y1, 2)
                    + Math.pow(beam.z2 - beam.z1, 2));
            int steps = Math.max(1, (int) Math.ceil(length / 0.1));
            double half = beam.thickness / Math.sqrt(2);
            for (int i = 0; i < steps; i++) {
                double a = (double) i / steps;
                double b = (double) (i + 1) / steps;
                double ax = beam.x1 + (beam.x2 - beam.x1) * a;
                double ay = beam.y1 + (beam.y2 - beam.y1) * a;
                double az = beam.z1 + (beam.z2 - beam.z1) * a;
                double bx = beam.x1 + (beam.x2 - beam.x1) * b;
                double by = beam.y1 + (beam.y2 - beam.y1) * b;
                double bz = beam.z1 + (beam.z2 - beam.z1) * b;
                collision.add(new AABB(Math.min(ax, bx) - half, Math.min(ay, by) - half, Math.min(az, bz) - half,
                        Math.max(ax, bx) + half, Math.max(ay, by) + half, Math.max(az, bz) + half));
            }
        }
        java.util.Arrays.fill(shapes, Shapes.empty());
        // Visit only the cells touched by each part instead of testing every part against all 500 cells.
        for (AABB original : collision) {
            AABB box = variant.bounds(original);
            if (variant != TowerVariant.TIER_3 && original.minY >= 3 && original.maxY <= 14) {
                // Compressed braces share a bounded grid instead of producing thousands of nearly equal voxel planes.
                box = new AABB(Math.floor(box.minX * 64) / 64, Math.floor(box.minY * 64) / 64,
                        Math.floor(box.minZ * 64) / 64, Math.ceil(box.maxX * 64) / 64,
                        Math.ceil(box.maxY * 64) / 64, Math.ceil(box.maxZ * 64) / 64);
            }
            int minX = Math.max(-RADIUS, (int) Math.floor(box.minX));
            int maxX = Math.min(RADIUS, (int) Math.ceil(box.maxX) - 1);
            int minY = Math.max(0, (int) Math.floor(box.minY));
            int maxY = Math.min(variant.height() - 1, (int) Math.ceil(box.maxY) - 1);
            int minZ = Math.max(-RADIUS, (int) Math.floor(box.minZ));
            int maxZ = Math.min(RADIUS, (int) Math.ceil(box.maxZ) - 1);
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        AABB cell = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        AABB clipped = box.intersect(cell).move(-x, -y, -z);
                        int index = (y * WIDTH + x + RADIUS) * WIDTH + z + RADIUS;
                        shapes[index] = Shapes.joinUnoptimized(shapes[index], Shapes.create(clipped), BooleanOp.OR);
                    }
                }
            }
        }
        for (int index = 0; index < shapes.length; index++) {
            shapes[index] = shapes[index].optimize();
        }
        return shapes;
    }

}
