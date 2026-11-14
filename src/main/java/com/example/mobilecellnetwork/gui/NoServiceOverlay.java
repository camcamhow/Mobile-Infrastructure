package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;

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
        McnPrimitives.fillRoundedRect(g, screenX, screenY, screenW, screenH,
                12, PhoneColors.bgPrimary(style));
        centered(g, font, style, appName, centerX, screenY + 13, PhoneColors.textSecondary(style));
        McnPrimitives.fillRoundedRect(g, screenX + 10, screenY + 43, screenW - 20, 162,
                14, PhoneColors.bgSecondary(style));
        McnPrimitives.fillRoundedRect(g, centerX - 24, screenY + 56, 48, 48,
                24, PhoneColors.bgTertiary(style));
        int icon = PhoneColors.textSecondary(style);
        for (int i = 0; i < 4; i++) {
            int barHeight = 5 + i * 4;
            g.fill(centerX - 14 + i * 7, screenY + 92 - barHeight,
                    centerX - 10 + i * 7, screenY + 92, icon);
        }
        for (int i = 0; i < 26; i++) {
            g.fill(centerX - 13 + i, screenY + 68 + i,
                    centerX - 11 + i, screenY + 70 + i, PhoneColors.error(style));
        }
        centered(g, font, style, "No active plan", centerX, screenY + 118, PhoneColors.textPrimary(style));
        centered(g, font, style, "Mobile service is unavailable.", centerX, screenY + 141,
                PhoneColors.textSecondary(style));
        centered(g, font, style, "Open Plan to choose a plan.", centerX, screenY + 156,
                PhoneColors.textSecondary(style));
    }

    private static void centered(GuiGraphics graphics, Font font, PhoneStyle style, String text,
                                 int centerX, int y, int color) {
        if (style == PhoneStyle.IOS) {
            SnappleText.centered(graphics, font, text, centerX, y, color);
        } else {
            graphics.drawCenteredString(font, text, centerX, y, color);
        }
    }
}