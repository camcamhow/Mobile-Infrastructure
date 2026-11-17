package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.registry.ModMenus;
import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ServerMenu extends AbstractContainerMenu {
    private final BlockPos position;
    private final BlockEntity serverBlock;
    private final ContainerData powerData;

    public ServerMenu(int containerId, Inventory inventory, BlockPos position) {
        this(containerId, inventory, position, inventory.player.level().getBlockEntity(position));
    }

    private ServerMenu(int containerId, Inventory inventory, BlockPos position, BlockEntity blockEntity) {
        super(ModMenus.SERVER_MENU.get(), containerId);
        this.position = position.immutable();
        this.serverBlock = blockEntity;
        this.powerData = blockEntity instanceof ServerBlockEntity rack
                && !inventory.player.level().isClientSide ? new ContainerData() {
                    public int get(int index) {
                        return switch (index) {
                            case 0 -> rack.getEnergyStored();
                            case 1 -> rack.isPowered() ? 1 : 0;
                            case 2 -> com.example.mobilecellnetwork.blockentity.InfrastructurePower
                                    .bypassed(rack.getLevel()) ? 1 : 0;
                            default -> 0;
                        };
                    }
                    public void set(int index, int value) { }
                    public int getCount() { return 3; }
                } : new SimpleContainerData(3);
        addDataSlots(powerData);
    }

    public int powerData(int index) {
        return powerData.get(index);
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
