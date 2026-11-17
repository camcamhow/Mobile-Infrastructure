package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity;
import com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PowerTapBlock extends NetworkBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(2, 0, 2, 14, 2, 14),
            box(3, 2, 3, 13, 14, 13), box(6, 14, 6, 10, 16, 10));

    public PowerTapBlock(BlockBehaviour.Properties properties) {
        super(properties, PowerTapBlockEntity::new);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PowerTapBlockEntity tap) {
            TechnicianDiagnostics.tap(player, tap);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
