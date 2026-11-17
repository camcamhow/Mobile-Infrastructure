package com.example.mobilecellnetwork.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfirmActionModal extends Screen {
    private final Screen parent;
    private final Consumer<List<String>> onConfirm;
    private EditBox confirmation;
    private Button okButton;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;

    public ConfirmActionModal(Screen parent, String title, String message, Consumer<List<String>> onConfirm) {
        super(Component.literal(title));
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    public boolean isPhoneModal() {
        return parent instanceof PhoneNavigableScreen;
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        confirmation = new EditBox(font, modalX + 12, modalY + 78, modalW - 24, 18,
                Component.literal("Confirmation"));
        confirmation.setMaxLength(16);
        confirmation.setHint(Component.literal("DELETE"));
        addRenderableWidget(confirmation);
        okButton = Button.builder(Component.literal("OK"), button -> confirm())
                .bounds(modalX + modalW - 116, modalY + modalH - 26, 48, 18).build();
        okButton.active = false;
        addRenderableWidget(okButton);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, modalY + modalH - 26, 54, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    private void confirm() {
        if (confirmation == null || !"DELETE".equals(confirmation.getValue())) {
            return;
        }
        onConfirm.accept(List.of("DELETE"));
        returnToParent();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (okButton != null && confirmation != null) {
            okButton.active = "DELETE".equals(confirmation.getValue());
        }
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF1E2A38);
        graphics.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + modalH - 2, 0xFF0F1620);
        graphics.renderOutline(modalX, modalY, modalW, modalH, 0xFF3A4A5A);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + 2, PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), modalW - 24),
                modalX + 12, modalY + 14, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "This cannot be undone.", modalX + 12, modalY + 42,
                PhoneColors.SECONDARY_TEXT, true);
        graphics.drawString(font, "Type DELETE to confirm.", modalX + 12, modalY + 55,
                PhoneColors.SECONDARY_TEXT, true);
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
