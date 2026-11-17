package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.AntennaTier;
import com.example.mobilecellnetwork.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum TowerVariant {
    TIER_1(10, 256, AntennaTier.COPPER),
    TIER_2(15, 512, AntennaTier.IRON),
    TIER_3(20, 1024, AntennaTier.DIAMOND),
    MONOPOLE(6, 128, AntennaTier.COPPER);

    private final int height;
    private final int range;
    private final AntennaTier radio;

    TowerVariant(int height, int range, AntennaTier radio) {
        this.height = height;
        this.range = range;
        this.radio = radio;
    }

    public int height() { return height; }
    public int range() { return range; }
    public AntennaTier radio() { return radio; }
    public boolean monopole() { return this == MONOPOLE; }
    public int radius() { return monopole() ? 1 : 2; }
    public int displayTier() { return monopole() ? 1 : ordinal() + 2; }
    public int cabinetCount() { return monopole() ? 1 : 3; }
    public boolean separateComputerDeck() { return this == TIER_1 || this == TIER_2; }
    public boolean hasCabinet(int index) {
        return index >= 0 && index < cabinetCount();
    }
    public boolean hasTerminal(int index) {
        return index >= 0 && index < cabinetCount() && !(separateComputerDeck() && index == 2);
    }
    public com.example.mobilecellnetwork.network.SignalTier maxSignalTier() {
        return monopole() ? com.example.mobilecellnetwork.network.SignalTier.TWO_G : radio.maxSignalTier();
    }

    public boolean onLadder(double x, double y, double z) {
        return monopole() ? MonopoleGeometry.onLadder(x, y, z)
                : TestTowerGeometry.onLadder(x, modelY(y), z);
    }

    // Keep the foundation/cabinets and both upper decks full size; shorten only the lower mast.
    public double y(double original) {
        if (this == TIER_3 || monopole()) return original;
        if (original <= 3) return original;
        if (original >= 14) return original - (20 - height);
        return 3 + (original - 3) * mastScale();
    }

    public double modelY(double world) {
        if (this == TIER_3 || monopole()) return world;
        if (world <= 3) return world;
        if (world >= height - 6) return world + 20 - height;
        return 3 + (world - 3) / mastScale();
    }

    public double mastScale() { return (height - 9.0) / 11.0; }
    public Vec3 toModel(Vec3 local) { return new Vec3(local.x, modelY(local.y), local.z); }
    public Vec3 toWorld(Vec3 local) { return new Vec3(local.x, y(local.y), local.z); }
    public AABB bounds(AABB box) {
        return new AABB(box.minX, y(box.minY), box.minZ, box.maxX, y(box.maxY), box.maxZ);
    }

    public Block baseBlock() {
        return switch (this) {
            case TIER_1 -> ModBlocks.TOWER_TIER_1.get();
            case TIER_2 -> ModBlocks.TOWER_TIER_2.get();
            case TIER_3 -> ModBlocks.TEST_ANTENNA.get();
            case MONOPOLE -> ModBlocks.MONOPOLE_TOWER.get();
        };
    }

    public TestTowerSectionBlock sectionBlock() {
        return switch (this) {
            case TIER_1 -> ModBlocks.TOWER_TIER_1_SECTION.get();
            case TIER_2 -> ModBlocks.TOWER_TIER_2_SECTION.get();
            case TIER_3 -> ModBlocks.TEST_TOWER_SECTION.get();
            case MONOPOLE -> ModBlocks.MONOPOLE_TOWER_SECTION.get();
        };
    }

    public static TowerVariant of(BlockState state) {
        if (state.getBlock() instanceof TestTowerBlock tower) return tower.variant();
        if (state.getBlock() instanceof TestTowerSectionBlock section) return section.variant();
        throw new IllegalArgumentException("Not a tower block: " + state);
    }
}
