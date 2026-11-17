package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.EmergencyDispatchBlockEntity;
import com.example.mobilecellnetwork.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class EmergencyDispatchMenu extends AbstractContainerMenu {
    private final BlockPos position;

    public EmergencyDispatchMenu(int containerId, Inventory inventory, BlockPos position) {
        super(ModMenus.EMERGENCY_DISPATCH_MENU.get(), containerId);
        this.position = position.immutable();
    }

    public static EmergencyDispatchMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        return new EmergencyDispatchMenu(containerId, inventory, buffer.readBlockPos());
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer admin && admin.hasPermissions(2)
                && player.level().getBlockEntity(position) instanceof EmergencyDispatchBlockEntity dispatch
                && !dispatch.isRemoved()
                && player.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D,
                position.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
