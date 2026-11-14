package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.registry.ModItems;
import com.example.mobilecellnetwork.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PhoneChargerMenu extends AbstractContainerMenu {
    private final PhoneChargerBlockEntity charger;
    private final ContainerData data;

    public PhoneChargerMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, createClientHandler(), null);
    }

    public PhoneChargerMenu(int containerId, Inventory inventory, PhoneChargerBlockEntity charger) {
        this(containerId, inventory, charger.getItemHandler(), charger);
    }

    private PhoneChargerMenu(int containerId, Inventory inventory, ItemStackHandler handler,
                             PhoneChargerBlockEntity charger) {
        super(ModMenus.PHONE_CHARGER_MENU.get(), containerId);
        this.charger = charger;
        this.data = charger == null ? new SimpleContainerData(7) : new ContainerData() {
            @Override
            public int get(int index) {
                int energy = charger.getEnergyStored();
                int capacity = charger.getMaxEnergyStored();
                int phoneCharge = charger.getPhoneCharge();
                return switch (index) {
                    case 0 -> energy & 0xffff;
                    case 1 -> energy >>> 16;
                    case 2 -> capacity & 0xffff;
                    case 3 -> capacity >>> 16;
                    case 4 -> phoneCharge & 0xffff;
                    case 5 -> phoneCharge >>> 16;
                    case 6 -> switch (charger.getChargeStatus()) {
                        case "Charging" -> 1;
                        case "No FE" -> 2;
                        default -> 0;
                    };
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 7;
            }
        };
        addSlot(new SlotItemHandler(handler, 0, 80, 35));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    private static ItemStackHandler createClientHandler() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ModItems.CELLPHONE.get());
            }
        };
    }

    public int getDataValue(int index) {
        return data.get(index);
    }

    public static PhoneChargerMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf data) {
        return new PhoneChargerMenu(containerId, inventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return charger == null || charger.isRemoved()
                ? charger == null
                : charger.getLevel() == player.level()
                && player.distanceToSqr(charger.getBlockPos().getX() + 0.5D,
                charger.getBlockPos().getY() + 0.5D, charger.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (slotIndex == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(ModItems.CELLPHONE.get())) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (slotIndex < 28) {
            if (!moveItemStackTo(stack, 28, slots.size(), false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
