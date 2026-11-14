package com.example.mobilecellnetwork.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MapPointShareModal extends Screen {
    private final Screen parent;
    private final Consumer<String> onShare;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;
    private EditBox numberInput;

    public MapPointShareModal(Screen parent, Consumer<String> onShare) {
        super(Component.literal("Share Point"));
        this.parent = parent;
        this.onShare = onShare;
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        numberInput = new EditBox(font, modalX + 12, modalY + 58, modalW - 24, 20,
                Component.literal("Phone number"));
        numberInput.setMaxLength(9);
        numberInput.setHint(Component.literal("Recipient phone number"));
        numberInput.setFilter(value -> value.matches("[0-9]*"));
        addRenderableWidget(numberInput);
        addRenderableWidget(Button.builder(Component.literal("Send Text"), button -> {
            String number = numberInput.getValue();
            if (number.matches("[0-9]{1,9}")) {
                onShare.accept(number);
                returnToParent();
            }
        }).bounds(modalX + modalW - 132, modalY + modalH - 28, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, modalY + modalH - 28, 54, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF1E2A38);
        graphics.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + modalH - 2, 0xFF0F1620);
        graphics.renderOutline(modalX, modalY, modalW, modalH, 0xFF3A4A5A);
        graphics.drawString(font, "Share Private Point", modalX + 12, modalY + 14,
                PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "The map location will be sent by SMS.", modalX + 12, modalY + 38,
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
