package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.gui.theme.McnIconRegistry;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;

public final class PhoneAppIcon {
    private PhoneAppIcon() {
    }

    public static void render(GuiGraphics graphics, Font font, PhoneScreen.AppShortcut shortcut,
                              int x, int y, int mouseX, int mouseY) {
        int tileSize = McnTheme.ICON_TILE_GRID;
        int width = tileSize + McnTheme.SPACE_XS;
        boolean hovered = mouseX >= x && mouseX < x + tileSize
                && mouseY >= y && mouseY < y + tileSize + McnTheme.SPACE_XXS + McnTheme.TEXT_CAPTION;
        var png = McnIconRegistry.hasPngTexture(shortcut.id())
                ? McnIconRegistry.getPngLocation(shortcut.id()) : null;
        McnPrimitives.drawIconTile(graphics, ClientPhoneCache.phoneStyle(), font, x, y, tileSize,
                hovered ? lighten(shortcut.color()) : shortcut.color(), png,
                McnIconRegistry.getGlyph(shortcut.id()));
        String label = shortcut.label();
        if (font.width(label) > width + 5) {
            label = font.plainSubstrByWidth(label, width) + "...";
        }

        graphics.drawCenteredString(font, label, x + tileSize / McnTheme.SPACE_XXS,
                y + tileSize + McnTheme.SPACE_XXS,
                PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
    }

    private static char glyph(String appId) {
        return switch (appId) {
            case "phone" -> 'P';
            case "sms" -> 'M';
            case "contacts" -> 'C';
            case "map" -> 'M';
            case "store" -> 'S';
            case "socials" -> 'S';
            case "bank" -> '$';
            case "settings" -> '*';
            case "plan" -> 'P';
            case "voicemail" -> 'V';
            case "camera" -> 'C';
            case "gallery" -> 'G';
            case "flashlight" -> 'F';
            default -> '?';
        };
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 0xFF) + 24);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 24);
        int blue = Math.min(255, (color & 0xFF) + 24);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static void drawGlyph(GuiGraphics graphics, String appId, int x, int y) {
        int white = 0xFFF8FBFF;
        switch (appId) {
            case "phone" -> {
                graphics.fill(x + 8, y + 7, x + 13, y + 11, white);
                graphics.fill(x + 19, y + 21, x + 24, y + 25, white);
                graphics.fill(x + 10, y + 8, x + 23, y + 24, white);
                graphics.fill(x + 12, y + 11, x + 21, y + 21, 0xFF23A979);
            }
            case "sms" -> {
                graphics.fill(x + 5, y + 7, x + 27, y + 23, white);
                graphics.fill(x + 9, y + 11, x + 23, y + 13, 0xFF11A8A0);
                graphics.fill(x + 9, y + 16, x + 20, y + 18, 0xFF11A8A0);
                graphics.fill(x + 7, y + 21, x + 12, y + 26, white);
            }
            case "contacts" -> {
                graphics.fill(x + 12, y + 6, x + 20, y + 14, white);
                graphics.fill(x + 8, y + 17, x + 24, y + 26, white);
                graphics.fill(x + 11, y + 19, x + 21, y + 25, 0xFF4376D4);
            }
            case "map" -> {
                graphics.renderOutline(x + 6, y + 6, 20, 20, white);
                graphics.fill(x + 12, y + 7, x + 14, y + 25, white);
                graphics.fill(x + 19, y + 7, x + 21, y + 25, white);
                graphics.fill(x + 16, y + 11, x + 19, y + 14, 0xFFE95757);
            }
            case "store" -> {
                graphics.fill(x + 7, y + 14, x + 25, y + 25, white);
                graphics.fill(x + 5, y + 9, x + 27, y + 15, 0xFFFFDB77);
                graphics.fill(x + 13, y + 18, x + 19, y + 25, 0xFFB95E36);
            }
            case "socials" -> {
                graphics.fill(x + 7, y + 8, x + 15, y + 16, white);
                graphics.fill(x + 18, y + 6, x + 26, y + 14, white);
                graphics.fill(x + 14, y + 18, x + 22, y + 26, white);
                graphics.fill(x + 12, y + 13, x + 21, y + 20, 0xFFAB63D5);
            }
            case "bank" -> {
                graphics.fill(x + 6, y + 11, x + 26, y + 14, white);
                graphics.fill(x + 8, y + 15, x + 11, y + 24, white);
                graphics.fill(x + 14, y + 15, x + 17, y + 24, white);
                graphics.fill(x + 21, y + 15, x + 24, y + 24, white);
                graphics.fill(x + 6, y + 25, x + 26, y + 27, white);
                graphics.fill(x + 8, y + 7, x + 24, y + 11, white);
            }
            case "settings" -> {
                graphics.fill(x + 14, y + 5, x + 18, y + 27, white);
                graphics.fill(x + 5, y + 14, x + 27, y + 18, white);
                graphics.fill(x + 8, y + 8, x + 24, y + 24, white);
                graphics.fill(x + 12, y + 12, x + 20, y + 20, 0xFF647486);
            }
            case "plan" -> {
                graphics.fill(x + 5, y + 8, x + 27, y + 24, white);
                graphics.fill(x + 7, y + 12, x + 25, y + 16, 0xFF5C74E8);
                graphics.fill(x + 9, y + 19, x + 17, y + 21, 0xFF5C74E8);
            }
            default -> graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    "?", x + 16, y + 12, white);
        }
    }
}
