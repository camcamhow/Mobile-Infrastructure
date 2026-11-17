package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class PhoneActionButton extends Button {
    public PhoneActionButton(int x, int y, int width, int height, String label, OnPress onPress) {
        super(x, y, width, height, Component.literal(label), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
