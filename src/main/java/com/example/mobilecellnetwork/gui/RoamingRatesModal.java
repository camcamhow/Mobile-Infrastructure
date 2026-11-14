package com.example.mobilecellnetwork.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RoamingRatesModal extends Screen {
    private final Screen parent;
    private final String networkName;
    private final boolean initialAllow;
    private final boolean initialAutoSend;
    private final long minuteRate;
    private final long textRate;
    private final long mbRate;
    private final Consumer<String> onSave;
    private Button allowButton;
    private Button autoSendButton;
    private EditBox minuteField;
    private EditBox textField;
    private EditBox mbField;
    private String error = "";

    public RoamingRatesModal(Screen parent, String networkName, boolean allowRoaming,
                             long minuteRate, long textRate, long mbRate,
                             boolean autoSend, Consumer<String> onSave) {
        super(Component.literal("Roaming Settings"));
        this.parent = parent;
        this.networkName = networkName;
        this.initialAllow = allowRoaming;
        this.initialAutoSend = autoSend;
        this.minuteRate = minuteRate;
        this.textRate = textRate;
        this.mbRate = mbRate;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int x = (width - 230) / 2;
        int y = (height - 184) / 2;
        allowButton = Button.builder(Component.literal("Roaming: " + (initialAllow ? "On" : "Off")),
                        button -> button.setMessage(Component.literal("Roaming: "
                                + (button.getMessage().getString().endsWith("Off") ? "On" : "Off"))))
                .bounds(x + 12, y + 38, 98, 18).build();
        autoSendButton = Button.builder(Component.literal("Auto requests: " + (initialAutoSend ? "On" : "Off")),
                        button -> button.setMessage(Component.literal("Auto requests: "
                                + (button.getMessage().getString().endsWith("Off") ? "On" : "Off"))))
                .bounds(x + 116, y + 38, 102, 18).build();
        addRenderableWidget(allowButton);
        addRenderableWidget(autoSendButton);
        minuteField = input(x, y + 72, "Per minute", minuteRate);
        textField = input(x, y + 96, "Per text", textRate);
        mbField = input(x, y + 120, "Per MB", mbRate);
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(x + 116, y + 156, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(x + 170, y + 156, 48, 18).build());
    }

    private EditBox input(int x, int y, String hint, long value) {
        EditBox field = new EditBox(font, x + 12, y, 206, 18, Component.literal(hint));
        field.setMaxLength(19);
        field.setValue(Long.toString(value));
        field.setHint(Component.literal(hint));
        addRenderableWidget(field);
        return field;
    }

    private void save() {
        try {
            long minute = Long.parseLong(minuteField.getValue());
            long text = Long.parseLong(textField.getValue());
            long mb = Long.parseLong(mbField.getValue());
            if (minute < 0L || text < 0L || mb < 0L) {
                error = "Rates must be non-negative.";
                return;
            }
            String value = String.join("|",
                    allowButton.getMessage().getString().endsWith("On") ? "allow" : "disallow",
                    Long.toString(minute), Long.toString(text), Long.toString(mb),
                    autoSendButton.getMessage().getString().endsWith("On") ? "auto" : "manual");
            onSave.accept(value);
            returnToParent();
        } catch (NumberFormatException exception) {
            error = "Enter valid whole-number rates.";
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = (width - 230) / 2;
        int y = (height - 184) / 2;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(x, y, x + 230, y + 184, 0xFF1E2A38);
        graphics.fill(x + 2, y + 2, x + 228, y + 182, 0xFF0F1620);
        graphics.renderOutline(x, y, 230, 184, 0xFF3A4A5A);
        graphics.drawString(font, "Roaming: " + font.plainSubstrByWidth(networkName, 170),
                x + 12, y + 12, PhoneColors.PRIMARY_TEXT, true);
        if (!error.isEmpty()) {
            graphics.drawString(font, error, x + 12, y + 143, PhoneColors.ERROR_TEXT, true);
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
