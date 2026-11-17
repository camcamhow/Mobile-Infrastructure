package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public final class SignalBoosterBlock extends NetworkBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 3, 12),
            Block.box(4.99, 3, 4.99, 11.01, 11, 11.01), Block.box(7, 11, 7, 9, 15, 9),
            Block.box(6.5, 11, 6.5, 9.5, 12, 9.5),
            Block.box(6, 6, 4, 10, 9, 5));

    public SignalBoosterBlock(BlockBehaviour.Properties properties) {
        super(properties, SignalBoosterBlockEntity::new);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
