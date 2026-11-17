package com.example.mobilecellnetwork.gui.admin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

public final class AdminToast {
    private final String message;
    private final int accentColor;
    private final long createdTimeMillis;
    private final long durationMillis;

    private static final List<AdminToast> TOASTS = new ArrayList<>();

    private AdminToast(String message, int accentColor, long durationMillis) {
        this.message = message;
        this.accentColor = accentColor;
        this.createdTimeMillis = System.currentTimeMillis();
        this.durationMillis = durationMillis;
    }

    public String getMessage() {
        return message;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdTimeMillis >= durationMillis;
    }

    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - createdTimeMillis;
        if (elapsed < 200) {
            return elapsed / 200f;
        }
        if (elapsed > durationMillis - 400) {
            return (durationMillis - elapsed) / 400f;
        }
        return 1.0f;
    }

    // Static helpers

    public static void show(String message, int accentColor) {
        TOASTS.add(new AdminToast(message, accentColor, AdminTheme.TOAST_DURATION_MS));
    }

    public static void success(String message) {
        show(message, AdminColors.SUCCESS);
    }

    public static void warn(String message) {
        show(message, AdminColors.WARN);
    }

    public static void error(String message) {
        show(message, AdminColors.ERROR);
    }

    public static void info(String message) {
        show(message, AdminColors.INFO);
    }

    public static void renderAll(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
            // Remove expired toasts
            TOASTS.removeIf(AdminToast::isExpired);

            int x = screenWidth - AdminTheme.TOAST_WIDTH - AdminTheme.SPACE_M;
            int y = screenHeight - AdminTheme.TOAST_HEIGHT - AdminTheme.SPACE_M;

            for (AdminToast toast : TOASTS) {
                float alpha = toast.getAlpha();
                if (alpha <= 0) continue;

                int accentColor = withAlpha(toast.getAccentColor(), (int) (0xFF * alpha));
                int bgColor = withAlpha(AdminColors.TOAST_BG, (int) (0xFF * alpha));
                int borderColor = withAlpha(AdminColors.BORDER, (int) (0xFF * alpha));

                int toastX = x;
                int toastY = y;
                int w = AdminTheme.TOAST_WIDTH;
                int h = AdminTheme.TOAST_HEIGHT;

                // Background
                AdminPrimitives.fillRoundedRect(graphics, toastX, toastY, w, h, AdminTheme.RADIUS_M, bgColor);
                AdminPrimitives.fillRoundedRectOutline(graphics, toastX, toastY, w, h, AdminTheme.RADIUS_M, 1, AdminColors.BORDER);

                // Accent bar
                graphics.fill(toastX, toastY, toastX + 4, toastY + AdminTheme.TOAST_HEIGHT, accentColor);

                // Message
                String message = toast.getMessage();
                int textColor = withAlpha(AdminColors.TEXT_PRIMARY, (int) (0xFF * alpha));
                graphics.drawString(font, message, toastX + AdminTheme.SPACE_M, toastY + (AdminTheme.TOAST_HEIGHT - font.lineHeight) / 2, textColor, false);

                y -= AdminTheme.TOAST_HEIGHT + AdminTheme.SPACE_XS;
            }
        }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}