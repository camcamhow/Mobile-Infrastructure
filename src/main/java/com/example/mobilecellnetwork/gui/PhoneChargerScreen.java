package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.admin.AdminColors;
import com.example.mobilecellnetwork.gui.admin.AdminButton;
import com.example.mobilecellnetwork.gui.admin.AdminPrimitives;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PhoneChargerScreen extends AbstractContainerScreen<PhoneChargerMenu> {
    public PhoneChargerScreen(PhoneChargerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new AdminButton(leftPos + 232, topPos + 7, 56, 18, "Close", button -> onClose()));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        AdminPrimitives.drawPanel(graphics, x, y, imageWidth, imageHeight);
        AdminPrimitives.drawHeader(graphics, x, y, imageWidth, 32);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 3, AdminColors.ACCENT);
        AdminPrimitives.drawContentArea(graphics, x + 1, y + 32, imageWidth - 2, 194);
        AdminPrimitives.drawCard(graphics, x + 12, y + 42, 276, 76);
        AdminPrimitives.drawCard(graphics, x + 12, y + 124, 276, 100);
        slotBackground(graphics, x + 25, y + 82);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotBackground(graphics, x + 69 + column * 18, y + 139 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotBackground(graphics, x + 69 + column * 18, y + 199);
        }
        int energy = menu.getDataValue(0) | menu.getDataValue(1) << 16;
        int capacity = menu.getDataValue(2) | menu.getDataValue(3) << 16;
        int fill = capacity <= 0 ? 0 : (int) Math.clamp((long) energy * 185 / capacity, 0L, 185L);
        AdminPrimitives.fillRoundedRect(graphics, x + 88, y + 83, 185, 7, 3, AdminColors.BG_INPUT);
        AdminPrimitives.fillRoundedRect(graphics, x + 88, y + 83, fill, 7, 3, AdminColors.ACCENT);
    }

    private static void slotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, AdminColors.BORDER_STRONG);
        graphics.fill(x, y, x + 16, y + 16, AdminColors.BG_INPUT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int energy = menu.getDataValue(0) | menu.getDataValue(1) << 16;
        int capacity = menu.getDataValue(2) | menu.getDataValue(3) << 16;
        int charge = menu.getDataValue(4) | menu.getDataValue(5) << 16;
        int maxCharge = com.example.mobilecellnetwork.config.MobileCellNetworkConfig.PHONE_MAX_CHARGE.get();
        int percent = (int) Math.clamp((long) charge * 100 / maxCharge, 0L, 100L);
        String status = switch (menu.getDataValue(6)) {
            case 1 -> "Charging";
            case 2 -> "No FE";
            default -> "Idle";
        };
        graphics.drawString(font, "PHONE CHARGER", 14, 12, AdminColors.TEXT_PRIMARY, false);
        graphics.drawString(font, "PHONE", 20, 53, AdminColors.TEXT_SECONDARY, false);
        graphics.drawString(font, "ENERGY", 88, 53, AdminColors.TEXT_SECONDARY, false);
        graphics.drawString(font, energy + " / " + capacity + " FE", 88, 68, AdminColors.TEXT_PRIMARY, false);
        String phoneStatus = menu.getSlot(0).hasItem() ? "Phone " + percent + "%  |  " + status : "No phone inserted";
        graphics.drawString(font, font.plainSubstrByWidth(phoneStatus, 185), 88, 99,
                AdminColors.TEXT_SECONDARY, false);
        graphics.drawString(font, "INVENTORY", 18, 128, AdminColors.TEXT_SECONDARY, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MenuKeyHandler.isInventoryKey(keyCode, scanCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
