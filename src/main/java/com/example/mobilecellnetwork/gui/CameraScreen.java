package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSocialImageCache;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CameraScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private final Set<KeyMapping> heldMovementKeys = new LinkedHashSet<>();
    private String status = "";
    private String error = "";
    private byte[] livePreview = new byte[0];
    private long livePreviewNumber = -1;
    private double previousMouseX;
    private double previousMouseY;
    private boolean hasPreviousMousePosition;

    public CameraScreen(PhoneMenu menu) {
        super(Component.literal("Camera"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        PhoneNavigation.openHome(this);
    }

    @Override
    protected void init() {
        Layout layout = layout();
        addRenderableWidget(new PhoneActionButton(layout.left(), layout.actionRowY(), layout.halfWidth(),
                McnTheme.BUTTON_HEIGHT_SM, "Capture", button -> capture()));
        addRenderableWidget(new PhoneActionButton(layout.right() - layout.halfWidth(), layout.actionRowY(),
                layout.halfWidth(), McnTheme.BUTTON_HEIGHT_SM, "Import", button -> importPhoto()));
        addRenderableWidget(new PhoneActionButton(layout.left(), layout.useRowY(), layout.width(),
                McnTheme.BUTTON_HEIGHT_SM, "Use photo in Socials", button -> usePhoto()));
    }

    private Layout layout() {
        int screenX = McnPrimitives.getScreenX(width);
        int screenY = McnPrimitives.getScreenY(height);
        int left = McnPrimitives.getContentLeft(screenX);
        int right = McnPrimitives.getContentRight(screenX, McnPrimitives.getScreenWidth());
        int top = McnPrimitives.getContentTop(screenY);
        int bottom = McnPrimitives.getContentBottom(screenY, McnPrimitives.getScreenHeight());
        int viewfinderHeight = (bottom - top) / McnTheme.SPACE_XXS;
        int useRowY = bottom - McnTheme.BUTTON_HEIGHT_SM;
        int actionRowY = useRowY - McnTheme.SPACE_XS - McnTheme.BUTTON_HEIGHT_SM;
        return new Layout(left, right, top, viewfinderHeight,
                top + viewfinderHeight + McnTheme.SPACE_S, actionRowY, useRowY);
    }

    private record Layout(int left, int right, int top, int viewfinderHeight, int infoY,
                          int actionRowY, int useRowY) {
        int width() {
            return right - left;
        }

        int halfWidth() {
            return (width() - McnTheme.SPACE_S) / McnTheme.SPACE_XXS;
        }
    }

    private void capture() {
        try {
            PhonePhotoLibrary.Photo photo = PhonePhotoLibrary.captureScreenshot();
            ClientSocialImageCache.setPendingImage(photo.bytes());
            status = "Captured " + photo.name();
            error = "";
        } catch (IOException | RuntimeException exception) {
            error = "Capture failed: " + exception.getMessage();
        }
    }

    private void importPhoto() {
        Minecraft.getInstance().setScreen(new PhotoImportScreen(this, menu, photo -> {
            ClientSocialImageCache.setPendingImage(photo.bytes());
            status = "Selected " + photo.name();
            error = "";
        }));
    }

    private void usePhoto() {
        if (ClientSocialImageCache.pendingImage().length == 0) {
            error = "Capture or import a photo first.";
            return;
        }
        Minecraft.getInstance().setScreen(new SocialsScreen(menu));
    }

    @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            PhoneFrame.render(graphics, this);
            int left = PhoneFrame.getScreenX(this);
            int top = PhoneFrame.getContentTop(this);
            PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
            PhoneFrame.renderHeader(graphics, this, "Camera", true);
            PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
            Layout layout = layout();
            int previewX = layout.left() + McnTheme.SPACE_S;
            int previewY = layout.top() + McnTheme.SPACE_S;
            int previewWidth = layout.width() - McnTheme.SPACE_S * McnTheme.SPACE_XXS;
            int previewHeight = layout.viewfinderHeight() - McnTheme.SPACE_S * McnTheme.SPACE_XXS;
            int centerX = layout.left() + layout.width() / McnTheme.SPACE_XXS;
            updatePreview();
            if (livePreview.length > 0) {
                PhonePhotoLibrary.drawVersionedThumbnail(graphics, "camera-live-" + livePreviewNumber,
                        livePreview, previewX, previewY, previewWidth, previewHeight);
            } else {
                McnPrimitives.fillRoundedRect(graphics, previewX, previewY, previewWidth, previewHeight,
                        McnTheme.RADIUS_L, 0xFF16191F);
                graphics.drawCenteredString(font, "Starting live view...", centerX,
                        previewY + (previewHeight - font.lineHeight) / McnTheme.SPACE_XXS,
                        PhoneColors.textSecondary(style));
            }
            graphics.drawCenteredString(font, "WASD move  |  Arrows or drag to look",
                    centerX, layout.infoY(), PhoneColors.textSecondary(style));
            String message = error.isBlank() ? status : error;
            if (!message.isBlank()) {
                graphics.drawCenteredString(font, font.plainSubstrByWidth(message, layout.width()),
                        centerX, layout.infoY() + font.lineHeight + McnTheme.SPACE_XS, error.isBlank()
                                ? PhoneColors.textSecondary(style) : PhoneColors.error(style));
            }
            PhoneRenderer.drawPrimaryButton(graphics, style, layout.left(), layout.actionRowY(),
                    layout.halfWidth(), McnTheme.BUTTON_HEIGHT_SM, "Capture", font);
            PhoneRenderer.drawSecondaryButton(graphics, style, layout.right() - layout.halfWidth(),
                    layout.actionRowY(), layout.halfWidth(), McnTheme.BUTTON_HEIGHT_SM, "Import", font);
            PhoneRenderer.drawPrimaryButton(graphics, style, layout.left(), layout.useRowY(),
                    layout.width(), McnTheme.BUTTON_HEIGHT_SM, "Use photo in Socials", font);
            super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                    PhoneFrame.displayMouseY(this, mouseY), partialTick);
            PhoneFrame.renderStatusBar(graphics, this);
        }

    private void updatePreview() {
        long frameNumber = CameraPreviewCapture.latestFrameNumber();
        if (frameNumber != livePreviewNumber) {
            byte[] current = CameraPreviewCapture.latestFrame();
            if (current.length > 0) {
                livePreview = current;
                livePreviewNumber = frameNumber;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        long window = minecraft.getWindow().getWindow();
        for (KeyMapping binding : movementBindings()) {
            var key = binding.getKey();
            boolean down = key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                    && com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key.getValue());
            binding.setDown(down);
            if (down) {
                heldMovementKeys.add(binding);
            } else {
                heldMovementKeys.remove(binding);
            }
        }
        double turnX = 0.0D;
        double turnY = 0.0D;
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT)) {
            turnX -= 30.0D;
        }
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT)) {
            turnX += 30.0D;
        }
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_UP)) {
            turnY -= 20.0D;
        }
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN)) {
            turnY += 20.0D;
        }
        if (turnX != 0.0D || turnY != 0.0D) {
            minecraft.player.turn(turnX, turnY);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && minecraft != null && minecraft.player != null
                && !(getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
            double sensitivity = Math.pow(minecraft.options.sensitivity().get() * 0.6D + 0.2D, 3.0D) * 8.0D;
            minecraft.player.turn(dragX * sensitivity * 0.15D, dragY * sensitivity * 0.15D);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        for (KeyMapping binding : movementBindings()) {
            if (binding.matches(key, scanCode)) {
                binding.setDown(true);
                heldMovementKeys.add(binding);
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        for (KeyMapping binding : movementBindings()) {
            if (binding.matches(key, scanCode) && heldMovementKeys.remove(binding)) {
                binding.setDown(false);
                return true;
            }
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    private Set<KeyMapping> movementBindings() {
        if (minecraft == null) {
            return Set.of();
        }
        return Set.of(minecraft.options.keyUp, minecraft.options.keyDown, minecraft.options.keyLeft,
                minecraft.options.keyRight, minecraft.options.keyJump, minecraft.options.keyShift);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        double layoutX = PhoneFrame.layoutMouseX(this, mouseX);
        double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        boolean insideViewfinder = layoutY >= top && layoutY < top + 270;
        if (insideViewfinder && hasPreviousMousePosition && minecraft != null && minecraft.player != null) {
            double sensitivity = Math.pow(minecraft.options.sensitivity().get() * 0.6D + 0.2D, 3.0D) * 8.0D;
            double deltaX = (mouseX - previousMouseX) * sensitivity * 0.15D;
            double deltaY = (mouseY - previousMouseY) * sensitivity * 0.15D;
            minecraft.player.turn(deltaX, deltaY);
        }
        previousMouseX = mouseX;
        previousMouseY = mouseY;
        hasPreviousMousePosition = true;
        super.mouseMoved((int) layoutX, (int) layoutY);
    }

    @Override
    public void removed() {
        heldMovementKeys.forEach(binding -> binding.setDown(false));
        heldMovementKeys.clear();
        hasPreviousMousePosition = false;
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        if (button == 0) {
            if (x >= left + 16 && x < left + 98 && y >= top + 276 && y < top + 298) {
                capture();
                return true;
            }
            if (x >= left + 106 && x < left + 188 && y >= top + 276 && y < top + 298) {
                importPhoto();
                return true;
            }
            if (x >= left + 16 && x < left + 188 && y >= top + 304 && y < top + 326) {
                usePhoto();
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
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
