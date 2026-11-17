package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TestTowerSectionBlock extends Block {
    public static final IntegerProperty OFFSET = IntegerProperty.create("offset", 0, TestTowerBlock.HEIGHT - 1);
    public static final IntegerProperty X_OFFSET = IntegerProperty.create("x_offset", 0, 4);
    public static final IntegerProperty Z_OFFSET = IntegerProperty.create("z_offset", 0, 4);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("lit");
    private static final VoxelShape LEGACY_SHAPE = Block.box(2, 0, 7, 14, 16, 14);
    private final TowerVariant variant;

    public TestTowerSectionBlock(BlockBehaviour.Properties properties) {
        this(properties, TowerVariant.TIER_3);
    }

    public TestTowerSectionBlock(BlockBehaviour.Properties properties, TowerVariant variant) {
        // Block constructs states in super(), before this.variant can be assigned.
        super(properties.lightLevel(state -> lightEmission(variant, state)));
        this.variant = variant;
        registerDefaultState(stateDefinition.any().setValue(OFFSET, 1).setValue(X_OFFSET, TestTowerGeometry.RADIUS)
                .setValue(Z_OFFSET, TestTowerGeometry.RADIUS).setValue(TestTowerBlock.WIDE, false)
                .setValue(LIT, false));
    }

    public TowerVariant variant() { return variant; }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OFFSET, X_OFFSET, Z_OFFSET, TestTowerBlock.WIDE, LIT);
    }

    public static int lightEmission(BlockState state) {
        return lightEmission(TowerVariant.of(state), state);
    }

    private static int lightEmission(TowerVariant variant, BlockState state) {
        return TowerWorkLights.emission(variant, state.getValue(TestTowerBlock.WIDE), state.getValue(LIT),
                state.getValue(X_OFFSET) - TestTowerGeometry.RADIUS, state.getValue(OFFSET),
                state.getValue(Z_OFFSET) - TestTowerGeometry.RADIUS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TestTowerBlock.WIDE)
                ? TestTowerGeometry.shape(variant, horizontalOffset(state, X_OFFSET), state.getValue(OFFSET),
                        horizontalOffset(state, Z_OFFSET))
                : LEGACY_SHAPE;
    }

    private static int horizontalOffset(BlockState state, IntegerProperty property) {
        // Legacy sections have no horizontal offsets and always belong to the block directly below.
        return state.getValue(TestTowerBlock.WIDE) ? state.getValue(property) - TestTowerGeometry.RADIUS : 0;
    }

    public static BlockPos basePosition(BlockPos pos, BlockState state) {
        return pos.offset(-horizontalOffset(state, X_OFFSET), -state.getValue(OFFSET),
                -horizontalOffset(state, Z_OFFSET));
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.phys.BlockHitResult hit) {
        return TowerCabinets.use(level, basePosition(pos, state), player, hit);
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        BlockPos base = basePosition(pos, state);
        double x = entity.getX() - base.getX();
        double z = entity.getZ() - base.getZ();
        return state.getValue(TestTowerBlock.WIDE) ? variant.onLadder(x,
                entity.getY() - base.getY(), z)
                : TestTowerGeometry.onLegacyLadder(x, z);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            TestTowerBlock.removeTower(level, basePosition(pos, state), true, !player.isCreative());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            TestTowerBlock.removeTower(level, basePosition(pos, state), true, true);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(variant.baseBlock());
    }
}
