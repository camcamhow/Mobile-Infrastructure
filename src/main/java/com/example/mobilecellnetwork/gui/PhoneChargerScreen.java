package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PhoneChargerScreen extends AbstractContainerScreen<PhoneChargerMenu> {
    private final Inventory playerInventory;

    public PhoneChargerScreen(PhoneChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        playerInventory = inventory;
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20242B);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFF363C46);
        int energy = menu.getDataValue(0) | menu.getDataValue(1) << 16;
        int capacity = menu.getDataValue(2) | menu.getDataValue(3) << 16;
        int barWidth = capacity == 0 ? 0 : Math.min(72, energy * 72 / capacity);
        graphics.fill(leftPos + 98, topPos + 36, leftPos + 170, topPos + 44, 0xFF16191D);
        graphics.fill(leftPos + 98, topPos + 36, leftPos + 98 + barWidth, topPos + 44, 0xFF35B85A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int energy = menu.getDataValue(0) | menu.getDataValue(1) << 16;
        int capacity = menu.getDataValue(2) | menu.getDataValue(3) << 16;
        int charge = menu.getDataValue(4) | menu.getDataValue(5) << 16;
        String status = switch (menu.getDataValue(6)) {
            case 1 -> "Charging";
            case 2 -> "No FE";
            default -> "Idle";
        };
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, "Energy: " + energy + " / " + capacity + " FE", 98, 19, 0xFFFFFF, false);
        graphics.drawString(font, "Phone: " + (charge / 100) + "%", 98, 52, 0xFFFFFF, false);
        graphics.drawString(font, status, 98, 66, 0xFFFFFF, false);
        graphics.drawString(font, playerInventory.getDisplayName(), 8, 72, 0xFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MenuKeyHandler.isInventoryKey(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
