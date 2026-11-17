package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DiamondAntennaBlockEntity extends AntennaBlockEntity {
    public DiamondAntennaBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.DIAMOND_ANTENNA.get(), position, state, AntennaTier.DIAMOND);
    }
}
