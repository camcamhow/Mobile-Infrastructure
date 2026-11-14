package com.example.mobilecellnetwork.gui;

import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AlertComposeModal extends Screen {
    private final Screen parent;
    private final boolean hasRecipient;
    private final BiConsumer<String, String> onSend;
    private EditBox recipient;
    private EditBox message;
    private Button sendButton;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;

    public AlertComposeModal(Screen parent, String title, boolean hasRecipient,
                             BiConsumer<String, String> onSend) {
        super(Component.literal(title));
        this.parent = parent;
        this.hasRecipient = hasRecipient;
        this.onSend = onSend;
    }

    @Override
    protected void init() {
        modalW = Math.min(320, width - 20);
        modalH = Math.min(190, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        int inputWidth = modalW - 24;
        if (hasRecipient) {
            recipient = new EditBox(font, modalX + 12, modalY + 48, inputWidth, 18,
                    Component.literal("Recipient"));
            recipient.setMaxLength(64);
            recipient.setValue("ALL");
            recipient.setHint(Component.literal("ALL, player name, or phone number"));
            addRenderableWidget(recipient);
        }
        int messageY = modalY + (hasRecipient ? 86 : 62);
        message = new EditBox(font, modalX + 12, messageY, inputWidth, 18,
                Component.literal("Message"));
        message.setMaxLength(1000);
        message.setHint(Component.literal("Alert message"));
        addRenderableWidget(message);
        int buttonY = modalY + modalH - 26;
        sendButton = Button.builder(Component.literal("Send"), button -> send())
                .bounds(modalX + modalW - 116, buttonY, 48, 18).build();
        sendButton.active = false;
        addRenderableWidget(sendButton);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, buttonY, 54, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (sendButton != null && message != null) {
            sendButton.active = !message.getValue().isBlank()
                    && (!hasRecipient || recipient != null && !recipient.getValue().isBlank());
        }
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF1E2A38);
        graphics.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + modalH - 2, 0xFF0F1620);
        graphics.renderOutline(modalX, modalY, modalW, modalH, 0xFF3A4A5A);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + 2, PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, title, modalX + 12, modalY + 14, PhoneColors.PRIMARY_TEXT, true);
        if (hasRecipient) {
            graphics.drawString(font, "Recipient: ALL, player name, or phone number",
                    modalX + 12, modalY + 36, PhoneColors.SECONDARY_TEXT, true);
            graphics.drawString(font, "Message", modalX + 12, modalY + 74,
                    PhoneColors.SECONDARY_TEXT, true);
        } else {
            graphics.drawString(font, "Broadcasts to all registered phone users, online or offline.",
                    modalX + 12, modalY + 42, PhoneColors.SECONDARY_TEXT, true);
            graphics.drawString(font, "Message", modalX + 12, modalY + 52,
                    PhoneColors.SECONDARY_TEXT, true);
        }
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void send() {
        if (message == null || message.getValue().isBlank()
                || (hasRecipient && (recipient == null || recipient.getValue().isBlank()))) {
            return;
        }
        onSend.accept(hasRecipient ? recipient.getValue() : "", message.getValue());
        returnToParent();
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
