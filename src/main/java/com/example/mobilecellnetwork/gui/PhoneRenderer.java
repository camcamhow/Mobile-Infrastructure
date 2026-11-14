package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.SignalTier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class PhoneRenderer {
    private PhoneRenderer() {
    }

    public static void fillRoundedRect(GuiGraphics graphics, PhoneStyle style,
                                       int x, int y, int width, int height, int radius, int color) {
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, radius, color);
    }

    public static void drawCard(GuiGraphics graphics, PhoneStyle style, int x, int y, int width, int height) {
        McnPrimitives.drawCard(graphics, style, x, y, width, height);
    }

    public static void drawDivider(GuiGraphics graphics, PhoneStyle style, int x, int y, int width) {
        McnPrimitives.drawDivider(graphics, style, x, y, width);
    }

    public static void drawToggle(GuiGraphics graphics, PhoneStyle style, int x, int y, boolean on) {
        McnPrimitives.drawToggle(graphics, style, x, y, on, false, false);
    }

    public static void drawPrimaryButton(GuiGraphics graphics, PhoneStyle style,
                                         int x, int y, int width, int height, String label, Font font) {
        McnPrimitives.drawButton(graphics, style, font, x, y, width, height, label,
                McnPrimitives.ButtonVariant.PRIMARY, false, false);
    }

    public static void drawSecondaryButton(GuiGraphics graphics, PhoneStyle style,
                                           int x, int y, int width, int height, String label, Font font) {
        McnPrimitives.drawButton(graphics, style, font, x, y, width, height, label,
                McnPrimitives.ButtonVariant.SECONDARY, false, false);
    }

    public static void drawBubble(GuiGraphics graphics, PhoneStyle style, int x, int y, int width, int height,
                                  boolean sent, String text, Font font) {
        int background = sent ? PhoneColors.accent(style) : PhoneColors.bgTertiary(style);
        fillRoundedRect(graphics, style, x, y, width, height, 10, background);
        graphics.drawString(font, fit(font, text, width - 12), x + 6,
                y + (height - font.lineHeight) / 2, PhoneColors.textPrimary(style), true);
    }

    public static void drawBubble(GuiGraphics graphics, PhoneStyle style, int x, int y, int width, int height,
                                  boolean sent, java.util.List<net.minecraft.util.FormattedCharSequence> lines,
                                  Font font) {
        int background = sent ? PhoneColors.accent(style) : PhoneColors.bgTertiary(style);
        fillRoundedRect(graphics, style, x, y, width, height, 10, background);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x + 6, y + 4 + index * font.lineHeight,
                    PhoneColors.textPrimary(style), true);
        }
    }

    public static void drawIcon(GuiGraphics graphics, PhoneStyle style, int x, int y, int size,
                                int radius, int backgroundColor, char glyph, Font font) {
        fillRoundedRect(graphics, style, x, y, size, size, radius, backgroundColor);
        String text = Character.toString(glyph);
        graphics.pose().pushPose();
        graphics.pose().translate(x + (size - font.width(text) * 3) / 2.0D,
                y + (size - font.lineHeight * 3) / 2.0D, 0.0D);
        graphics.pose().scale(3.0F, 3.0F, 1.0F);
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    public static void drawStatusBar(GuiGraphics graphics, PhoneStyle style, int x, int y, int width,
                                     String carrier, String time, int signalBars, SignalTier tier,
                                     int batteryPercent, Font font) {
        boolean hasService = tier != null && tier != SignalTier.NO_SERVICE
                && (carrier == null || !"No Service".equalsIgnoreCase(carrier));
        McnPrimitives.drawStatusBar(graphics, style, font, x, y, width, carrier, time,
                signalBars, batteryPercent, ClientSignalCache.roaming(), hasService);
    }

    public static void drawHomeIndicator(GuiGraphics graphics, PhoneStyle style, int x, int y, int width) {
        McnPrimitives.drawHomeIndicator(graphics, style, x, y, width,
                McnTheme.HOME_INDICATOR_HEIGHT + McnTheme.SPACE_XS);
    }

    public static void drawHeader(GuiGraphics graphics, PhoneStyle style, int x, int y,
                                  String title, Font font, boolean showBackChevron) {
        McnPrimitives.drawHeader(graphics, style, font, x, y,
                McnTheme.PHONE_WIDTH - McnTheme.PHONE_BEZEL * McnTheme.SPACE_XXS,
                title, showBackChevron);
    }

    public static String formatPhoneNumber(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < digits.length(); index++) {
            if (index > 0 && index % 3 == 0) {
                builder.append('-');
            }
            builder.append(digits.charAt(index));
        }
        return builder.toString();
    }

    public static String formatRelativeTime(long timestampMillis) {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(0L, now - timestampMillis);
        long minutes = elapsed / 60_000L;
        if (minutes < 1) {
            return "now";
        }
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }
        if (hours < 48) {
            return "Yesterday";
        }
        return java.time.format.DateTimeFormatter.ofPattern("MM/dd")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(timestampMillis));
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }
}
