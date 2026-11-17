package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GalleryPhotoViewerScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private final GalleryScreen gallery;
    private final PhonePhotoLibrary.Photo photo;

    public GalleryPhotoViewerScreen(PhoneMenu menu, GalleryScreen gallery, PhonePhotoLibrary.Photo photo) {
        super(Component.literal("Photo"));
        this.menu = menu;
        this.gallery = gallery;
        this.photo = photo;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(gallery);
    }

    @Override
    public void onClose() {
        navigateBack();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        if (style == PhoneStyle.SAMSUNG_ONE_UI) {
            int y = PhoneFrame.getScreenY(this);
            PhoneFrame.renderHeader(graphics, this, "Photo", true);
            graphics.fill(left + 8, y + 67, left + 196, y + 282, 0xFF101218);
            PhonePhotoLibrary.drawThumbnail(graphics, photo.name(), photo.bytesView(),
                    left + 10, y + 69, 184, 211);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(photo.name(), 180),
                    left + 102, y + 290, PhoneColors.textSecondary(style));
            PhoneRenderer.drawSecondaryButton(graphics, style, left + 37, y + 309, 130, 22,
                    "Back to gallery", font);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Photo", true);
        graphics.fill(left + 8, top + 55, left + 196, top + 276, 0xFF101014);
        PhonePhotoLibrary.drawThumbnail(graphics, photo.name(), photo.bytesView(),
                left + 10, top + 57, 184, 217);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(photo.name(), 180),
                left + 102, top + 285, PhoneColors.textSecondary(style));
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 37, top + 305, 130, 22,
                "Back to gallery", font);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            int yTop = PhoneFrame.getScreenY(this);
            if (button == 0 && x >= left + 37 && x < left + 167 && y >= yTop + 309 && y < yTop + 331) {
                navigateBack();
                return true;
            }
            return super.mouseClicked(x, y, button);
        }
        if (button == 0 && x >= left + 37 && x < left + 167 && y >= top + 305 && y < top + 327) {
            navigateBack();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
