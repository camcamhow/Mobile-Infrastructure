package com.example.mobilecellnetwork.gui;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class PhotoImportScreen extends Screen implements PhoneNavigableScreen {
    private final Screen returnScreen;
    private final PhoneMenu menu;
    private final Consumer<PhonePhotoLibrary.Photo> onPhotoSelected;
    private final boolean openPickerInitially;
    private EditBox pathField;
    private String error = "";
    private boolean pickerOpened;
    private boolean pickerPending;

    public PhotoImportScreen(Screen returnScreen, PhoneMenu menu,
                             Consumer<PhonePhotoLibrary.Photo> onPhotoSelected) {
        this(returnScreen, menu, onPhotoSelected, false);
    }

    public PhotoImportScreen(Screen returnScreen, PhoneMenu menu,
                             Consumer<PhonePhotoLibrary.Photo> onPhotoSelected,
                             boolean openPickerInitially) {
        super(Component.literal("Import photo"));
        this.returnScreen = returnScreen;
        this.menu = menu;
        this.onPhotoSelected = onPhotoSelected;
        this.openPickerInitially = openPickerInitially;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(returnScreen);
    }

    @Override
    protected void init() {
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        pathField = PhoneTextField.create(font, left + 12, top + 148, 180, 28,
                "Full image file path", 512, com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle());
        addRenderableWidget(pathField);
        addRenderableWidget(new PhoneActionButton(left + 12, top + 192, 84, 22,
                "Import", button -> importPhoto()));
        addRenderableWidget(new PhoneActionButton(left + 108, top + 192, 84, 22,
                "Cancel", button -> navigateBack()));
        addRenderableWidget(new PhoneActionButton(left + 12, top + 225, 180, 22,
                "Browse files", button -> openFilePicker()));
        if (openPickerInitially && !pickerOpened) {
            pickerOpened = true;
            openFilePicker();
        }
    }

    private void importPhoto() {
        String value = pathField.getValue().strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            importPhoto(Path.of(value));
        } catch (InvalidPathException exception) {
            error = exception.getMessage() == null ? "Unable to import that image." : exception.getMessage();
        }
    }

    private void importPhoto(Path path) {
        try {
            PhonePhotoLibrary.Photo photo = PhonePhotoLibrary.importImage(path);
            onPhotoSelected.accept(photo);
            minecraft.setScreen(returnScreen);
        } catch (IOException exception) {
            error = exception.getMessage() == null ? "Unable to import that image." : exception.getMessage();
        }
    }

    private void openFilePicker() {
        if (pickerPending) {
            return;
        }
        pickerPending = true;
        error = "";
        var client = minecraft;
        var phoneId = com.example.mobilecellnetwork.network.PhoneDeviceClientCache.phoneId();
        Thread picker = new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var patterns = stack.mallocPointer(3);
                patterns.put(stack.UTF8("*.png"));
                patterns.put(stack.UTF8("*.jpg"));
                patterns.put(stack.UTF8("*.jpeg"));
                patterns.flip();
                String filename = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select a profile or gallery photo", "", patterns, "PNG and JPEG images", false);
                Path selected = filename == null ? null : Path.of(filename);
                client.execute(() -> {
                    pickerPending = false;
                    if (client.screen != this
                            || !java.util.Objects.equals(phoneId,
                                    com.example.mobilecellnetwork.network.PhoneDeviceClientCache.phoneId())) {
                        return;
                    }
                    if (selected == null) {
                        if (openPickerInitially) {
                            navigateBack();
                        }
                    } else {
                        importPhoto(selected);
                    }
                });
            } catch (SecurityException | InvalidPathException | IllegalStateException | LinkageError exception) {
                com.mojang.logging.LogUtils.getLogger().warn("Unable to open the photo file picker", exception);
                client.execute(() -> {
                    pickerPending = false;
                    if (client.screen == this) {
                        error = "File picker unavailable. Enter a full image path.";
                    }
                });
            }
        }, "Phone photo file picker");
        picker.setDaemon(true);
        picker.start();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Import photo", true);
        PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, left + 8, top + 68, 188, 68);
        graphics.drawCenteredString(font, "Browse or paste a PNG/JPEG path",
                left + 102, top + 84, PhoneColors.textPrimary(style));
        graphics.drawCenteredString(font, "Image limit: 1,000,000 bytes",
                left + 102, top + 103, PhoneColors.textSecondary(style));
        PhoneTextField.renderBackground(graphics, style, left + 12, top + 148, 180, 28);
        PhoneRenderer.drawPrimaryButton(graphics, style, left + 12, top + 192, 84, 22,
                "Import", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 108, top + 192, 84, 22,
                "Cancel", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 12, top + 225, 180, 22,
                "Browse files", font);
        if (!error.isBlank()) {
            graphics.drawCenteredString(font, font.plainSubstrByWidth(error, 180),
                    left + 102, top + 260, PhoneColors.error(style));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257) {
            importPhoto();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
