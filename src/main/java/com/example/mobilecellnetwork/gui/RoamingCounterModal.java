package com.example.mobilecellnetwork.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RoamingCounterModal extends Screen {
    private final Screen parent;
    private final Consumer<long[]> onCounter;
    private EditBox minuteRate;
    private EditBox textRate;
    private EditBox mbRate;
    private String error = "";
    private int modalX;
    private int modalY;

    public RoamingCounterModal(Screen parent, Consumer<long[]> onCounter) {
        super(Component.literal("Counter roaming proposal"));
        this.parent = parent;
        this.onCounter = onCounter;
    }

    @Override
    protected void init() {
        modalX = (width - 240) / 2;
        modalY = (height - 190) / 2;
        minuteRate = input(modalY + 48, "Rate per minute");
        textRate = input(modalY + 72, "Rate per text");
        mbRate = input(modalY + 96, "Rate per MB");
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        Component.literal("Send counter"), button -> submit())
                .bounds(modalX + 100, modalY + 150, 72, 20).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + 178, modalY + 150, 50, 20).build());
    }

    private EditBox input(int y, String label) {
        EditBox field = new EditBox(font, modalX + 12, y, 216, 18, Component.literal(label));
        field.setMaxLength(19);
        field.setValue("0");
        field.setHint(Component.literal(label));
        addRenderableWidget(field);
        return field;
    }

    private void submit() {
        try {
            long minute = Long.parseLong(minuteRate.getValue());
            long text = Long.parseLong(textRate.getValue());
            long mb = Long.parseLong(mbRate.getValue());
            if (minute < 0L || text < 0L || mb < 0L) {
                error = "Rates must be non-negative.";
                return;
            }
            onCounter.accept(new long[]{minute, text, mb});
            returnToParent();
        } catch (NumberFormatException exception) {
            error = "Enter whole-number rates.";
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(modalX, modalY, modalX + 240, modalY + 190, 0xFF1E2A38);
        graphics.fill(modalX + 2, modalY + 2, modalX + 238, modalY + 188, 0xFF0F1620);
        graphics.renderOutline(modalX, modalY, 240, 190, 0xFF3A4A5A);
        graphics.fill(modalX, modalY, modalX + 240, modalY + 2, PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, "Counter roaming proposal", modalX + 12, modalY + 12,
                PhoneColors.PRIMARY_TEXT, true);
        if (!error.isEmpty()) {
            graphics.drawString(font, error, modalX + 12, modalY + 126, PhoneColors.ERROR_TEXT, true);
        }
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
