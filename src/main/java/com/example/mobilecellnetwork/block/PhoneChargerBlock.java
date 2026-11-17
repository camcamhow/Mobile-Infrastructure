package com.example.mobilecellnetwork.block;

import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.item.CellphoneItem;
import com.example.mobilecellnetwork.network.PhoneDeviceService;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;

public final class PhoneChargerBlock extends NetworkBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 4, 14);

    public PhoneChargerBlock(BlockBehaviour.Properties properties) {
        super(properties, PhoneChargerBlockEntity::new);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!CellphoneItem.isPhone(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PhoneChargerBlockEntity charger) {
            if (!charger.getItemHandler().getStackInSlot(0).isEmpty()) {
                player.displayClientMessage(Component.literal("The charger already has a phone."), true);
            } else {
                if (player instanceof ServerPlayer serverPlayer
                        && PhoneDeviceService.activePhoneId(serverPlayer) != null
                        && PhoneDeviceService.activePhoneId(serverPlayer).equals(
                                stack.get(ModDataComponents.PHONE_ID.get()))) {
                    PhoneDeviceService.closeActiveSession(serverPlayer);
                }
                ItemStack remainder = charger.getItemHandler().insertItem(0, stack.copyWithCount(1), false);
                if (remainder.isEmpty()) {
                    stack.shrink(1);
                    charger.openChargerMenu(player);
                } else {
                    player.displayClientMessage(Component.literal("Could not place the phone on the charger."), true);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PhoneChargerBlockEntity charger) {
            ItemStack phone = charger.getItemHandler().extractItem(0, 1, false);
            if (phone.isEmpty()) {
                return super.useWithoutItem(state, level, pos, player, hit);
            } else if (!player.getInventory().add(phone)) {
                player.drop(phone, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
