package com.example.mobilecellnetwork.block;

import java.util.function.BiFunction;
import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import com.example.mobilecellnetwork.gui.AntennaMenu;
import com.example.mobilecellnetwork.gui.PhoneChargerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NetworkBlock extends Block implements EntityBlock {
    private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory;

    public NetworkBlock(
            BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory
    ) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return blockEntityFactory.apply(position, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos position, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, position, state, placer, stack);
        if (!level.isClientSide
                && com.example.mobilecellnetwork.config.MobileCellNetworkConfig.REQUIRE_INSTALLATION.get()
                && level.getBlockEntity(position) instanceof com.example.mobilecellnetwork.infrastructure.Serviceable device) {
            device.installation().begin(placer instanceof Player player ? player.getUUID() : null);
            level.getBlockEntity(position).setChanged();
            level.sendBlockUpdated(position, state, state, 3);
        }
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(position) instanceof AntennaBlockEntity antenna) {
            antenna.setOwner(player.getUUID(), player.getGameProfile().getName());
            antenna.setRoamingPolicy(com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                    .DEFAULT_ANTENNA_ROAMING_POLICY.get());
        }
        if (!level.isClientSide && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof ServerBlockEntity serverBlock) {
            String networkId = com.example.mobilecellnetwork.network.ServerBlockPlaceHandler
                    .assignOrCreate(serverPlayer, position);
            serverBlock.addManagedNetwork(networkId);
            serverBlock.openServerMenu(serverPlayer);
        }
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType
    ) {
        return (tickerLevel, position, tickerState, blockEntity) -> {
            if (blockEntity instanceof AntennaBlockEntity antenna) {
                antenna.onBlockTick();
            } else if (blockEntity instanceof com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity charger) {
                charger.tick();
            } else if (blockEntity instanceof com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity booster) {
                booster.tick();
            } else if (blockEntity instanceof ServerBlockEntity server) {
                server.tick();
            } else if (blockEntity instanceof com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity tap) {
                tap.tick();
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos position, Player player, BlockHitResult hit
    ) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof ServerBlockEntity serverBlockEntity) {
            if (!level.isClientSide) {
                serverBlockEntity.openServerMenu(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (blockEntity instanceof com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity booster) {
                booster.openMenu(serverPlayer);
                return InteractionResult.CONSUME;
            }
            if (blockEntity instanceof AntennaBlockEntity antenna) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, user) -> new AntennaMenu(id, inventory, antenna),
                        Component.translatable("gui.mobilecellnetwork.antenna")),
                        buffer -> buffer.writeBlockPos(position));
                return InteractionResult.CONSUME;
            }
            if (blockEntity instanceof com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity charger) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, user) -> new PhoneChargerMenu(id, inventory, charger),
                        Component.translatable("gui.mobilecellnetwork.phone_charger")));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos position, BlockState newState,
                            boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(position)
                instanceof com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity charger) {
            net.minecraft.world.item.ItemStack phone = charger.getItemHandler().getStackInSlot(0);
            if (!phone.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity dropped = new net.minecraft.world.entity.item.ItemEntity(
                        level, position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D,
                        phone.copy());
                level.addFreshEntity(dropped);
                charger.getItemHandler().setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && level.getBlockEntity(position) instanceof AntennaBlockEntity) {
            com.example.mobilecellnetwork.data.NetworkSavedData.get(serverLevel.getServer().overworld())
                    .unregisterAntenna(serverLevel.dimension().location(), position);
        }
        if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && level.getBlockEntity(position) instanceof ServerBlockEntity) {
            com.example.mobilecellnetwork.network.ServerBlockPlaceHandler.onServerBlockBroken(serverLevel, position);
        }
        super.onRemove(state, level, position, newState, movedByPiston);
    }
}
