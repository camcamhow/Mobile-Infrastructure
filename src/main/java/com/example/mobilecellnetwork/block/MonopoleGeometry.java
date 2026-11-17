package com.example.mobilecellnetwork.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MonopoleGeometry {
    public enum Material { POLE, STEEL, DARK, PANEL, CONCRETE }
    public record Part(Material material, AABB bounds) {}
    public static final AABB BEACON = new AABB(0.4, 5.82, 0.4, 0.6, 6, 0.6);
    public static final List<Part> PARTS = parts();
    public static final List<TestTowerGeometry.Beam> ARMS = arms();
    private static final VoxelShape[] SHAPES = shapes();

    private MonopoleGeometry() {}

    public static void prepare() {
        for (VoxelShape shape : SHAPES) shape.toAabbs();
    }

    public static boolean onLadder(double x, double y, double z) {
        return z >= 0.2 && z <= 0.8 && y >= 0 && y < 5.65 && x >= -0.55 && x < 0.05;
    }

    public static VoxelShape shape(int x, int y, int z) {
        return SHAPES[(y * 3 + x + 1) * 3 + z + 1];
    }

    private static List<Part> parts() {
        var parts = new ArrayList<Part>();
        add(parts, Material.CONCRETE, -0.3, 0, -0.8, 1.3, TowerDimensions.FOUNDATION_TOP, 1.3);
        var support = new AABB(0.47, 1.4, 0.21, 0.53, 1.5, 0.27)
                .move(TowerCabinets.MONOPOLE_OFFSET);
        parts.add(new Part(Material.STEEL, support));
        add(parts, Material.STEEL, 0.45, 1.25, 0.23, 0.55, 1.32, 0.5);
        for (int section = 0; section < 6; section++) {
            double radius = 0.15 - section * 0.012;
            add(parts, Material.POLE, 0.5 - radius, Math.max(TowerDimensions.FOUNDATION_TOP, section),
                    0.5 - radius, 0.5 + radius, Math.min(section + 1, 5.72), 0.5 + radius);
        }
        add(parts, Material.STEEL, 0.3, TowerDimensions.FOUNDATION_TOP, 0.3, 0.7, 0.4, 0.7);
        for (double x : new double[] {0.32, 0.68}) {
            for (double z : new double[] {0.32, 0.68}) {
                add(parts, Material.STEEL, x - 0.025, 0.4, z - 0.025, x + 0.025, 0.46, z + 0.025);
            }
        }
        for (double z : new double[] {0.28, 0.72}) {
            add(parts, Material.STEEL, 0.05, TowerDimensions.FOUNDATION_TOP, z - 0.018, 0.086, 5.65, z + 0.018);
            for (double y = 2.1; y < 5.6; y += 0.8) {
                add(parts, Material.STEEL, 0.086, y, z - 0.015, 0.5, y + 0.035, z + 0.015);
            }
        }
        for (double y = 0.55; y < 5.65; y += 0.28) {
            add(parts, Material.STEEL, 0.05, y, 0.28, 0.086, y + 0.025, 0.72);
        }
        for (double y = 2.1; y < 5.6; y += 0.8) {
            add(parts, Material.STEEL, 0.485, y, 0.265, 0.515, y + 0.035, 0.735);
        }
        for (double y : new double[] {2.8, 4.35}) {
            add(parts, Material.STEEL, 0.31, y - 0.04, 0.31, 0.69, y + 0.04, 0.69);
            for (int sector = 0; sector < 3; sector++) {
                double angle = Math.toRadians(sector * 120 + 90);
                double x = 0.5 + Math.sin(angle) * 1.02;
                double z = 0.5 + Math.cos(angle) * 1.02;
                add(parts, Material.DARK, x - 0.08, y - 0.16, z - 0.08, x + 0.08, y + 0.34, z + 0.08);
                add(parts, Material.PANEL, x - 0.12, y - 0.35, z - 0.12, x + 0.12, y + 0.8, z + 0.12);
                double innerX = 0.5 + Math.sin(angle) * 0.32;
                double innerZ = 0.5 + Math.cos(angle) * 0.32;
                add(parts, Material.DARK, innerX - 0.08, y - 0.12, innerZ - 0.06,
                        innerX + 0.08, y + 0.23, innerZ + 0.06);
            }
        }
        add(parts, Material.DARK, 0.66, 0.95, 0.6, 0.69, 4.6, 0.63);
        add(parts, Material.STEEL, 0.46, 5.70, 0.46, 0.54, 5.84, 0.54);
        return List.copyOf(parts);
    }

    private static List<TestTowerGeometry.Beam> arms() {
        var beams = new ArrayList<TestTowerGeometry.Beam>();
        for (float y : new float[] {2.8F, 4.35F}) {
            for (int sector = 0; sector < 3; sector++) {
                double angle = Math.toRadians(sector * 120 + 90);
                float x = 0.5F + (float) Math.sin(angle) * 1.02F;
                float z = 0.5F + (float) Math.cos(angle) * 1.02F;
                beams.add(new TestTowerGeometry.Beam(0.5F, y, 0.5F, x, y, z, 0.07F));
                beams.add(new TestTowerGeometry.Beam(0.5F, y - 0.28F, 0.5F, x, y, z, 0.035F));
                beams.add(new TestTowerGeometry.Beam(0.66F, y + 0.18F, 0.61F, x, y + 0.15F, z, 0.018F));
            }
        }
        return List.copyOf(beams);
    }

    private static void add(List<Part> parts, Material material,
                            double x1, double y1, double z1, double x2, double y2, double z2) {
        parts.add(new Part(material, new AABB(x1, y1, z1, x2, y2, z2)));
    }

    private static VoxelShape[] shapes() {
        var boxes = new ArrayList<>(PARTS.stream().map(Part::bounds).toList());
        boxes.add(TowerCabinets.bounds(TowerVariant.MONOPOLE, 0, false));
        boxes.add(TowerCabinets.bounds(TowerVariant.MONOPOLE, 0, true));
        boxes.add(BEACON);
        for (var beam : ARMS) {
            int steps = 12;
            for (int i = 0; i < steps; i++) {
                double a = (double) i / steps, b = (double) (i + 1) / steps;
                double half = beam.thickness() * 0.71;
                double x1 = beam.x1() + (beam.x2() - beam.x1()) * a;
                double y1 = beam.y1() + (beam.y2() - beam.y1()) * a;
                double z1 = beam.z1() + (beam.z2() - beam.z1()) * a;
                double x2 = beam.x1() + (beam.x2() - beam.x1()) * b;
                double y2 = beam.y1() + (beam.y2() - beam.y1()) * b;
                double z2 = beam.z1() + (beam.z2() - beam.z1()) * b;
                boxes.add(new AABB(Math.min(x1, x2) - half, Math.min(y1, y2) - half, Math.min(z1, z2) - half,
                        Math.max(x1, x2) + half, Math.max(y1, y2) + half, Math.max(z1, z2) + half));
            }
        }
        VoxelShape[] shapes = new VoxelShape[6 * 9];
        java.util.Arrays.fill(shapes, Shapes.empty());
        for (AABB box : boxes) {
            for (int y = Math.max(0, (int) Math.floor(box.minY)); y < Math.min(6, Math.ceil(box.maxY)); y++) {
                for (int x = Math.max(-1, (int) Math.floor(box.minX)); x < Math.min(2, Math.ceil(box.maxX)); x++) {
                    for (int z = Math.max(-1, (int) Math.floor(box.minZ)); z < Math.min(2, Math.ceil(box.maxZ)); z++) {
                        AABB clipped = box.intersect(new AABB(x, y, z, x + 1, y + 1, z + 1)).move(-x, -y, -z);
                        int index = (y * 3 + x + 1) * 3 + z + 1;
                        shapes[index] = Shapes.joinUnoptimized(shapes[index], Shapes.create(clipped), BooleanOp.OR);
                    }
                }
            }
        }
        for (int i = 0; i < shapes.length; i++) shapes[i] = shapes[i].optimize();
        return shapes;
    }
}
