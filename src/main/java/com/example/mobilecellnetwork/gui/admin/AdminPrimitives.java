package com.example.mobilecellnetwork.gui.admin;

import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class AdminPrimitives {
    private AdminPrimitives() {
    }

    // ===== SHAPES =====

    public static void fillRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, radius, color);
    }

    public static void fillRoundedRectOutline(GuiGraphics graphics, int x, int y, int width, int height, int radius, int thickness, int color) {
        McnPrimitives.fillRoundedRectOutline(graphics, x, y, width, height, radius, thickness, color);
    }

    // ===== PANELS =====

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_L, AdminColors.BG_PANEL);
        fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_L, 1, AdminColors.BORDER);
    }

    public static void drawSidebar(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_L, AdminColors.BG_SIDEBAR);
        graphics.fill(x + width - 1, y, x + width, y + height, AdminColors.SIDEBAR_DIVIDER);
    }

    public static void drawContentArea(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_L, AdminColors.BG_CONTENT);
    }

    public static void drawHeader(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_L, AdminColors.BG_HEADER);
        graphics.fill(x, y + height - 1, x + width, y + height, AdminColors.DIVIDER);
    }

    public static void drawFooter(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_L, AdminColors.BG_FOOTER);
        graphics.fill(x, y, x + width, y + 1, AdminColors.DIVIDER);
    }

    public static void drawSectionHeader(GuiGraphics graphics, Font font, int x, int y, int width, String label) {
        String upper = label.toUpperCase(java.util.Locale.ROOT);
        drawCenteredText(graphics, font, upper, x, y, width, AdminTheme.SECTION_HEADER_HEIGHT,
                AdminTheme.TEXT_CAPTION, AdminColors.TEXT_MUTED, false);
    }

    // ===== ROWS / CARDS =====

    public static void drawCard(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_M, AdminColors.BG_CARD);
        fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_M, 1, AdminColors.BORDER);
    }

    public static void drawCardHovered(GuiGraphics graphics, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_M, AdminColors.BG_CARD_HOVER);
        fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_M, 1, AdminColors.BORDER);
    }

    public static void drawListRow(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean selected) {
        int bgColor = hovered ? AdminColors.BG_CARD_HOVER : AdminColors.BG_CARD;
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S, bgColor);
        if (selected) {
            fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_S, 2, AdminColors.ACCENT);
        }
    }

    // ===== SIDEBAR ITEMS =====

    public static void drawSidebarItem(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                       String label, boolean active, boolean hovered, int badgeCount) {
        // Background
        if (active) {
            fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S, AdminColors.TAB_ACTIVE_BG);
            graphics.fill(x, y, x + 2, y + height, AdminColors.TAB_ACTIVE_BAR);
        } else if (hovered) {
            fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S, AdminColors.TAB_HOVER_BG);
        }

        // Label
        int labelX = x + AdminTheme.SPACE_M;
        int labelColor = active ? AdminColors.TEXT_PRIMARY : AdminColors.TEXT_SECONDARY;
        drawLeftAlignedText(graphics, font, label, labelX, y, AdminTheme.TEXT_BODY, labelColor, height);

        // Badge
        if (badgeCount > 0) {
            String badgeText = badgeCount > 99 ? "99+" : String.valueOf(badgeCount);
            int badgeW = font.width(badgeText) + 12;
            int badgeH = 16;
            int badgeX = x + width - AdminTheme.SPACE_M - badgeW;
            int badgeY = y + (height - badgeH) / 2;
            fillRoundedRect(graphics, badgeX, badgeY, badgeW, badgeH, AdminTheme.RADIUS_ROUND, AdminColors.BADGE_BG);
            drawCenteredText(graphics, font, badgeText, badgeX, badgeY, badgeW, badgeH,
                    AdminTheme.TEXT_CAPTION, AdminColors.BADGE_TEXT, true);
        }
    }

    // ===== BUTTONS =====

    public enum ButtonVariant {
        PRIMARY, SECONDARY, TERTIARY, DESTRUCTIVE, GHOST
    }

    public static void drawButton(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                  String label, ButtonVariant variant, boolean hovered, boolean disabled) {
        int fillColor;
        int textColor;
        int outlineColor = 0;

        switch (variant) {
            case PRIMARY -> {
                fillColor = AdminColors.ACCENT;
                textColor = AdminColors.TEXT_INVERSE;
            }
            case SECONDARY -> {
                fillColor = AdminColors.BG_CARD;
                textColor = AdminColors.TEXT_PRIMARY;
                outlineColor = AdminColors.BORDER;
            }
            case TERTIARY -> {
                fillColor = 0;
                textColor = AdminColors.ACCENT;
            }
            case DESTRUCTIVE -> {
                fillColor = AdminColors.ERROR;
                textColor = AdminColors.TEXT_INVERSE;
            }
            case GHOST -> {
                fillColor = 0;
                textColor = AdminColors.TEXT_SECONDARY;
            }
            default -> {
                fillColor = AdminColors.ACCENT;
                textColor = AdminColors.TEXT_INVERSE;
            }
        }

        // Background
        if (fillColor != 0) {
            fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S, fillColor);
        }
        if (outlineColor != 0) {
            fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_S, 1, outlineColor);
        }

        // Hover overlay
        if (hovered && !disabled) {
            fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S,
                    withAlpha(textColor, AdminTheme.ALPHA_HOVER));
        }

        // Disabled overlay
        if (disabled) {
            fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S,
                    withAlpha(AdminColors.TEXT_MUTED, AdminTheme.ALPHA_DISABLED));
        }

        // Label
        drawCenteredText(graphics, font, label, x, y, width, height,
                AdminTheme.TEXT_BODY, textColor, true);
    }

    // ===== INPUTS =====

    public enum InputState {
        NORMAL, FOCUSED, ERROR, DISABLED
    }

    public static void drawTextField(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                     String text, String placeholder, InputState state, int cursorPos, boolean showCursor,
                                     int selStart, int selEnd) {
        int outlineColor;
        int outlineThickness = 1;
        switch (state) {
            case FOCUSED -> { outlineColor = AdminColors.ACCENT; outlineThickness = 2; }
            case ERROR -> { outlineColor = AdminColors.ERROR; outlineThickness = 2; }
            case DISABLED -> { outlineColor = AdminColors.BORDER; }
            default -> { outlineColor = AdminColors.DIVIDER; }
        }

        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_S, AdminColors.BG_INPUT);
        fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_S, outlineThickness, outlineColor);

        String displayText = text.isEmpty() ? placeholder : text;
        int textColor = text.isEmpty() ? AdminColors.TEXT_MUTED : AdminColors.TEXT_PRIMARY;

        // Render selection highlight
        boolean hasSelection = selStart != selEnd && !text.isEmpty();
        if (hasSelection) {
            int selMin = Math.min(selStart, selEnd);
            int selMax = Math.max(selStart, selEnd);
            float scale = AdminTheme.TEXT_BODY / (float) font.lineHeight;
            int textX = x + AdminTheme.SPACE_S;
            int selStartX = textX + (int) (font.width(displayText.substring(0, Math.min(selMin, displayText.length()))) * scale);
            int selEndX = textX + (int) (font.width(displayText.substring(0, Math.min(selMax, displayText.length()))) * scale);
            graphics.fill(selStartX, y + 4, selEndX, y + height - 4, 0x60FFFFFF);
        }

        // Account for text scaling in cursor position calculation
        float scale = AdminTheme.TEXT_BODY / (float) font.lineHeight;
        int textX = x + AdminTheme.SPACE_S;
        int cursorX = x + AdminTheme.SPACE_S + (int) (font.width(text.substring(0, Math.min(cursorPos, text.length()))) * scale);

        drawLeftAlignedText(graphics, font, displayText, x + AdminTheme.SPACE_S, y, AdminTheme.TEXT_BODY, textColor, height);

        if (state == InputState.FOCUSED && showCursor) {
            graphics.fill(cursorX, y + 4, cursorX + 1, y + height - 4, AdminColors.ACCENT);
        }
    }

    public static void drawSearchBar(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                         String query, String placeholder, boolean focused, int selStart, int selEnd) {
            int textX = x + AdminTheme.SPACE_S;
            int textW = width - AdminTheme.SPACE_S * 2;

            InputState state = focused ? InputState.FOCUSED : InputState.NORMAL;
            drawTextField(graphics, font, x, y, width, height, query, placeholder, state, query.length(), focused, selStart, selEnd);
        }

    // ===== TOGGLE =====

    public static void drawToggle(GuiGraphics graphics, com.example.mobilecellnetwork.gui.PhoneStyle style, int x, int y, boolean on, boolean hovered, boolean disabled) {
            McnPrimitives.drawToggle(graphics, style, x, y, on, hovered, disabled);
    }

    // ===== TABS =====

    public static void drawTab(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                               String label, boolean active, boolean hovered) {
        int textColor = active ? AdminColors.TEXT_PRIMARY : AdminColors.TEXT_SECONDARY;
        if (hovered && !active) {
            textColor = AdminColors.TEXT_PRIMARY;
        }

        drawCenteredText(graphics, font, label, x, y, width, height, AdminTheme.TEXT_BODY, textColor, true);

        if (active) {
            graphics.fill(x, y + height - 2, x + width, y + height, AdminColors.TAB_ACTIVE_BAR);
        }
    }

    // ===== TOASTS =====

    public static void drawToast(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                                 String message, int accentColor) {
        fillRoundedRect(graphics, x, y, width, height, AdminTheme.RADIUS_M, AdminColors.TOAST_BG);
        fillRoundedRectOutline(graphics, x, y, width, height, AdminTheme.RADIUS_M, 1, AdminColors.BORDER);
        graphics.fill(x, y, x + 4, y + height, accentColor);

        drawLeftAlignedText(graphics, font, message, x + AdminTheme.SPACE_M, y, AdminTheme.TEXT_BODY,
                AdminColors.TEXT_PRIMARY, height);
    }

    // ===== PAGINATION =====

    public static void drawPageIndicator(GuiGraphics graphics, Font font, int x, int y, int width,
                                         int currentPage, int totalPages) {
        String text = "Page " + currentPage + " of " + totalPages;
        drawCenteredText(graphics, font, text, x, y, width, AdminTheme.TEXT_CAPTION, AdminTheme.TEXT_CAPTION,
                AdminColors.TEXT_MUTED, false);
    }

    // ===== SCROLLBAR =====

    public static void drawScrollbar(GuiGraphics graphics, int x, int y, int width, int height,
                                     float scrollFraction, float visibleFraction) {
        int trackRadius = 2;
        fillRoundedRect(graphics, x, y, width, height, trackRadius, AdminColors.SCROLL_TRACK);

        int thumbHeight = Math.max(20, (int) (height * visibleFraction));
        int thumbY = y + (int) ((height - thumbHeight) * scrollFraction);
        fillRoundedRect(graphics, x, thumbY, width, thumbHeight, trackRadius, AdminColors.SCROLL_THUMB);
    }

    // ===== DIVIDER =====

    public static void drawDivider(GuiGraphics graphics, int x, int y, int width, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
    }

    // ===== HELPERS =====

    private static void drawCenteredText(GuiGraphics graphics, Font font, String text, int x, int y, int width, int height,
                                         int textSize, int color, boolean shadow) {
        if (text == null || text.isEmpty() || width <= 0 || height <= 0) return;
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2.0, y + height / 2.0, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, color, shadow);
        graphics.pose().popPose();
    }

    private static void drawLeftAlignedText(GuiGraphics graphics, Font font, String text, int x, int y, int textSize, int color, int height) {
        if (text == null || text.isEmpty()) return;
        float scale = textSize / (float) font.lineHeight;
        int textY = y + (height - (int) (font.lineHeight * scale)) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(x, textY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}