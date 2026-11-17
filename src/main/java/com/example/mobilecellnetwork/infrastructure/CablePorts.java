package com.example.mobilecellnetwork.infrastructure;

import com.example.mobilecellnetwork.block.TestTowerSectionBlock;
import com.example.mobilecellnetwork.block.TowerCabinets;
import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity;
import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class CablePorts {
    public enum Socket {
        ANTENNA(new Vec3(0.5, 1.5 / 16, 11.0 / 16), Direction.SOUTH),
        BOOSTER(new Vec3(0.5, 5.1 / 16, 11.02 / 16), Direction.SOUTH),
        CHARGER(new Vec3(0.5, 0.8 / 16, 14.02 / 16), Direction.SOUTH),
        TOWER(new Vec3(0.85, 0.65, 0.5), Direction.EAST),
        MONOPOLE(new Vec3(0.85, 0.65, 0.5).add(TowerCabinets.MONOPOLE_OFFSET), Direction.EAST),
        POWER_TAP(new Vec3(0.5, 1, 0.5), Direction.UP);

        private final Vec3 mount;
        private final Direction facing;

        Socket(Vec3 mount, Direction facing) {
            this.mount = mount;
            this.facing = facing;
        }

        public Vec3 mount() { return mount; }
        public Direction facing() { return facing; }
        public Vec3 point() {
            return mount.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.065));
        }
        public Vec3 at(BlockPos pos) { return point().add(Vec3.atLowerCornerOf(pos)); }
    }

    private CablePorts() {}

    public static Socket socket(BlockEntity block) {
        if (block instanceof TestAntennaBlockEntity tower && tower.isWideTower()) {
            return tower.variant().monopole() ? Socket.MONOPOLE : Socket.TOWER;
        }
        if (block instanceof AntennaBlockEntity) return Socket.ANTENNA;
        if (block instanceof SignalBoosterBlockEntity) return Socket.BOOSTER;
        if (block instanceof PhoneChargerBlockEntity) return Socket.CHARGER;
        if (block instanceof PowerTapBlockEntity) return Socket.POWER_TAP;
        throw new IllegalArgumentException("Equipment has no cable socket: " + block.getBlockPos());
    }

    public static Vec3 point(BlockEntity block) {
        return socket(block).at(block.getBlockPos());
    }

    public static Vec3 sourcePoint(Level level, BlockPos pos, Vec3 clicked) {
        return level.getBlockEntity(pos) instanceof PowerTapBlockEntity ? Socket.POWER_TAP.at(pos) : clicked;
    }

    public static Vec3 previewPoint(Level level, BlockHitResult hit) {
        BlockPos clicked = hit.getBlockPos();
        var state = level.getBlockState(clicked);
        BlockPos base = state.getBlock() instanceof TestTowerSectionBlock
                ? TestTowerSectionBlock.basePosition(clicked, state) : clicked;
        BlockEntity block = level.getBlockEntity(base);
        if (!(block instanceof WiredDevice)) return hit.getLocation();
        if (block instanceof TestAntennaBlockEntity tower && tower.isWideTower()
                && TowerCabinets.at(tower.variant(), tower.variant().toModel(
                        hit.getLocation().subtract(Vec3.atLowerCornerOf(base)))) != 0) {
            return hit.getLocation();
        }
        return point(block);
    }
}
