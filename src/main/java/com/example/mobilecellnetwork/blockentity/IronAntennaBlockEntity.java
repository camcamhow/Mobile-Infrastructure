package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class IronAntennaBlockEntity extends AntennaBlockEntity {
    public IronAntennaBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.IRON_ANTENNA.get(), position, state, AntennaTier.IRON);
    }
}
