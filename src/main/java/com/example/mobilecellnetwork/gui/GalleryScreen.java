package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSocialImageCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GalleryScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private java.util.List<PhonePhotoLibrary.Photo> photos = java.util.List.of();
    private int scroll;
    private String error = "";
    private String selectedPhoto = "";

    public GalleryScreen(PhoneMenu menu) {
        super(Component.literal("Gallery"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(new SocialsScreen(menu));
    }

    @Override
    protected void init() {
        reloadPhotos();
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        addRenderableWidget(new PhoneActionButton(left + 12, top + 293, 88, 22,
                "Import photo", button -> importPhoto()));
        addRenderableWidget(new PhoneActionButton(left + 108, top + 293, 88, 22,
                "Use selected", button -> useSelected()));
    }

    private void reloadPhotos() {
        try {
            photos = PhonePhotoLibrary.photos();
            error = "";
        } catch (java.io.IOException exception) {
            photos = java.util.List.of();
            error = "Gallery unavailable: " + exception.getMessage();
        }
    }

    private void importPhoto() {
        minecraft.setScreen(new PhotoImportScreen(this, menu, photo -> {
            ClientSocialImageCache.setPendingImage(photo.bytes());
            selectedPhoto = photo.name();
            reloadPhotos();
        }));
    }

    private void useSelected() {
        if (ClientSocialImageCache.pendingImage().length == 0) {
            error = "Select or import a photo first.";
            return;
        }
        minecraft.setScreen(new SocialsScreen(menu));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Gallery", true);
        PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
        int first = Math.min(scroll, Math.max(0, photos.size() - 5));
        if (photos.isEmpty()) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 68, 188, 62);
            graphics.drawCenteredString(font, "No photos yet",
                    left + 102, top + 86, PhoneColors.textSecondary(style));
            graphics.drawCenteredString(font, "Import one or open Camera",
                    left + 102, top + 102, PhoneColors.textSecondary(style));
        }
        for (int index = first; index < Math.min(photos.size(), first + 5); index++) {
            PhonePhotoLibrary.Photo photo = photos.get(index);
            int y = top + 66 + (index - first) * 44;
            PhoneRenderer.drawCard(graphics, style, left + 8, y, 188, 40);
            PhonePhotoLibrary.drawThumbnail(graphics, photo.name(), photo.bytesView(),
                    left + 12, y + 4, 42, 32);
                        graphics.drawString(font, font.plainSubstrByWidth(photo.name(), 120),
                    left + 60, y + 12, PhoneColors.textPrimary(style), true);
            if (photo.name().equals(selectedPhoto)) {
                graphics.renderOutline(left + 8, y, 188, 40, PhoneColors.accent(style));
            }
        }
        if (!error.isBlank()) {
            graphics.drawCenteredString(font, font.plainSubstrByWidth(error, 180),
                    left + 102, top + 276, PhoneColors.error(style));
        } else {
            graphics.drawCenteredString(font, ClientSocialImageCache.hasPendingImage()
                            ? "Photo ready to attach" : "Select a photo to use in Socials",
                    left + 102, top + 276, PhoneColors.textSecondary(style));
        }
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 12, top + 293, 88, 22,
                "Import photo", font);
        PhoneRenderer.drawPrimaryButton(graphics, style, left + 108, top + 293, 88, 22,
                "Use selected", font);
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        if (button == 0) {
            int first = Math.min(scroll, Math.max(0, photos.size() - 5));
            for (int index = first; index < Math.min(photos.size(), first + 5); index++) {
                int rowY = top + 66 + (index - first) * 44;
                if (x >= left + 8 && x < left + 196 && y >= rowY && y < rowY + 40) {
                    var photo = photos.get(index);
                    ClientSocialImageCache.setPendingImage(photo.bytes());
                    selectedPhoto = photo.name();
                    error = "";
                    return true;
                }
            }
            if (x >= left + 12 && x < left + 100 && y >= top + 293 && y < top + 315) {
                importPhoto();
                return true;
            }
            if (x >= left + 108 && x < left + 196 && y >= top + 293 && y < top + 315) {
                useSelected();
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, photos.size() - 5));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
