package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.registry.ModMenus;
import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ServerMenu extends AbstractContainerMenu {
    private final BlockPos position;
    private final BlockEntity serverBlock;

    public ServerMenu(int containerId, Inventory inventory, BlockPos position) {
        this(containerId, inventory, position, inventory.player.level().getBlockEntity(position));
    }

    private ServerMenu(int containerId, Inventory inventory, BlockPos position, BlockEntity blockEntity) {
        super(ModMenus.SERVER_MENU.get(), containerId);
        this.position = position.immutable();
        this.serverBlock = blockEntity;
    }

    public BlockPos position() {
        return position;
    }

    public static ServerMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        return new ServerMenu(containerId, inventory, buffer.readBlockPos());
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && ServerBlockEntity.canOpenServerMenu(serverPlayer)
                && serverBlock != null && !serverBlock.isRemoved()
                && serverBlock.getLevel() == player.level()
                && player.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D,
                position.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
