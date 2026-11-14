package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class TestAntennaBlockEntity extends AntennaBlockEntity {
    public TestAntennaBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.TEST_ANTENNA.get(), position, state, AntennaTier.DIAMOND);
    }

}
