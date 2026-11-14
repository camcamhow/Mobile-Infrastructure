package com.example.mobilecellnetwork.gui;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PhotoImportScreen extends Screen implements PhoneNavigableScreen {
    private final Screen returnScreen;
    private final PhoneMenu menu;
    private final Consumer<PhonePhotoLibrary.Photo> onPhotoSelected;
    private EditBox pathField;
    private String error = "";

    public PhotoImportScreen(Screen returnScreen, PhoneMenu menu,
                             Consumer<PhonePhotoLibrary.Photo> onPhotoSelected) {
        super(Component.literal("Import photo"));
        this.returnScreen = returnScreen;
        this.menu = menu;
        this.onPhotoSelected = onPhotoSelected;
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
    }

    private void importPhoto() {
        String value = pathField.getValue().strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            PhonePhotoLibrary.Photo photo = PhonePhotoLibrary.importImage(Path.of(value));
            onPhotoSelected.accept(photo);
            minecraft.setScreen(returnScreen);
        } catch (InvalidPathException | IOException exception) {
            error = exception.getMessage() == null ? "Unable to import that image." : exception.getMessage();
        }
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
        graphics.drawCenteredString(font, "Paste the full path to a PNG/JPEG",
                left + 102, top + 84, PhoneColors.textPrimary(style));
        graphics.drawCenteredString(font, "Image limit: 1,000,000 bytes",
                left + 102, top + 103, PhoneColors.textSecondary(style));
        PhoneTextField.renderBackground(graphics, style, left + 12, top + 148, 180, 28);
        PhoneRenderer.drawPrimaryButton(graphics, style, left + 12, top + 192, 84, 22,
                "Import", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 108, top + 192, 84, 22,
                "Cancel", font);
        if (!error.isBlank()) {
            graphics.drawCenteredString(font, font.plainSubstrByWidth(error, 180),
                    left + 102, top + 230, PhoneColors.error(style));
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
