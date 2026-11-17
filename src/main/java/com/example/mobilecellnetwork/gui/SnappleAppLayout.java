package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class SnappleAppLayout {
    private SnappleAppLayout() {
    }

    static void screen(GuiGraphics graphics, Font font, int x, int y, String title) {
        int width = PhoneFrame.getScreenW();
        McnPrimitives.fillRoundedRect(graphics, x, y, width, PhoneFrame.getScreenH(), 14,
                PhoneColors.bgPrimary(PhoneStyle.IOS));
        McnPrimitives.drawPhoneCamera(graphics, PhoneStyle.IOS, x, y, width);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 13, y + 34, 0);
        graphics.pose().scale(1.6F, 1.6F, 1);
        SnappleText.draw(graphics, font, title, 0, 0, PhoneColors.textPrimary(PhoneStyle.IOS));
        graphics.pose().popPose();
    }

    static void group(GuiGraphics graphics, int x, int y, int width, int height) {
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, 12,
                PhoneColors.bgSecondary(PhoneStyle.IOS));
    }

    static void separator(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, PhoneColors.divider(PhoneStyle.IOS));
    }

    static void pill(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                     String text, int background, int foreground) {
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, height / 2, background);
        String label = fit(font, text, width - 8);
        SnappleText.centered(graphics, font, label, x + width / 2,
                y + (height - font.lineHeight) / 2, foreground);
    }

    static void avatar(GuiGraphics graphics, Font font, int centerX, int y, int size, String name) {
        int width = PhoneFrame.circularElementWidth(size);
        McnPrimitives.fillRoundedRect(graphics, centerX - width / 2, y, width, size, size / 2, 0xFF554B73);
        SnappleText.centered(graphics, font, initials(name), centerX,
                y + (size - font.lineHeight) / 2, 0xFFFFFFFF);
    }

    static String initials(String value) {
        String text = value == null ? "" : value.strip();
        if (text.isEmpty() || !Character.isLetter(text.codePointAt(0))) {
            return "?";
        }
        String[] parts = text.split("\\s+");
        String first = parts[0].substring(0, parts[0].offsetByCodePoints(0, 1));
        String last = parts.length > 1 ? parts[parts.length - 1]
                .substring(0, parts[parts.length - 1].offsetByCodePoints(0, 1)) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    static void searchIcon(GuiGraphics graphics, int x, int y, int color) {
        McnPrimitives.fillRoundedRectOutline(graphics, x, y, 10, 10, 5, 1, color);
        graphics.fill(x + 8, y + 8, x + 10, y + 10, color);
        graphics.fill(x + 10, y + 10, x + 12, y + 12, color);
    }

    static String fit(Font font, String text, int width) {
        if (font.width(SnappleText.text(text)) <= width) {
            return text;
        }
        String suffix = "...";
        if (font.width(SnappleText.text(suffix)) > width) {
            return "";
        }
        int end = text.length();
        while (end > 0) {
            end = text.offsetByCodePoints(end, -1);
            String result = text.substring(0, end) + suffix;
            if (font.width(SnappleText.text(result)) <= width) {
                return result;
            }
        }
        return suffix;
    }
}
