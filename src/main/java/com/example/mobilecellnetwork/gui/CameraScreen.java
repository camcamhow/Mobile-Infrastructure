package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSocialImageCache;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import java.io.IOException;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CameraScreen extends Screen implements PhoneNavigableScreen {
    private static final String[] ZOOM_LABELS = {"0.5x", "1x", "2x", "5x", "10x"};
    private static final double[] ZOOM_VALUES = {0.5D, 1.0D, 2.0D, 5.0D, 10.0D};
    private static final int ZOOM_BUTTON_WIDTH = 30;
    private static final int ZOOM_BUTTON_STEP = 32;
    private static final int ZOOM_CONTROLS_WIDTH =
            (ZOOM_LABELS.length - 1) * ZOOM_BUTTON_STEP + ZOOM_BUTTON_WIDTH;
    private final PhoneMenu menu;
    private String status = "";
    private String error = "";
    private String previewError = "";
    private int previewGeneration;
    private boolean draggingViewfinder;
    private boolean captureRequested;
    private boolean capturePending;
    private boolean previewReady;
    private boolean selfie;
    private CameraType originalCamera;
    private CameraType rearCamera;
    private CameraType managedCamera;
    private double rearZoom = 1.0D;
    private double selfieZoom = 2.0D;

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
        if (minecraft != null && minecraft.player != null) {
            if (originalCamera == null) {
                originalCamera = minecraft.options.getCameraType();
                selfie = originalCamera == CameraType.THIRD_PERSON_FRONT;
                rearCamera = selfie ? CameraType.FIRST_PERSON : originalCamera;
            }
            applyCameraMode();
        }
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            return;
        }
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
        if (!capturePending) {
            capturePending = true;
            captureRequested = true;
            status = "Capturing...";
            error = "";
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
            if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
                renderSnappleCamera(graphics);
                PhoneFrame.renderStatusBar(graphics, this);
                return;
            }
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
            McnPrimitives.fillRoundedRect(graphics, previewX, previewY, previewWidth, previewHeight,
                    McnTheme.RADIUS_L, 0xFF16191F);
            if (!CameraPreviewCapture.draw(this, graphics, previewX, previewY, previewWidth, previewHeight)) {
                graphics.drawCenteredString(font, previewError.isBlank() ? "Starting live view..." : "Preview unavailable",
                        centerX,
                        previewY + (previewHeight - font.lineHeight) / McnTheme.SPACE_XXS,
                        PhoneColors.textSecondary(style));
            }
            renderCameraSwitch(graphics, previewX, previewY, previewWidth);
            renderZoomControls(graphics, previewX, previewY + previewHeight - 22, previewWidth);

            graphics.drawCenteredString(font, font.plainSubstrByWidth(
                            "Move: WASD  |  Look: drag preview or arrows", layout.width()),
                    centerX, layout.infoY(), PhoneColors.textSecondary(style));
            String message = !error.isBlank() ? error : !previewError.isBlank() ? previewError : status;
            if (!message.isBlank()) {
                graphics.drawCenteredString(font, font.plainSubstrByWidth(message, layout.width()),
                        centerX, layout.infoY() + font.lineHeight + McnTheme.SPACE_XS,
                        error.isBlank() && previewError.isBlank()
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

    private void renderSnappleCamera(GuiGraphics graphics) {
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = PhoneStyle.IOS;
        McnPrimitives.fillRoundedRect(graphics, x, y, PhoneFrame.getScreenW(), PhoneFrame.getScreenH(),
                14, PhoneColors.bgPrimary(style));
        McnPrimitives.fillRoundedRect(graphics, x + 7, y + 61, 190, 185, 10, 0xFF101216);
        if (!CameraPreviewCapture.draw(this, graphics, x + 7, y + 61, 190, 185)) {
            SnappleText.centered(graphics, font,
                    previewError.isBlank() ? "Starting live view..." : "Preview unavailable",
                    x + 102, y + 148, 0xFFFFFFFF);
        }
        renderCameraSwitch(graphics, x + 7, y + 61, 190);
        renderZoomControls(graphics, x + 7, y + 224, 190);
        SnappleAppLayout.pill(graphics, font, x + 10, y + 31, 55, 22, "Import",
                PhoneColors.bgSecondary(style), PhoneColors.textPrimary(style));
        SnappleAppLayout.pill(graphics, font, x + 132, y + 31, 62, 22, "Socials",
                PhoneColors.bgSecondary(style), PhoneColors.accent(style));
        String message = !error.isBlank() ? error : !previewError.isBlank() ? previewError : status;
        if (!message.isBlank()) {
            SnappleText.centered(graphics, font, SnappleAppLayout.fit(font, message, 184), x + 102, y + 251,
                    error.isBlank() && previewError.isBlank()
                            ? PhoneColors.textSecondary(style) : PhoneColors.error(style));
        } else {
            SnappleText.centered(graphics, font, "Drag preview to look", x + 102, y + 251,
                    PhoneColors.textSecondary(style));
        }
        int shutterWidth = PhoneFrame.circularElementWidth(42);
        McnPrimitives.fillRoundedRect(graphics, x + 102 - shutterWidth / 2, y + 268,
                shutterWidth, 42, 21, 0xFFAAAAAA);
        int innerWidth = PhoneFrame.circularElementWidth(34);
        McnPrimitives.fillRoundedRect(graphics, x + 102 - innerWidth / 2, y + 272,
                innerWidth, 34, 17, 0xFFFFFFFF);
        SnappleAppLayout.pill(graphics, font, x + 12, y + 317, 70, 27, "Gallery",
                PhoneColors.bgSecondary(style), PhoneColors.textPrimary(style));
        SnappleText.centered(graphics, font, "PHOTO", x + 141, y + 326, PhoneColors.accent(style));
    }

    private void renderZoomControls(GuiGraphics graphics, int x, int y, int width) {
        int start = x + (width - ZOOM_CONTROLS_WIDTH) / 2;
        for (int i = 0; i < ZOOM_LABELS.length; i++) {
            int color = zoom() == ZOOM_VALUES[i] ? 0xFFFFD75E : 0xFFFFFFFF;
            McnPrimitives.fillRoundedRect(graphics, start + i * ZOOM_BUTTON_STEP, y,
                    ZOOM_BUTTON_WIDTH, 18, 8, 0xC0202024);
            graphics.drawCenteredString(font, ZOOM_LABELS[i],
                    start + i * ZOOM_BUTTON_STEP + ZOOM_BUTTON_WIDTH / 2, y + 5, color);
        }
    }

    private void renderCameraSwitch(GuiGraphics graphics, int x, int y, int width) {
        int buttonX = x + width - 63;
        McnPrimitives.fillRoundedRect(graphics, buttonX, y + 4, 59, 18, 8, 0xD0202024);
        graphics.drawCenteredString(font, selfie ? "Front" : "Rear", buttonX + 29, y + 9,
                capturePending ? 0xFF929292 : 0xFFFFFFFF);
    }

    private void toggleCamera() {
        if (capturePending || minecraft == null || minecraft.player == null) {
            return;
        }
        selfie = !selfie;
        applyCameraMode();
        previewReady = false;
        previewGeneration++;
        draggingViewfinder = false;
        status = selfie ? "Front camera" : "Rear camera";
        error = "";
    }

    private void applyCameraMode() {
        CameraType cameraType = selfie ? CameraType.THIRD_PERSON_FRONT : rearCamera;
        minecraft.options.setCameraType(cameraType);
        managedCamera = cameraType;
    }

    private void setZoom(double value) {
        if (selfie) {
            selfieZoom = value;
        } else {
            rearZoom = value;
        }
    }

    private void restoreCamera() {
        if (minecraft != null && originalCamera != null && managedCamera != null
                && minecraft.options.getCameraType() == managedCamera) {
            minecraft.options.setCameraType(originalCamera);
        }
        managedCamera = null;
    }

    double zoom() {
        return selfie ? selfieZoom : rearZoom;
    }

    int previewGeneration() {
        return previewGeneration;
    }

    boolean takeCaptureRequest() {
        boolean requested = captureRequested;
        captureRequested = false;
        return requested;
    }

    void cameraPreviewReady() {
        previewReady = true;
        previewError = "";
    }

    boolean previewReady() {
        return previewReady;
    }

    void cameraPreviewFailed(int generation, String message) {
        if (generation == previewGeneration) {
            capturePending = false;
            captureRequested = false;
            previewReady = false;
            status = "";
            previewError = message;
        }
    }

    void captureFailed(int generation) {
        if (generation == previewGeneration) {
            capturePending = false;
            status = "";
            error = "Could not capture photo; try again.";
        }
    }

    void finishCapture(int generation, byte[] bytes) {
        if (generation != previewGeneration || minecraft.screen != this) {
            return;
        }
        capturePending = false;
        try {
            PhonePhotoLibrary.Photo photo = PhonePhotoLibrary.saveCapturedImage(bytes);
            ClientSocialImageCache.setPendingImage(photo.bytes());
            status = "Captured " + photo.name();
            error = "";
        } catch (IOException | RuntimeException exception) {
            status = "";
            error = "Capture failed: " + exception.getMessage();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        CameraType currentCamera = minecraft.options.getCameraType();
        if (managedCamera != null && currentCamera != managedCamera) {
            originalCamera = currentCamera;
            selfie = currentCamera == CameraType.THIRD_PERSON_FRONT;
            rearCamera = selfie ? CameraType.FIRST_PERSON : currentCamera;
            managedCamera = currentCamera;
            previewReady = false;
            previewGeneration++;
            capturePending = false;
            captureRequested = false;
            status = "Camera perspective changed.";
        }
        long window = minecraft.getWindow().getWindow();
        if (!minecraft.isWindowActive()) {
            return;
        }
        double turnX = 0.0D;
        double turnY = 0.0D;
        if (lookKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT)) {
            turnX -= 30.0D;
        }
        if (lookKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT)) {
            turnX += 30.0D;
        }
        if (lookKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_UP)) {
            turnY -= 20.0D;
        }
        if (lookKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN)) {
            turnY += 20.0D;
        }
        if (turnX != 0.0D || turnY != 0.0D) {
            minecraft.player.turn(turnX, turnY);
        }
    }

    private boolean lookKeyDown(long window, int key) {
        var options = minecraft.options;
        for (var binding : new net.minecraft.client.KeyMapping[] {options.keyUp, options.keyDown,
                options.keyLeft, options.keyRight, options.keyJump, options.keyShift, options.keySprint}) {
            if (binding.matches(key, -1)) {
                return false;
            }
        }
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, key);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingViewfinder && minecraft != null && minecraft.player != null) {
            double sensitivity = Math.pow(minecraft.options.sensitivity().get() * 0.6D + 0.2D, 3.0D) * 8.0D;
            double dx = dragX * minecraft.getWindow().getScreenWidth() / width;
            double dy = dragY * minecraft.getWindow().getScreenHeight() / height;
            minecraft.player.turn(dx * sensitivity,
                    dy * sensitivity * (minecraft.options.invertYMouse().get() ? -1 : 1));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void removed() {
        draggingViewfinder = false;
        restoreCamera();
        previewGeneration++;
        captureRequested = false;
        capturePending = false;
        previewReady = false;
        previewError = "";
        CameraPreviewCapture.close(this);
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        Layout layout = layout();
        boolean snapple = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.IOS;
        int previewX = snapple ? left + 7 : layout.left() + McnTheme.SPACE_S;
        int previewY = snapple ? PhoneFrame.getScreenY(this) + 61 : layout.top() + McnTheme.SPACE_S;
        int previewWidth = snapple ? 190 : layout.width() - McnTheme.SPACE_S * McnTheme.SPACE_XXS;
        int previewHeight = snapple ? 185 : layout.viewfinderHeight() - McnTheme.SPACE_S * McnTheme.SPACE_XXS;
        int switchX = previewX + previewWidth - 63;
        if (button == 0 && x >= switchX && x < switchX + 59
                && y >= previewY + 4 && y < previewY + 22) {
            toggleCamera();
            return true;
        }
        int zoomStart = previewX + (previewWidth - ZOOM_CONTROLS_WIDTH) / 2;
        int zoomY = previewY + previewHeight - 22;
        if (button == 0 && y >= zoomY && y < zoomY + 18) {
            for (int i = 0; i < ZOOM_VALUES.length; i++) {
                if (x >= zoomStart + i * ZOOM_BUTTON_STEP
                        && x < zoomStart + i * ZOOM_BUTTON_STEP + ZOOM_BUTTON_WIDTH) {
                    setZoom(ZOOM_VALUES[i]);
                    return true;
                }
            }
        }
        if (button == 0 && x >= previewX && x < previewX + previewWidth
                && y >= previewY && y < previewY + previewHeight) {
            draggingViewfinder = true;
            return true;
        }
        if (snapple) {
            int sy = PhoneFrame.getScreenY(this);
            if (button == 0) {
                if (x >= left + 10 && x < left + 65 && y >= sy + 31 && y < sy + 53) {
                    importPhoto();
                    return true;
                }
                if (x >= left + 132 && x < left + 194 && y >= sy + 31 && y < sy + 53) {
                    usePhoto();
                    return true;
                }
                int shutterWidth = PhoneFrame.circularElementWidth(42);
                if (x >= left + 102 - shutterWidth / 2 && x < left + 102 + shutterWidth / 2
                        && y >= sy + 268 && y < sy + 310) {
                    capture();
                    return true;
                }
                if (x >= left + 12 && x < left + 82 && y >= sy + 317 && y < sy + 344) {
                    minecraft.setScreen(new GalleryScreen(menu, this, photo -> {
                        ClientSocialImageCache.setPendingImage(photo.bytes());
                        status = "Selected " + photo.name();
                        error = "";
                    }));
                    return true;
                }
            }
            return false;
        }
        if (button == 0) {
            if (x >= layout.left() && x < layout.left() + layout.halfWidth()
                    && y >= layout.actionRowY() && y < layout.actionRowY() + McnTheme.BUTTON_HEIGHT_SM) {
                capture();
                return true;
            }
            if (x >= layout.right() - layout.halfWidth() && x < layout.right()
                    && y >= layout.actionRowY() && y < layout.actionRowY() + McnTheme.BUTTON_HEIGHT_SM) {
                importPhoto();
                return true;
            }
            if (x >= layout.left() && x < layout.right()
                    && y >= layout.useRowY() && y < layout.useRowY() + McnTheme.BUTTON_HEIGHT_SM) {
                usePhoto();
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingViewfinder) {
            draggingViewfinder = false;
            return true;
        }
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
