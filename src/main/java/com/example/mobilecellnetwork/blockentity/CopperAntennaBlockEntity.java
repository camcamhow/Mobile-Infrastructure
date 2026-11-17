package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CopperAntennaBlockEntity extends AntennaBlockEntity {
    public CopperAntennaBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.COPPER_ANTENNA.get(), position, state, AntennaTier.COPPER);
    }
}
