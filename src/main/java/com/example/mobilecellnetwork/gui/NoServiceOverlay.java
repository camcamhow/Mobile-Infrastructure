package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import com.example.mobilecellnetwork.gui.PhoneStyle;

public final class NoServiceOverlay {

    private NoServiceOverlay() {}

    /**
     * Renders the "No Service" overlay inside the phone content area.
     * Called when a network-dependent screen has no active plan.
     *
     * @param g          GuiGraphics for drawing
     * @param style      PhoneStyle for colors
     * @param screenX    Left coordinate of the content area
     * @param screenY    Top coordinate of the content area
     * @param screenW    Width of the content area
     * @param screenH    Height of the content area
     * @param appName    Name of the app to display at the top
     * @param font       Font for text rendering
     */
    public static void render(GuiGraphics g, PhoneStyle style, int screenX, int screenY,
                              int screenW, int screenH, String appName, Font font) {
        int centerX = screenX + screenW / 2;
        int centerY = screenY + screenH / 2;

        // App name at top (12pt textSecondary, centered)
        String appLabel = Component.literal(appName).getString();
        g.drawString(font, appLabel, centerX - font.width(appLabel) / 2, screenY + 12,
                PhoneColors.textSecondary(style), true);

        // "No Service" in the middle (16pt error color, centered)
        String noService = Component.literal("No Service").getString();
        g.drawString(font, noService, centerX - font.width(noService) / 2, centerY - 8,
                PhoneColors.error(style), true);

        // Instruction at bottom (11pt textSecondary, centered)
        String instruction = Component.literal("Open the Plan app to choose a plan").getString();
        g.drawString(font, instruction, centerX - font.width(instruction) / 2, centerY + 24,
                PhoneColors.textSecondary(style), true);
    }
}