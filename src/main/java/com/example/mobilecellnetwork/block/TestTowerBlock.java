package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.example.mobilecellnetwork.registry.ModBlocks;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class TestTowerBlock extends NetworkBlock {
    public static final int HEIGHT = 10;
    private static final ThreadLocal<Set<BlockPos>> REMOVING = ThreadLocal.withInitial(HashSet::new);

    public TestTowerBlock(BlockBehaviour.Properties properties) {
        super(properties, TestAntennaBlockEntity::new);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos base = context.getClickedPos();
        for (int i = 1; i < HEIGHT; i++) {
            BlockPos pos = base.above(i);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || (context.getPlayer() != null && !level.mayInteract(context.getPlayer(), pos))
                    || !level.isEmptyBlock(pos)
                    || !level.isUnobstructed(ModBlocks.TEST_TOWER_SECTION.get().defaultBlockState(), pos,
                            net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "The test tower needs nine clear blocks above its base."), true);
                }
                return null;
            }
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            for (int i = 1; i < HEIGHT; i++) {
                level.setBlock(pos.above(i), ModBlocks.TEST_TOWER_SECTION.get().defaultBlockState()
                        .setValue(TestTowerSectionBlock.OFFSET, i), 3);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            removeTower(level, pos, false, false);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static void removeTower(Level level, BlockPos base, boolean destroyBase, boolean drop) {
        Set<BlockPos> removing = REMOVING.get();
        if (!removing.add(base.immutable())) return;
        try {
            if (destroyBase && level.getBlockState(base).is(ModBlocks.TEST_ANTENNA.get())) {
                level.destroyBlock(base, drop);
            }
            for (int i = 1; i < HEIGHT; i++) {
                BlockPos pos = base.above(i);
                BlockState section = level.getBlockState(pos);
                if (section.is(ModBlocks.TEST_TOWER_SECTION.get())
                        && section.getValue(TestTowerSectionBlock.OFFSET) == i) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            removing.remove(base);
            if (removing.isEmpty()) REMOVING.remove();
        }
    }
}
