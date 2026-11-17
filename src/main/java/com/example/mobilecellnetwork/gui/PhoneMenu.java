package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.registry.ModItems;
import com.example.mobilecellnetwork.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class PhoneMenu extends AbstractContainerMenu {
    private final PhoneData phoneData;
    private java.util.UUID deviceId;

    public PhoneMenu(int containerId, Inventory playerInventory) {
        this(ModMenus.PHONE_MENU.get(), containerId, playerInventory);
    }

    protected PhoneMenu(MenuType<?> menuType, int containerId, Inventory playerInventory) {
        super(menuType, containerId);
        phoneData = playerInventory.player.getData(ModCapabilities.PHONE_DATA);
    }

    public PhoneData phoneData() {
        return phoneData;
    }

    public void setDeviceId(java.util.UUID deviceId) {
        this.deviceId = deviceId;
    }

    public boolean hasCellphone(Player player) {
        if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(player.getMainHandItem())
                || com.example.mobilecellnetwork.item.CellphoneItem.isPhone(player.getOffhandItem())) {
            return true;
        }
        return player.getInventory().items.stream().anyMatch(com.example.mobilecellnetwork.item.CellphoneItem::isPhone);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && hasCellphone(player)
                && (deviceId == null || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                || (deviceId.equals(com.example.mobilecellnetwork.network.PhoneDeviceService.activePhoneId(serverPlayer))
                && com.example.mobilecellnetwork.network.PhoneDeviceService.isPhoneInInventory(serverPlayer, deviceId)));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            java.util.UUID activeId =
                    com.example.mobilecellnetwork.network.PhoneDeviceService.activePhoneId(serverPlayer);
            if (activeId != null) {
                com.example.mobilecellnetwork.network.PhoneDeviceService.closeSession(
                        serverPlayer, activeId, containerId);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
