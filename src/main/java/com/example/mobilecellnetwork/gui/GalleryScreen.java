package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSocialImageCache;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GalleryScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private java.util.List<PhonePhotoLibrary.Photo> photos = java.util.List.of();
    private int scroll;
    private String error = "";
    private String selectedPhoto = "";
    private boolean libraryView;
    private final java.util.function.Consumer<PhonePhotoLibrary.Photo> selection;
    private final Screen returnScreen;

    public GalleryScreen(PhoneMenu menu) {
        this(menu, null, null);
    }

    public GalleryScreen(PhoneMenu menu, Screen returnScreen,
                         java.util.function.Consumer<PhonePhotoLibrary.Photo> selection) {
        super(Component.literal("Gallery"));
        this.menu = menu;
        this.returnScreen = returnScreen;
        this.selection = selection;
        this.libraryView = selection != null;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        if (returnScreen != null) {
            minecraft.setScreen(returnScreen);
        } else {
            PhoneNavigation.openHome(this);
        }
    }

    @Override
    protected void init() {
        reloadPhotos();
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() != PhoneStyle.DEFAULT) {
            return;
        }
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        addRenderableWidget(new PhoneActionButton(left + 12, top + 280, 88, 22,
                "Import photo", button -> importPhoto()));
        addRenderableWidget(new PhoneActionButton(left + 108, top + 280, 88, 22,
                "Use selected", button -> useSelected()));
        addRenderableWidget(new PhoneActionButton(left + 12, top + 307, 184, 20,
                "Delete selected", button -> confirmDelete()));
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
            if (selection == null) {
                ClientSocialImageCache.setPendingImage(photo.bytes());
            }
            selectedPhoto = photo.name();
            reloadPhotos();
        }));
    }

    private void useSelected() {
        if (selection != null) {
            for (PhonePhotoLibrary.Photo photo : photos) {
                if (photo.name().equals(selectedPhoto)) {
                    selection.accept(photo);
                    minecraft.setScreen(returnScreen);
                    return;
                }
            }
            error = "Select or import a photo first.";
            return;
        }
        if (selectedPhoto.isEmpty() || ClientSocialImageCache.pendingImage().length == 0) {
            error = "Select or import a photo first.";
            return;
        }
        minecraft.setScreen(new SocialsScreen(menu));
    }

    private void openPhoto(PhonePhotoLibrary.Photo photo) {
        if (selection == null) {
            ClientSocialImageCache.setPendingImage(photo.bytes());
        }
        selectedPhoto = photo.name();
        error = "";
        minecraft.setScreen(new GalleryPhotoViewerScreen(menu, this, photo));
    }

    private void confirmDelete() {
        if (selectedPhoto.isEmpty()) {
            error = "Select a photo to delete.";
            return;
        }
        var phoneId = com.example.mobilecellnetwork.network.PhoneDeviceClientCache.phoneId();
        if (phoneId == null) {
            error = "No active phone.";
            return;
        }
        String name = selectedPhoto;
        minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed -> {
            minecraft.setScreen(this);
            if (confirmed) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.example.mobilecellnetwork.network.PhonePhotoDeletePacket(phoneId, name));
            }
        }, Component.literal("Delete this photo?"),
                Component.literal("This removes it from the phone and cannot be undone."),
                Component.literal("Delete"), Component.literal("Cancel")));
    }

    public void photoDeleted(String name) {
        if (name.equals(selectedPhoto)) {
            if (selection == null) {
                for (PhonePhotoLibrary.Photo photo : photos) {
                    if (photo.name().equals(name)
                            && java.util.Arrays.equals(photo.bytesView(), ClientSocialImageCache.pendingImage())) {
                        ClientSocialImageCache.clearPendingImage();
                        break;
                    }
                }
            }
            selectedPhoto = "";
        }
        reloadPhotos();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            renderSnappleGallery(graphics);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            renderAndroidGallery(graphics);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Gallery", true);
        PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
        int first = Math.min(scroll, Math.max(0, photos.size() - 4));
        if (photos.isEmpty()) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 68, 188, 62);
            graphics.drawCenteredString(font, "No photos yet",
                    left + 102, top + 86, PhoneColors.textSecondary(style));
            graphics.drawCenteredString(font, "Import one or open Camera",
                    left + 102, top + 102, PhoneColors.textSecondary(style));
        }
        for (int index = first; index < Math.min(photos.size(), first + 4); index++) {
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
                    left + 102, top + 262, PhoneColors.error(style));
        } else {
            graphics.drawCenteredString(font, selection != null
                            ? (selectedPhoto.isEmpty() ? "Select a profile photo" : "Photo selected")
                            : ClientSocialImageCache.hasPendingImage()
                                    ? "Photo ready to attach" : "Select a photo to use in Socials",
                    left + 102, top + 262, PhoneColors.textSecondary(style));
        }
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 12, top + 280, 88, 22,
                "Import photo", font);
        PhoneRenderer.drawPrimaryButton(graphics, style, left + 108, top + 280, 88, 22,
                "Use selected", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 12, top + 307, 184, 20,
                "Delete selected", font);
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderAndroidGallery(GuiGraphics graphics) {
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = PhoneStyle.SAMSUNG_ONE_UI;
        PhoneFrame.renderHeader(graphics, this, "Gallery", false);
        graphics.drawString(font, photos.size() + (photos.size() == 1 ? " photo" : " photos"),
                x + 12, y + 67, PhoneColors.textSecondary(style), false);
        int first = Math.clamp(scroll, 0, Math.max(0, ((photos.size() + 2) / 3 - 3) * 3));
        if (photos.isEmpty()) {
            graphics.drawCenteredString(font, "No photos yet", x + 102, y + 145,
                    PhoneColors.textPrimary(style));
            graphics.drawCenteredString(font, "Capture or import a photo", x + 102, y + 162,
                    PhoneColors.textSecondary(style));
        }
        for (int index = first; index < Math.min(photos.size(), first + 9); index++) {
            int position = index - first;
            int px = x + 9 + position % 3 * 63;
            int py = y + 86 + position / 3 * 64;
            PhonePhotoLibrary.Photo photo = photos.get(index);
            McnPrimitives.fillRoundedRect(graphics, px, py, 60, 60, 7, PhoneColors.bgTertiary(style));
            PhonePhotoLibrary.drawThumbnail(graphics, photo.name(), photo.bytesView(), px + 2, py + 2, 56, 56);
            if (photo.name().equals(selectedPhoto)) {
                McnPrimitives.fillRoundedRectOutline(graphics, px, py, 60, 60, 7, 2, PhoneColors.accent(style));
            }
        }
        String message = !error.isBlank() ? error : selectedPhoto.isEmpty()
                ? "Tap a photo to view" : "Photo selected";
        graphics.drawCenteredString(font, font.plainSubstrByWidth(message, 180), x + 102, y + 287,
                error.isBlank() ? PhoneColors.textSecondary(style) : PhoneColors.error(style));
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 9, y + 308, 58, 24, "Import", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 73, y + 308, 58, 24, "Use", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 137, y + 308, 58, 24, "Delete", font);
    }

    private void renderSnappleGallery(GuiGraphics graphics) {
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = PhoneStyle.IOS;
        SnappleAppLayout.screen(graphics, font, x, y, libraryView ? "Library" : "Collections");
        SnappleText.draw(graphics, font, libraryView ? "All photos" : "Recent photos",
                x + 12, y + 68, PhoneColors.textPrimary(style));
        if (photos.isEmpty()) {
            SnappleAppLayout.group(graphics, x + 10, y + 87, 184, 112);
            SnappleText.centered(graphics, font, "No photos yet", x + 102, y + 127,
                    PhoneColors.textPrimary(style));
            SnappleText.centered(graphics, font,
                    SnappleAppLayout.fit(font, "Import a photo or open Camera", 168), x + 102, y + 145,
                    PhoneColors.textSecondary(style));
        } else if (!libraryView) {
            SnappleAppLayout.group(graphics, x + 10, y + 87, 184, 134);
            PhonePhotoLibrary.Photo newest = photos.get(0);
            PhonePhotoLibrary.drawThumbnail(graphics, newest.name(), newest.bytesView(), x + 14, y + 91, 176, 97);
            SnappleText.draw(graphics, font, "Recent", x + 18, y + 196, PhoneColors.textPrimary(style));
            SnappleText.draw(graphics, font, photos.size() + (photos.size() == 1 ? " photo" : " photos"),
                    x + 109, y + 196, PhoneColors.textSecondary(style));
        } else {
            int first = Math.clamp(scroll, 0, Math.max(0, ((photos.size() + 2) / 3 - 2) * 3));
            for (int index = first; index < Math.min(photos.size(), first + 6); index++) {
                int position = index - first;
                int px = x + 10 + position % 3 * 62;
                int py = y + 86 + position / 3 * 68;
                PhonePhotoLibrary.Photo photo = photos.get(index);
                SnappleAppLayout.group(graphics, px, py, 58, 62);
                PhonePhotoLibrary.drawThumbnail(graphics, photo.name(), photo.bytesView(), px + 2, py + 2, 54, 58);
                if (photo.name().equals(selectedPhoto)) {
                    graphics.renderOutline(px, py, 58, 62, PhoneColors.accent(style));
                }
            }
        }
        String message = !error.isBlank() ? error : selectedPhoto.isEmpty()
                ? (libraryView ? "Select a photo" : "Open Recent to browse") : "Photo selected";
        SnappleText.centered(graphics, font, SnappleAppLayout.fit(font, message, 184), x + 102, y + 238,
                error.isBlank() ? PhoneColors.textSecondary(style) : PhoneColors.error(style));
        SnappleAppLayout.pill(graphics, font, x + 10, y + 259, 58, 25, "Import",
                PhoneColors.bgSecondary(style), PhoneColors.accent(style));
        SnappleAppLayout.pill(graphics, font, x + 73, y + 259, 58, 25, "Use",
                PhoneColors.bgSecondary(style), PhoneColors.accent(style));
        SnappleAppLayout.pill(graphics, font, x + 136, y + 259, 58, 25, "Delete",
                PhoneColors.bgSecondary(style), PhoneColors.error(style));
        com.example.mobilecellnetwork.gui.theme.McnPrimitives.fillRoundedRect(graphics, x + 19, y + 315,
                166, 30, 15, PhoneColors.bgSecondary(style));
        int activeX = libraryView ? x + 21 : x + 102;
        SnappleAppLayout.pill(graphics, font, activeX, y + 317, 81, 26,
                libraryView ? "Library" : "Collections", PhoneColors.bgTertiary(style), PhoneColors.accent(style));
        SnappleText.centered(graphics, font, libraryView ? "Collections" : "Library",
                libraryView ? x + 143 : x + 61, y + 326, PhoneColors.textSecondary(style));
    }

    private boolean clickSnappleGallery(double x, double y, int left, int top) {
        if (y >= top + 315 && y < top + 345 && x >= left + 19 && x < left + 185) {
            libraryView = x < left + 102;
            scroll = 0;
            return true;
        }
        if (y >= top + 259 && y < top + 284) {
            if (x >= left + 10 && x < left + 68) importPhoto();
            else if (x >= left + 73 && x < left + 131) useSelected();
            else if (x >= left + 136 && x < left + 194) confirmDelete();
            else return false;
            return true;
        }
        if (!libraryView && !photos.isEmpty() && x >= left + 10 && x < left + 194
                && y >= top + 87 && y < top + 221) {
            libraryView = true;
            scroll = 0;
            return true;
        }
        if (libraryView) {
            int first = Math.clamp(scroll, 0, Math.max(0, ((photos.size() + 2) / 3 - 2) * 3));
            for (int index = first; index < Math.min(photos.size(), first + 6); index++) {
                int position = index - first;
                int px = left + 10 + position % 3 * 62;
                int py = top + 86 + position / 3 * 68;
                if (x >= px && x < px + 58 && y >= py && y < py + 62) {
                    openPhoto(photos.get(index));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            return button == 0 && clickSnappleGallery(x, y, left, PhoneFrame.getScreenY(this));
        }
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            if (button != 0) return false;
            int sy = PhoneFrame.getScreenY(this);
            if (y >= sy + 308 && y < sy + 332) {
                if (x >= left + 9 && x < left + 67) importPhoto();
                else if (x >= left + 73 && x < left + 131) useSelected();
                else if (x >= left + 137 && x < left + 195) confirmDelete();
                else return false;
                return true;
            }
            int first = Math.clamp(scroll, 0, Math.max(0, ((photos.size() + 2) / 3 - 3) * 3));
            for (int index = first; index < Math.min(photos.size(), first + 9); index++) {
                int position = index - first;
                int px = left + 9 + position % 3 * 63;
                int py = sy + 86 + position / 3 * 64;
                if (x >= px && x < px + 60 && y >= py && y < py + 60) {
                    openPhoto(photos.get(index));
                    return true;
                }
            }
            return false;
        }
        if (button == 0) {
            int first = Math.min(scroll, Math.max(0, photos.size() - 4));
            for (int index = first; index < Math.min(photos.size(), first + 4); index++) {
                int rowY = top + 66 + (index - first) * 44;
                if (x >= left + 8 && x < left + 196 && y >= rowY && y < rowY + 40) {
                    openPhoto(photos.get(index));
                    return true;
                }
            }
            if (x >= left + 12 && x < left + 100 && y >= top + 280 && y < top + 302) {
                importPhoto();
                return true;
            }
            if (x >= left + 108 && x < left + 196 && y >= top + 280 && y < top + 302) {
                useSelected();
                return true;
            }
            if (x >= left + 12 && x < left + 196 && y >= top + 307 && y < top + 327) {
                confirmDelete();
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            if (scrollY == 0) return false;
            scroll = Math.clamp(scroll + (scrollY < 0 ? 3 : -3),
                    0, Math.max(0, ((photos.size() + 2) / 3 - 3) * 3));
            return true;
        }
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            if (libraryView && scrollY != 0) {
                scroll = Math.clamp(scroll + (scrollY < 0 ? 3 : -3),
                        0, Math.max(0, ((photos.size() + 2) / 3 - 2) * 3));
                return true;
            }
            return false;
        }
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, photos.size() - 4));
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
