package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.EmergencyDispatchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class EmergencyDispatchBlock extends Block implements EntityBlock {
    public EmergencyDispatchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new EmergencyDispatchBlockEntity(position, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position,
                                               Player player, BlockHitResult hit) {
        if (!player.hasPermissions(2)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal(
                        "You need administrator permission to use the Emergency Dispatch."), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer admin)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(position) instanceof EmergencyDispatchBlockEntity dispatch) {
            dispatch.openDispatchMenu(admin);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
