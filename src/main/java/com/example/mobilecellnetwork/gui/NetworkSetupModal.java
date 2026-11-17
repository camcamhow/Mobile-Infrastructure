package com.example.mobilecellnetwork.gui;

import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class NetworkSetupModal extends Screen {
    private final Screen parent;
    private final String currentName;
    private final BiConsumer<String, String> onSave;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private EditBox nameField;
    private EditBox passwordField;
    private Button saveButton;

    public NetworkSetupModal(Screen parent, String currentName,
                             BiConsumer<String, String> onSave) {
        super(Component.literal("Set Up Network"));
        this.parent = parent;
        this.currentName = currentName;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(300, width - 20);
        panelHeight = 166;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        nameField = new EditBox(font, panelX + 16, panelY + 44, panelWidth - 32, 18,
                Component.literal("Network name"));
        nameField.setMaxLength(64);
        nameField.setValue(currentName);
        addRenderableWidget(nameField);

        passwordField = new EditBox(font, panelX + 16, panelY + 84, panelWidth - 32, 18,
                Component.literal("Password"));
        passwordField.setMaxLength(128);
        passwordField.setFormatter((value, cursorPosition) ->
                FormattedCharSequence.forward("*".repeat(value.length()), Style.EMPTY));
        addRenderableWidget(passwordField);

        saveButton = Button.builder(Component.literal("Save"), button -> save())
                .bounds(panelX + panelWidth - 124, panelY + 132, 52, 20).build();
        addRenderableWidget(saveButton);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(panelX + panelWidth - 66, panelY + 132, 52, 20).build());
    }

    private void save() {
        String name = nameField.getValue().trim();
        String password = passwordField.getValue();
        int minimum = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        if (name.isEmpty() || name.length() > 64 || password.length() < minimum || password.length() > 128) {
            return;
        }
        onSave.accept(name, password);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF0F1620);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, 0xFF3A4A5A);
        graphics.drawString(font, title, panelX + 16, panelY + 14, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "Network name", panelX + 16, panelY + 32,
                PhoneColors.SECONDARY_TEXT, true);
        int minimum = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        graphics.drawString(font, "Password (min " + minimum + " characters)",
                panelX + 16, panelY + 72, PhoneColors.SECONDARY_TEXT, true);
        saveButton.active = !nameField.getValue().trim().isEmpty()
                && passwordField.getValue().length() >= minimum;
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
