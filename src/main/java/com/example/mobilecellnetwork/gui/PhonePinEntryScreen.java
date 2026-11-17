package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.RemovePhonePinPacket;
import com.example.mobilecellnetwork.network.SetPhonePinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhonePinEntryScreen extends Screen {
    private final Screen parent;
    private final boolean remove;
    private EditBox pinField;

    public PhonePinEntryScreen(Screen parent, boolean remove) {
        super(Component.literal(remove ? "Remove Phone PIN" : "Set Phone PIN"));
        this.parent = parent;
        this.remove = remove;
    }

    @Override
    protected void init() {
        int x = width / 2 - 90;
        int y = height / 2 - 15;
        pinField = new EditBox(font, x, y, 180, 20, Component.literal("PIN"));
        pinField.setMaxLength(12);
        pinField.setFilter(value -> value.matches("[0-9]*"));
        pinField.setHint(Component.literal(remove ? "Current 4-12 digit PIN" : "New 4-12 digit PIN"));
        addRenderableWidget(pinField);
        addRenderableWidget(Button.builder(Component.literal(remove ? "Remove PIN" : "Save PIN"),
                        button -> submit())
                .bounds(x + 4, y + 32, 82, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(x + 94, y + 32, 82, 20).build());
    }

    private void submit() {
        String pin = pinField.getValue();
        if (pin.length() < 4 || pin.length() > 12) {
            return;
        }
        if (remove) {
            PacketDistributor.sendToServer(new RemovePhonePinPacket(pin));
        } else {
            PacketDistributor.sendToServer(new SetPhonePinPacket(pin));
        }
        returnToParent();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        PhoneFrame.beginPhoneText();
        try {
            graphics.fill(width / 2 - 108, height / 2 - 70, width / 2 + 108, height / 2 + 75,
                    PhoneColors.bgPrimary(ClientPhoneCache.phoneStyle()));
            graphics.renderOutline(width / 2 - 108, height / 2 - 70, 216, 145,
                    PhoneColors.divider(ClientPhoneCache.phoneStyle()));
            graphics.drawCenteredString(font, title, width / 2, height / 2 - 52,
                    PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
            graphics.drawCenteredString(font, remove ? "Verify your current PIN to remove it."
                            : "Choose a 4-12 digit PIN for this phone.",
                    width / 2, height / 2 - 34, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
            super.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            PhoneFrame.endPhoneText();
        }
    }

    private void returnToParent() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() { returnToParent(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
