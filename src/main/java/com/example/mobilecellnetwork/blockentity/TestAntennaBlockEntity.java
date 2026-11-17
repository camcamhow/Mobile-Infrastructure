package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import com.example.mobilecellnetwork.block.TestTowerBlock;
import com.example.mobilecellnetwork.block.TestTowerSectionBlock;
import com.example.mobilecellnetwork.block.TowerWorkLights;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

public final class TestAntennaBlockEntity extends AntennaBlockEntity {
    private int towerHeight = TestTowerBlock.HEIGHT;
    private int openCabinets;
    private boolean workLightsPowered;

    public boolean areWorkLightsPowered() { return workLightsPowered; }

    @Override
    public void onBlockTick() {
        super.onBlockTick();
        if (level == null || level.isClientSide || !isWideTower() || level.getGameTime() % 20 != 0) return;
        boolean powered = isEnabled() && (InfrastructurePower.bypassed(level) || getEnergyStored() > 0 || isPowered());
        if (workLightsPowered != powered) {
            workLightsPowered = powered;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        for (BlockPos cell : TowerWorkLights.updateCells(variant())) {
            BlockPos pos = worldPosition.offset(cell);
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            boolean lit = TowerWorkLights.emission(variant(), true, powered, cell.getX(), cell.getY(), cell.getZ()) > 0;
            if (state.getBlock() instanceof TestTowerSectionBlock && state.getValue(TestTowerBlock.WIDE)
                    && TestTowerSectionBlock.basePosition(pos, state).equals(worldPosition)
                    && state.getValue(TestTowerSectionBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(TestTowerSectionBlock.LIT, lit), 3);
            }
        }
    }

    public boolean isCabinetOpen(int cabinet) {
        return variant().hasCabinet(cabinet)
                && (openCabinets & (1 << cabinet)) != 0;
    }

    public int getOpenCabinets() { return openCabinets; }

    public void toggleCabinet(int cabinet) {
        if (!variant().hasCabinet(cabinet)) {
            throw new IllegalArgumentException("Invalid tower cabinet: " + cabinet);
        }
        openCabinets ^= 1 << cabinet;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public TestAntennaBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.TEST_ANTENNA.get(), position, state,
                com.example.mobilecellnetwork.block.TowerVariant.of(state).radio());
        towerHeight = variant().height();
    }

    public com.example.mobilecellnetwork.block.TowerVariant variant() {
        return com.example.mobilecellnetwork.block.TowerVariant.of(getBlockState());
    }

    @Override public int getRange() { return variant().range(); }
    @Override public com.example.mobilecellnetwork.network.SignalTier getMaxSignalTier() {
        return variant().maxSignalTier();
    }
    public int getTowerHeight() {
        return towerHeight;
    }

    public boolean isWideTower() {
        return getBlockState().getValue(TestTowerBlock.WIDE);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        towerHeight = readHeight(tag);
        openCabinets = tag.getInt("openCabinets") & com.example.mobilecellnetwork.block.TowerCabinets.ALL_OPEN;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("towerHeight", towerHeight);
        tag.putInt("openCabinets", openCabinets);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("towerHeight", towerHeight);
        tag.putInt("openCabinets", openCabinets);
        tag.putBoolean("workLightsPowered", workLightsPowered);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        towerHeight = readHeight(tag);
        openCabinets = tag.getInt("openCabinets") & com.example.mobilecellnetwork.block.TowerCabinets.ALL_OPEN;
        workLightsPowered = tag.getBoolean("workLightsPowered");
    }

    private int readHeight(CompoundTag tag) {
        if (variant() != com.example.mobilecellnetwork.block.TowerVariant.TIER_3) return variant().height();
        // Older towers have no height tag and must not grow into nearby builds.
        if (!tag.contains("towerHeight")) {
            return TestTowerBlock.LEGACY_HEIGHT;
        }
        if (tag.contains("towerHeight", Tag.TAG_INT)) {
            int height = tag.getInt("towerHeight");
            if (height == TestTowerBlock.HEIGHT || height == TestTowerBlock.LEGACY_HEIGHT) {
                return height;
            }
        }
        com.mojang.logging.LogUtils.getLogger().warn("Invalid test tower height {}; using legacy height",
                tag.get("towerHeight"));
        return TestTowerBlock.LEGACY_HEIGHT;
    }
}
