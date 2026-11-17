package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShopTerminalBlockEntity extends BlockEntity {
    public ShopTerminalBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SHOP_TERMINAL.get(), position, state);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // TODO: Load shop terminal configuration.
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // TODO: Save shop terminal configuration.
    }
}
