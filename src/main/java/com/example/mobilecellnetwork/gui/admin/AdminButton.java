package com.example.mobilecellnetwork.gui.admin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class AdminButton extends Button {
    public AdminButton(int x, int y, int width, int height, String label, OnPress action) {
        super(x, y, width, height, Component.literal(label), action, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        AdminPrimitives.drawButton(graphics, Minecraft.getInstance().font, getX(), getY(), getWidth(), getHeight(),
                getMessage().getString(), AdminPrimitives.ButtonVariant.SECONDARY, isHovered(), !active);
    }
}
