package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.registry.ModMenus;
import com.example.mobilecellnetwork.block.TowerCabinets;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.core.BlockPos;

public class AntennaMenu extends AbstractContainerMenu {
    private final AntennaBlockEntity antenna;
    private final BlockPos blockPos;
    private final ContainerData data;
    private int terminalIndex = -1;

    public AntennaMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, BlockPos.ZERO);
    }

    public AntennaMenu(int containerId, Inventory inventory, AntennaBlockEntity antenna) {
        this(containerId, inventory, antenna, antenna == null ? BlockPos.ZERO : antenna.getBlockPos());
    }

    public AntennaMenu(int containerId, Inventory inventory, TestAntennaBlockEntity antenna, int terminalIndex) {
        this(containerId, inventory, antenna);
        if (!antenna.variant().hasTerminal(terminalIndex)) {
            throw new IllegalArgumentException("Invalid tower terminal: " + terminalIndex);
        }
        this.terminalIndex = terminalIndex;
    }

    private AntennaMenu(int containerId, Inventory inventory, AntennaBlockEntity antenna, BlockPos blockPos) {
        super(ModMenus.ANTENNA_MENU.get(), containerId);
        this.antenna = antenna;
        this.blockPos = blockPos.immutable();
        this.data = antenna == null ? new SimpleContainerData(12) : new ContainerData() {
            @Override
            public int get(int index) {
                int energy = antenna.getEnergyStored();
                int capacity = antenna.getMaxEnergyStored();
                int draw = antenna.getAntennaDrawPerSecond();
                return switch (index) {
                    case 0 -> energy & 0x7fff;
                    case 1 -> energy >>> 15;
                    case 2 -> capacity & 0x7fff;
                    case 3 -> capacity >>> 15;
                    case 4 -> draw & 0x7fff;
                    case 5 -> draw >>> 15;
                    case 6 -> antenna.getPowerState().ordinal();
                    case 7 -> antenna.getTier().ordinal();
                    case 8 -> antenna.getUserSet().size();
                    case 9 -> antenna.isChunkLoadedByAntenna() ? 1 : 0;
                    case 10 -> antenna.getRange();
                    case 11 -> antenna.installation().status().ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 12;
            }
        };
        addDataSlots(data);
    }

    public int getDataValue(int index) {
        return data.get(index);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public static AntennaMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf data) {
        BlockPos position = data.readBlockPos();
        return new AntennaMenu(containerId, inventory, null, position);
    }

    @Override
    public boolean stillValid(Player player) {
        if (antenna != null && terminalIndex >= 0) {
            return !antenna.isRemoved() && antenna.getLevel() == player.level()
                    && antenna instanceof TestAntennaBlockEntity tower && tower.isWideTower()
                    && TowerCabinets.withinTerminalRange(player.position(),
                            tower.variant().toWorld(TowerCabinets.bounds(tower.variant(), terminalIndex, true).getCenter())
                                    .add(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        }
        return antenna == null || antenna.isRemoved()
                ? player.level().getBlockEntity(blockPos) instanceof AntennaBlockEntity
                : antenna.getLevel() == player.level()
                && player.distanceToSqr(antenna.getBlockPos().getX() + 0.5D,
                antenna.getBlockPos().getY() + 0.5D, antenna.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int slotIndex) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
