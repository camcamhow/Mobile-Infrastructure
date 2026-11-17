package com.example.mobilecellnetwork.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class TowerWorkLights {
    public record Fixture(BlockPos cell, AABB housing, AABB lens) {}

    public static final List<Fixture> FIXTURES = fixtures();
    public static final List<BlockPos> UPDATE_CELLS = updateCells();

    private TowerWorkLights() {}

    private static List<Fixture> fixtures() {
        List<Fixture> fixtures = new ArrayList<>();
        for (int deck : new int[] {TestTowerBlock.HEIGHT - 6, TestTowerBlock.HEIGHT - 3}) {
            for (double x : new double[] {-1.3, 2.3}) {
                for (double z : new double[] {-1.3, 2.3}) {
                    fixtures.add(new Fixture(BlockPos.containing(x, deck - 0.2, z),
                            new AABB(x - 0.15, deck - 0.18, z - 0.15, x + 0.15, deck, z + 0.15),
                            new AABB(x - 0.12, deck - 0.2, z - 0.12, x + 0.12, deck - 0.18, z + 0.12)));
                }
            }
        }
        return List.copyOf(fixtures);
    }

    private static List<BlockPos> updateCells() {
        List<BlockPos> cells = new ArrayList<>();
        for (Fixture fixture : FIXTURES) cells.add(fixture.cell());
        // Clear saved LIT states left by the removed railing and mast floodlights.
        for (int y : new int[] {3, 8, 15, 18}) {
            int[] sides = y < 10 ? new int[] {-1, 1} : new int[] {-2, 2};
            for (int x : sides) {
                for (int z : sides) cells.add(new BlockPos(x, y, z));
            }
        }
        return List.copyOf(cells);
    }

    public static int emission(boolean wide, boolean lit, int x, int y, int z) {
        return emission(TowerVariant.TIER_3, wide, lit, x, y, z);
    }

    public static List<BlockPos> updateCells(TowerVariant variant) {
        if (variant.monopole()) return List.of();
        if (variant == TowerVariant.TIER_3) return UPDATE_CELLS;
        return FIXTURES.stream().map(fixture -> cell(variant, fixture)).toList();
    }

    public static BlockPos cell(TowerVariant variant, Fixture fixture) {
        return BlockPos.containing(fixture.lens().getCenter().x, variant.y(fixture.lens().minY),
                fixture.lens().getCenter().z);
    }

    public static int emission(TowerVariant variant, boolean wide, boolean lit, int x, int y, int z) {
        if (!wide || !lit || variant.monopole()) return 0;
        for (Fixture fixture : FIXTURES) {
            BlockPos cell = cell(variant, fixture);
            if (cell.getX() == x && cell.getY() == y && cell.getZ() == z) return 15;
        }
        return 0;
    }
}
