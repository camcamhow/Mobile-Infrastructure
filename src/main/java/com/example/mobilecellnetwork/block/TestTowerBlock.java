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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TestTowerBlock extends NetworkBlock {
    public static final int LEGACY_HEIGHT = 10;
    public static final int HEIGHT = 20;
    public static final BooleanProperty WIDE = BooleanProperty.create("wide");
    private static final ThreadLocal<Set<BlockPos>> REMOVING = ThreadLocal.withInitial(HashSet::new);
    private final TowerVariant variant;

    public TestTowerBlock(BlockBehaviour.Properties properties) {
        this(properties, TowerVariant.TIER_3);
    }

    public TestTowerBlock(BlockBehaviour.Properties properties, TowerVariant variant) {
        super(properties, TestAntennaBlockEntity::new);
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(WIDE, false));
    }

    public TowerVariant variant() { return variant; }
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        var result = TowerCabinets.use(level, pos, player, hit);
        if (result.consumesAction()) return result;
        if (state.getValue(WIDE)) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.mobilecellnetwork.technician.use_terminal"), true);
            }
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WIDE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WIDE) ? TestTowerGeometry.shape(variant, 0, 0, 0)
                : Shapes.or(Block.box(1, 0, 1, 15, 5, 15), Block.box(3, 5, 7, 13, 16, 13));
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return state.getValue(WIDE)
                ? variant.onLadder(entity.getX() - pos.getX(), entity.getY() - pos.getY(),
                        entity.getZ() - pos.getZ())
                : TestTowerGeometry.onLegacyLadder(entity.getX() - pos.getX(), entity.getZ() - pos.getZ());
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos base = context.getClickedPos();
        int radius = variant.radius();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-radius, 0, -radius),
                base.offset(radius, variant.height() - 1, radius))) {
            if (pos.equals(base)) continue;
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || (context.getPlayer() != null && !level.mayInteract(context.getPlayer(), pos))
                    || (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(pos,
                            context.getClickedFace(), context.getItemInHand()))
                    || !level.isEmptyBlock(pos)
                    || !level.isUnobstructed(sectionState(base, pos), pos,
                            net.minecraft.world.phys.shapes.CollisionContext.empty())) {
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "This tower needs a clear " + (radius * 2 + 1) + " by " + (radius * 2 + 1) + " area, " + variant.height()
                                    + " blocks tall, centered on its base."), true);
                }
                return null;
            }
        }
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        BlockState wideState = state.setValue(WIDE, true);
        if (!level.isUnobstructed(wideState, base, CollisionContext.empty())) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "The test tower foundation is blocked by an entity."), true);
            }
            return null;
        }
        return wideState;
    }

    private BlockState sectionState(BlockPos base, BlockPos section) {
        return variant.sectionBlock().defaultBlockState().setValue(WIDE, true)
                .setValue(TestTowerSectionBlock.OFFSET, section.getY() - base.getY())
                .setValue(TestTowerSectionBlock.X_OFFSET, section.getX() - base.getX() + TestTowerGeometry.RADIUS)
                .setValue(TestTowerSectionBlock.Z_OFFSET, section.getZ() - base.getZ() + TestTowerGeometry.RADIUS);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            int radius = variant.radius();
            for (BlockPos section : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius),
                    pos.offset(radius, variant.height() - 1, radius))) {
                if (section.equals(pos)) continue;
                if (!level.setBlock(section, sectionState(pos, section), 3)) {
                    com.mojang.logging.LogUtils.getLogger().warn("Failed to place test tower section at {}", section);
                    removeTower(level, pos, true, false);
                    if (placer instanceof net.minecraft.world.entity.player.Player player) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "The test tower could not be placed; its incomplete structure was removed."), true);
                    }
                    return;
                }
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            int height = level.getBlockEntity(pos) instanceof TestAntennaBlockEntity antenna
                    ? antenna.getTowerHeight() : variant.height();
            removeTower(level, pos, false, false, state.getValue(WIDE), height, variant.radius());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    public static void removeTower(Level level, BlockPos base, boolean destroyBase, boolean drop) {
        BlockState baseState = level.getBlockState(base);
        if (!(baseState.getBlock() instanceof TestTowerBlock tower)) return;
        int height = level.getBlockEntity(base) instanceof TestAntennaBlockEntity antenna
                ? antenna.getTowerHeight() : tower.variant().height();
        removeTower(level, base, destroyBase, drop, baseState.getValue(WIDE), height, tower.variant().radius());
    }

    private static void removeTower(Level level, BlockPos base, boolean destroyBase, boolean drop,
                                    boolean wide, int height, int towerRadius) {
        Set<BlockPos> removing = REMOVING.get();
        if (!removing.add(base.immutable())) return;
        try {
            if (destroyBase && level.getBlockState(base).getBlock() instanceof TestTowerBlock) {
                level.destroyBlock(base, drop);
            }
            int radius = wide ? towerRadius : 0;
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-radius, 0, -radius),
                    base.offset(radius, height - 1, radius))) {
                if (pos.equals(base)) continue;
                BlockState section = level.getBlockState(pos);
                if (section.getBlock() instanceof TestTowerSectionBlock
                        && section.getValue(WIDE) == wide
                        && TestTowerSectionBlock.basePosition(pos, section).equals(base)) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            removing.remove(base);
            if (removing.isEmpty()) REMOVING.remove();
        }
    }
}
