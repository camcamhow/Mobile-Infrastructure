package com.example.mobilecellnetwork.gui.theme;

import com.example.mobilecellnetwork.gui.PhoneColors;
import com.example.mobilecellnetwork.gui.PhoneStyle;
import com.example.mobilecellnetwork.gui.PhoneFrame;
import com.example.mobilecellnetwork.gui.SnappleText;
import com.example.mobilecellnetwork.registry.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class McnPrimitives {
    private static final ResourceLocation SNAPPLE_WALLPAPER = ResourceLocation.fromNamespaceAndPath(
            "mobilecellnetwork", "textures/gui/snapple_wallpaper.png");
    private static final ResourceLocation SNAPPLE_FRAME = ResourceLocation.fromNamespaceAndPath(
            "mobilecellnetwork", "textures/gui/snapple_frame.png");
    private static final ResourceLocation G24_WALLPAPER = ResourceLocation.fromNamespaceAndPath(
            "mobilecellnetwork", "textures/gui/g24_wallpaper.png");
    private static final ResourceLocation G24_FRAME = ResourceLocation.fromNamespaceAndPath(
            "mobilecellnetwork", "textures/gui/g24_frame.png");

    private McnPrimitives() {
    }

    public static void fillRoundedRect(GuiGraphics graphics, int x, int y, int width, int height,
                                       int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int clampedRadius = Math.clamp(radius, McnTheme.RADIUS_NONE,
                Math.min(width, height) / McnTheme.SPACE_XXS);
        if (clampedRadius == McnTheme.RADIUS_NONE) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        for (int row = 0; row < clampedRadius; row++) {
            int inset = roundedRowInset(row, height, clampedRadius);
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
            graphics.fill(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
        if (height > clampedRadius * 2) {
            graphics.fill(x, y + clampedRadius, x + width, y + height - clampedRadius, color);
        }
    }

    public static void fillRoundedRectOutline(GuiGraphics graphics, int x, int y, int width, int height,
                                              int radius, int thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }
        int clampedRadius = Math.clamp(radius, McnTheme.RADIUS_NONE,
                Math.min(width, height) / McnTheme.SPACE_XXS);
        int minDimension = Math.min(width, height);
        if (thickness >= (minDimension + McnTheme.DIVIDER_THICKNESS) / McnTheme.SPACE_XXS) {
            fillRoundedRect(graphics, x, y, width, height, clampedRadius, color);
            return;
        }

        int innerHeight = height - thickness * McnTheme.SPACE_XXS;
        int innerRadius = Math.max(McnTheme.RADIUS_NONE, clampedRadius - thickness);
        for (int row = McnTheme.RADIUS_NONE; row < height; row += McnTheme.DIVIDER_THICKNESS) {
            int outerInset = roundedRowInset(row, height, clampedRadius);
            int outerLeft = x + outerInset;
            int outerRight = x + width - outerInset;
            int rowY = y + row;
            if (row < thickness || row >= height - thickness) {
                graphics.fill(outerLeft, rowY, outerRight, rowY + McnTheme.DIVIDER_THICKNESS, color);
                continue;
            }

            int innerInset = roundedRowInset(row - thickness, innerHeight, innerRadius);
            int innerLeft = x + thickness + innerInset;
            int innerRight = x + width - thickness - innerInset;
            int leftEnd = Math.min(outerRight, innerLeft);
            int rightStart = Math.max(outerLeft, innerRight);
            if (leftEnd > outerLeft) {
                graphics.fill(outerLeft, rowY, leftEnd, rowY + McnTheme.DIVIDER_THICKNESS, color);
            }
            if (outerRight > rightStart) {
                graphics.fill(rightStart, rowY, outerRight, rowY + McnTheme.DIVIDER_THICKNESS, color);
            }
        }
    }

    public static void drawCard(GuiGraphics graphics, PhoneStyle style, int x, int y, int width, int height) {
        fillRoundedRect(graphics, x, y, width, height, McnTheme.RADIUS_L, PhoneColors.bgSecondary(style));
        if (style != PhoneStyle.SAMSUNG_ONE_UI) {
            fillRoundedRectOutline(graphics, x, y, width, height, McnTheme.RADIUS_L,
                    McnTheme.DIVIDER_THICKNESS, PhoneColors.divider(style));
        }
    }

    public static void drawElevatedCard(GuiGraphics graphics, PhoneStyle style,
                                        int x, int y, int width, int height) {
        fillRoundedRect(graphics, x + McnTheme.ELEV_1, y + McnTheme.ELEV_1,
                width, height, McnTheme.RADIUS_L, PhoneColors.shadow(style));
        drawCard(graphics, style, x, y, width, height);
    }

    public static void drawDivider(GuiGraphics graphics, PhoneStyle style, int x, int y, int width) {
        if (width > 0) {
            graphics.fill(x, y, x + width, y + McnTheme.DIVIDER_THICKNESS,
                    PhoneColors.divider(style));
        }
    }

    public static void drawButton(GuiGraphics graphics, PhoneStyle style, Font font,
                                  int x, int y, int width, int height, String label,
                                  ButtonVariant variant, boolean hovered, boolean disabled) {
        int fillColor = switch (variant) {
            case PRIMARY -> PhoneColors.accent(style);
            case SECONDARY -> PhoneColors.bgTertiary(style);
            case TERTIARY -> 0;
            case DESTRUCTIVE -> PhoneColors.error(style);
        };
        int radius = height / 2;
        if (variant != ButtonVariant.TERTIARY) {
            fillRoundedRect(graphics, x, y, width, height, radius, fillColor);
            if (disabled) {
                fillRoundedRect(graphics, x, y, width, height, radius,
                        withAlpha(PhoneColors.SAMSUNG_BG_PRIMARY, McnTheme.ALPHA_DISABLED));
            } else if (hovered) {
                fillRoundedRect(graphics, x, y, width, height, radius,
                        withAlpha(PhoneColors.buttonTextOnAccent(style), McnTheme.ALPHA_HOVER));
            }
        }

        int textColor = disabled ? PhoneColors.textSecondary(style) : switch (variant) {
            case PRIMARY -> PhoneColors.buttonTextOnAccent(style);
            case DESTRUCTIVE -> PhoneColors.buttonTextOnDestructive(style);
            case SECONDARY -> PhoneColors.buttonTextOnSecondary(style);
            case TERTIARY -> PhoneColors.accent(style);
        };
        drawCenteredText(graphics, font, label, x, y, width, height,
                McnTheme.TEXT_BODY, textColor, true);
    }

    public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return width > 0 && height > 0 && mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    public static void drawChip(GuiGraphics graphics, PhoneStyle style, Font font,
                                int x, int y, int width, int height, String label,
                                int fillColor, int textColor) {
        fillRoundedRect(graphics, x, y, width, height, McnTheme.RADIUS_ROUND, fillColor);
        drawCenteredText(graphics, font, label, x, y, width, height,
                McnTheme.TEXT_CAPTION, textColor, false);
    }

    public static void drawBadge(GuiGraphics graphics, PhoneStyle style, Font font,
                                 int x, int y, int width, int height, int count) {
        fillRoundedRect(graphics, x, y, width, height, McnTheme.RADIUS_ROUND,
                PhoneColors.error(style));
        String label = count >= 100 ? "99+" : Integer.toString(count);
        drawCenteredText(graphics, font, label, x, y, width, height,
                McnTheme.TEXT_FOOTNOTE, 0xFFFFFFFF, true);
    }

    public static void drawIconTile(GuiGraphics graphics, PhoneStyle style, Font font,
                                    int x, int y, int size, int backgroundColor, String glyph) {
        drawIconTile(graphics, style, font, x, y, size, backgroundColor, null, glyph);
    }

    public static void drawIconTile(GuiGraphics graphics, PhoneStyle style, Font font,
                                    int x, int y, int size, int backgroundColor,
                                    ResourceLocation png, String glyph) {
            if (png != null) {
                // Render PNG directly without background box
                int padding = size / McnTheme.SPACE_S;
                int iconSize = size - padding * McnTheme.SPACE_XXS;
                if (iconSize <= McnTheme.RADIUS_NONE) {
                    return;
                }
                int iconWidth = style == PhoneStyle.SAMSUNG_ONE_UI
                        ? Math.min(size, PhoneFrame.circularElementWidth(iconSize)) : iconSize;
                int iconX = x + (size - iconWidth) / 2;
                int iconY = y + padding;
                int texSize = 512;
                graphics.blit(png, iconX, iconY, iconWidth, iconSize,
                        0.0F, 0.0F, texSize, texSize, texSize, texSize);
                return;
            }
        
            // Fallback to glyph with background
            fillRoundedRect(graphics, x, y, size, size, size / McnTheme.RADIUS_S, backgroundColor);
            drawCenteredText(graphics, font, glyph, x, y, size, size,
                    size / McnTheme.SPACE_XXS, 0xFFFFFFFF, true);
    }

    private static int roundedRowInset(int row, int height, int radius) {
        if (radius <= McnTheme.RADIUS_NONE || row < McnTheme.RADIUS_NONE || row >= height) {
            return McnTheme.RADIUS_NONE;
        }
        int cornerRow = row < radius ? row : row >= height - radius ? height - McnTheme.DIVIDER_THICKNESS - row
                : radius;
        if (cornerRow >= radius) {
            return McnTheme.RADIUS_NONE;
        }
        double halfPixel = McnTheme.DIVIDER_THICKNESS / (double) McnTheme.SPACE_XXS;
        double distanceY = radius - cornerRow - halfPixel;
        double distanceX = Math.sqrt((double) radius * radius - distanceY * distanceY);
        return Math.clamp((int) Math.ceil(radius - distanceX - halfPixel),
                McnTheme.RADIUS_NONE, radius);
    }

    private static int withAlpha(int color, int alpha) {
        int maxAlpha = 255;
        return (color & 0x00FFFFFF) | (Math.clamp(alpha, McnTheme.RADIUS_NONE, maxAlpha) << 24);
    }

    /**
     * Draws text centered in a rectangle at an approximate pixel size.
     *
     * The text is scaled by (textSize / font.lineHeight), which means
     * non-integer scales (e.g. 12 / 9 = 1.33) will render slightly
     * softer than native text. Use text sizes that are multiples of
     * font.lineHeight when pixel-perfect rendering matters.
     */
    private static void drawCenteredText(GuiGraphics graphics, Font font, String text,
                                         int x, int y, int width, int height,
                                         int textSize, int color, boolean shadow) {
        if (text == null || textSize <= McnTheme.RADIUS_NONE || width <= 0 || height <= 0) {
            return;
        }
        float scale = textSize / (float) font.lineHeight;
        if (com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI
                && com.example.mobilecellnetwork.gui.PhoneFrame.isDrawingPhone()) {
            scale = Math.min(scale, Math.max(0.1F, (width - 8.0F) / Math.max(1, font.width(text))));
            scale = Math.min(scale, Math.max(0.1F, (height - 4.0F) / font.lineHeight));
            shadow = false;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / (double) McnTheme.SPACE_XXS,
                y + height / (double) McnTheme.SPACE_XXS, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        graphics.drawString(font, text, -font.width(text) / McnTheme.SPACE_XXS,
                -font.lineHeight / McnTheme.SPACE_XXS, color, shadow);
        graphics.pose().popPose();
    }

    public enum ButtonVariant {
        PRIMARY,
        SECONDARY,
        TERTIARY,
        DESTRUCTIVE
    }

    public enum TextFieldState {
        NORMAL,
        FOCUSED,
        ERROR,
        DISABLED
    }

    public enum ProgressBarStyle {
        LINEAR,
        ROUNDED
    }

    private static final int AVATAR_CORAL = 0xFFFF6F61;
    private static final int AVATAR_SKY = 0xFF4DA3FF;
    private static final int AVATAR_MINT = 0xFF5BC8A5;
    private static final int AVATAR_LAVENDER = 0xFF9B7EDE;
    private static final int AVATAR_AMBER = 0xFFFFB547;
    private static final int AVATAR_ROSE = 0xFFE46B9B;
    private static final int[] AVATAR_PALETTE = {
            AVATAR_CORAL, AVATAR_SKY, AVATAR_MINT, AVATAR_LAVENDER, AVATAR_AMBER, AVATAR_ROSE
    };

    public static void drawToggle(GuiGraphics graphics, PhoneStyle style,
                                  int x, int y, boolean on, boolean hovered, boolean disabled) {
        int width = McnTheme.TOGGLE_WIDTH;
        int height = McnTheme.TOGGLE_HEIGHT;
        int radius = height / McnTheme.SPACE_XXS;
        int trackColor = on ? PhoneColors.success(style) : PhoneColors.bgTertiary(style);
        fillRoundedRect(graphics, x, y, width, height, radius, trackColor);
        if (disabled) {
            fillRoundedRect(graphics, x, y, width, height, radius,
                    withAlpha(PhoneColors.SAMSUNG_BG_PRIMARY, McnTheme.ALPHA_DISABLED));
        } else if (hovered) {
            fillRoundedRect(graphics, x, y, width, height, radius,
                    withAlpha(PhoneColors.buttonTextOnAccent(style), McnTheme.ALPHA_HOVER));
        }

        int knobDiameter = height - McnTheme.SPACE_XXS * McnTheme.SPACE_XXS;
        int knobRadius = knobDiameter / McnTheme.SPACE_XXS;
        int padding = McnTheme.SPACE_XXS;
        int knobCenterX = on
                ? x + width - padding - knobRadius
                : x + padding + knobRadius;
        int knobCenterY = y + height / McnTheme.SPACE_XXS;
        fillCircle(graphics, knobCenterX, knobCenterY, knobRadius, PhoneColors.SAMSUNG_TEXT_PRIMARY);
    }

    public static void drawTextField(GuiGraphics graphics, PhoneStyle style, Font font,
                                     int x, int y, int width, int height,
                                     String text, String placeholder, TextFieldState state,
                                     int cursorPos, boolean showCursor, int selStart, int selEnd) {
        fillRoundedRect(graphics, x, y, width, height, McnTheme.RADIUS_M,
                PhoneColors.bgSecondary(style));
        int borderThickness = state == TextFieldState.FOCUSED || state == TextFieldState.ERROR
                ? McnTheme.SPACE_XXS : McnTheme.DIVIDER_THICKNESS;
        int borderColor = switch (state == null ? TextFieldState.NORMAL : state) {
            case NORMAL, DISABLED -> PhoneColors.divider(style);
            case FOCUSED -> PhoneColors.accent(style);
            case ERROR -> PhoneColors.error(style);
        };
        fillRoundedRectOutline(graphics, x, y, width, height, McnTheme.RADIUS_M,
                borderThickness, borderColor);

        String value = text == null ? "" : text;
        boolean showingPlaceholder = value.isEmpty() && placeholder != null;
        String displayText = showingPlaceholder ? placeholder : value;
        int textColor = showingPlaceholder
                ? PhoneColors.textSecondary(style) : PhoneColors.textPrimary(style);
        int padding = McnTheme.SPACE_S;
        int availableWidth = Math.max(McnTheme.RADIUS_NONE,
                width - padding * McnTheme.SPACE_XXS);
        int clampedCursor = Math.clamp(cursorPos, McnTheme.RADIUS_NONE, value.length());
        int cursorTextWidth = font.width(value.substring(McnTheme.RADIUS_NONE, clampedCursor));
        int scrollOffset = Math.max(McnTheme.RADIUS_NONE, cursorTextWidth - availableWidth);
        int textX = x + padding - scrollOffset;
        int textY = y + (height - font.lineHeight) / McnTheme.SPACE_XXS;

        // Render selection highlight
        boolean hasSelection = selStart != selEnd && !value.isEmpty();
        if (hasSelection) {
            int selMin = Math.min(selStart, selEnd);
            int selMax = Math.max(selStart, selEnd);
            int selStartX = textX + font.width(value.substring(0, Math.min(selMin, value.length())));
            int selEndX = textX + font.width(value.substring(0, Math.min(selMax, value.length())));
            graphics.fill(selStartX, y + McnTheme.SPACE_XXS, selEndX, y + height - McnTheme.SPACE_XXS,
                    0x60FFFFFF);
        }

        if (availableWidth > McnTheme.RADIUS_NONE) {
            graphics.enableScissor(x + padding, y, x + width - padding, y + height);
            graphics.drawString(font, displayText, textX, textY, textColor, true);
            if (showCursor) {
                int cursorInset = McnTheme.SPACE_XXS + McnTheme.DIVIDER_THICKNESS;
                int cursorHeight = Math.max(McnTheme.RADIUS_NONE,
                        height - cursorInset * McnTheme.SPACE_XXS);
                int cursorX = textX + cursorTextWidth;
                graphics.fill(cursorX, y + cursorInset, cursorX + McnTheme.DIVIDER_THICKNESS,
                        y + cursorInset + cursorHeight, PhoneColors.accent(style));
            }
            graphics.disableScissor();
        }

        if (state == TextFieldState.DISABLED) {
            fillRoundedRect(graphics, x, y, width, height, McnTheme.RADIUS_M,
                    withAlpha(PhoneColors.SAMSUNG_BG_PRIMARY, McnTheme.ALPHA_DISABLED));
        }
    }

    public static void drawProgressBar(GuiGraphics graphics, PhoneStyle style,
                                       int x, int y, int width, int height,
                                       float progress, ProgressBarStyle barStyle) {
        float clampedProgress = Math.clamp(progress, 0.0F, 1.0F);
        int radius = barStyle == ProgressBarStyle.LINEAR
                ? McnTheme.RADIUS_XS : height / McnTheme.SPACE_XXS;
        fillRoundedRect(graphics, x, y, width, height, radius, PhoneColors.bgTertiary(style));
        if (clampedProgress <= 0.0F) {
            return;
        }
        int fillWidth = clampedProgress >= 1.0F ? width : Math.round(width * clampedProgress);
        if (fillWidth > McnTheme.RADIUS_NONE) {
            fillRoundedRect(graphics, x, y, fillWidth, height, radius, PhoneColors.accent(style));
        }
    }

    public static void drawCircularProgress(GuiGraphics graphics, PhoneStyle style,
                                            int centerX, int centerY, int radius, float progress,
                                            int trackColor, int fillColor) {
        if (radius <= McnTheme.RADIUS_NONE) {
            return;
        }
        float clampedProgress = Math.clamp(progress, 0.0F, 1.0F);
        int ringThickness = McnTheme.RADIUS_S;
        double innerRadius = Math.max(McnTheme.RADIUS_NONE, radius - ringThickness);
        double outerRadiusSquared = (double) radius * radius;
        double innerRadiusSquared = innerRadius * innerRadius;
        double halfPixel = McnTheme.DIVIDER_THICKNESS / (double) McnTheme.SPACE_XXS;
        double fullCircle = Math.PI * McnTheme.SPACE_XXS;

        for (int pixelY = centerY - radius; pixelY <= centerY + radius;
             pixelY += McnTheme.DIVIDER_THICKNESS) {
            for (int pixelX = centerX - radius; pixelX <= centerX + radius;
                 pixelX += McnTheme.DIVIDER_THICKNESS) {
                double deltaX = pixelX + halfPixel - centerX;
                double deltaY = pixelY + halfPixel - centerY;
                double distanceSquared = deltaX * deltaX + deltaY * deltaY;
                if (distanceSquared > outerRadiusSquared || distanceSquared < innerRadiusSquared) {
                    continue;
                }
                graphics.fill(pixelX, pixelY, pixelX + McnTheme.DIVIDER_THICKNESS,
                        pixelY + McnTheme.DIVIDER_THICKNESS, trackColor);
                if (clampedProgress <= 0.0F) {
                    continue;
                }
                double angle = Math.atan2(deltaX, -deltaY);
                if (angle < 0.0D) {
                    angle += fullCircle;
                }
                if (angle <= clampedProgress * fullCircle) {
                    graphics.fill(pixelX, pixelY, pixelX + McnTheme.DIVIDER_THICKNESS,
                            pixelY + McnTheme.DIVIDER_THICKNESS, fillColor);
                }
            }
        }
    }

    public static void drawAvatar(GuiGraphics graphics, PhoneStyle style, Font font,
                                  int x, int y, int size, String initials, int backgroundSeed) {
        if (size <= McnTheme.RADIUS_NONE) {
            return;
        }
        int centerX = x + size / McnTheme.SPACE_XXS;
        int centerY = y + size / McnTheme.SPACE_XXS;
        int radius = size / McnTheme.SPACE_XXS;
        int paletteIndex = Math.floorMod(backgroundSeed, AVATAR_PALETTE.length);
        fillCircle(graphics, centerX, centerY, radius, AVATAR_PALETTE[paletteIndex]);

        if (initials == null || initials.isBlank()) {
            int headRadius = size / McnTheme.SPACE_S;
            int headCenterY = y + size * McnTheme.SPACE_M / McnTheme.SPACE_XXL;
            fillCircle(graphics, centerX, headCenterY, headRadius, PhoneColors.SAMSUNG_TEXT_PRIMARY);
            int bodyWidth = size / McnTheme.SPACE_XXS;
            int bodyHeight = size / McnTheme.RADIUS_S;
            int bodyY = y + size / McnTheme.SPACE_XXS;
            fillRoundedRect(graphics, centerX - bodyWidth / McnTheme.SPACE_XXS, bodyY,
                    bodyWidth, bodyHeight, bodyHeight / McnTheme.SPACE_XXS,
                    PhoneColors.SAMSUNG_TEXT_PRIMARY);
            return;
        }

        String label = initials.substring(McnTheme.RADIUS_NONE,
                Math.min(initials.length(), McnTheme.SPACE_XXS)).toUpperCase(java.util.Locale.ROOT);
        drawCenteredText(graphics, font, label, x, y, size, size,
                size / McnTheme.SPACE_XXS, PhoneColors.SAMSUNG_TEXT_PRIMARY, true);
    }

    public static void drawIconTile(GuiGraphics graphics, PhoneStyle style, Font font,
                                    int x, int y, int size, int backgroundColor,
                                    String glyph, boolean gradient) {
        if (!gradient) {
            drawIconTile(graphics, style, font, x, y, size, backgroundColor, glyph);
            return;
        }
        if (size <= McnTheme.RADIUS_NONE) {
            return;
        }

        int maxChannel = PhoneColors.SAMSUNG_TEXT_PRIMARY >>> 24;
        int alpha = backgroundColor >>> 24;
        int red = backgroundColor >>> 16 & maxChannel;
        int green = backgroundColor >>> 8 & maxChannel;
        int blue = backgroundColor & maxChannel;
        int brightRed = Math.min(maxChannel, red + Math.round((maxChannel - red) * 0.15F));
        int brightGreen = Math.min(maxChannel, green + Math.round((maxChannel - green) * 0.15F));
        int brightBlue = Math.min(maxChannel, blue + Math.round((maxChannel - blue) * 0.15F));
        int darkRed = Math.round(red * (1.0F - 0.10F));
        int darkGreen = Math.round(green * (1.0F - 0.10F));
        int darkBlue = Math.round(blue * (1.0F - 0.10F));
        int brightColor = withAlpha((brightRed << 16) | (brightGreen << 8) | brightBlue, alpha);
        int darkColor = withAlpha((darkRed << 16) | (darkGreen << 8) | darkBlue, alpha);
        int cornerRadius = size / McnTheme.RADIUS_S;
        fillRoundedRect(graphics, x, y, size, size, cornerRadius, darkColor);

        int topHeight = size / McnTheme.SPACE_XXS;
        graphics.enableScissor(x, y, x + size, y + topHeight);
        fillRoundedRect(graphics, x, y, size, size, cornerRadius, brightColor);
        graphics.disableScissor();
        drawCenteredText(graphics, font, glyph, x, y, size, size,
                size / McnTheme.SPACE_XXS, PhoneColors.SAMSUNG_TEXT_PRIMARY, true);
    }

    private static void fillCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        if (radius <= McnTheme.RADIUS_NONE) {
            return;
        }
        double halfPixel = McnTheme.DIVIDER_THICKNESS / (double) McnTheme.SPACE_XXS;
        double radiusSquared = (double) radius * radius;
        for (int pixelY = centerY - radius; pixelY < centerY + radius;
             pixelY += McnTheme.DIVIDER_THICKNESS) {
            for (int pixelX = centerX - radius; pixelX < centerX + radius;
                 pixelX += McnTheme.DIVIDER_THICKNESS) {
                double deltaX = pixelX + halfPixel - centerX;
                double deltaY = pixelY + halfPixel - centerY;
                if (deltaX * deltaX + deltaY * deltaY <= radiusSquared) {
                    graphics.fill(pixelX, pixelY, pixelX + McnTheme.DIVIDER_THICKNESS,
                            pixelY + McnTheme.DIVIDER_THICKNESS, color);
                }
            }
        }
    }

    public static void drawPhoneFrame(GuiGraphics graphics, PhoneStyle style,
                                      int screenWidth, int screenHeight) {
        int phoneWidth = McnTheme.PHONE_WIDTH;
        int phoneHeight = McnTheme.PHONE_HEIGHT;
        int phoneX = (screenWidth - phoneWidth) / McnTheme.SPACE_XXS;
        int phoneY = (screenHeight - phoneHeight) / McnTheme.SPACE_XXS;

        if (style == PhoneStyle.IOS) {
            graphics.flush();
            Minecraft minecraft = Minecraft.getInstance();
            ItemStack phone = new ItemStack(ModItems.CELLPHONE.get());
            var renderer = minecraft.getItemRenderer();
            var model = renderer.getModel(phone, minecraft.level, minecraft.player, 0);
            graphics.pose().pushPose();
            graphics.pose().translate(phoneX + phoneWidth / 2.0F, phoneY + phoneHeight / 2.0F, -10.0F);
            graphics.pose().scale(phoneWidth / (0.90F * 71.6F / 147.6F), -phoneHeight / 0.90F, 1.0F);
            graphics.pose().mulPose(Axis.YP.rotationDegrees(180.0F));
            renderer.render(phone, ItemDisplayContext.NONE, false, graphics.pose(), graphics.bufferSource(),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
            graphics.pose().popPose();
            int screenX = getScreenX(screenWidth);
            int screenY = getScreenY(screenHeight);
            graphics.fill(screenX, screenY, screenX + getScreenWidth(), screenY + getScreenHeight(),
                    0xFF08090D);
            fillRoundedRect(graphics, screenX, screenY, getScreenWidth(), getScreenHeight(),
                    McnTheme.PHONE_RADIUS - McnTheme.PHONE_BEZEL, PhoneColors.bgPrimary(style));
            graphics.blit(SNAPPLE_FRAME, phoneX, phoneY, phoneWidth, phoneHeight,
                    0.0F, 0.0F, 880, 1600, 880, 1600);
            drawPhoneCamera(graphics, style, getScreenX(screenWidth), getScreenY(screenHeight), getScreenWidth());
            return;
        }

            if (style == PhoneStyle.SAMSUNG_ONE_UI) {
                int screenX = getScreenX(screenWidth);
                int screenY = getScreenY(screenHeight);
                fillRoundedRect(graphics, phoneX + 3, phoneY + 5, phoneWidth, phoneHeight,
                        McnTheme.PHONE_RADIUS, 0x55000000);
                fillRoundedRect(graphics, phoneX + 2, phoneY + 2, phoneWidth - 4, phoneHeight - 4,
                        McnTheme.PHONE_RADIUS - 2, PhoneColors.bgPrimary(style));
                graphics.blit(G24_FRAME, phoneX, phoneY, phoneWidth, phoneHeight,
                        0.0F, 0.0F, 880, 1600, 880, 1600);
                drawPhoneCamera(graphics, style, screenX, screenY, getScreenWidth());
                return;
            }

            int bodyColor = 0xFF454D5C;

            fillRoundedRect(graphics, phoneX + 3, phoneY + 5, phoneWidth, phoneHeight,
                    McnTheme.PHONE_RADIUS, 0x55000000);
            fillRoundedRect(graphics, phoneX, phoneY, phoneWidth, phoneHeight,
                    McnTheme.PHONE_RADIUS, bodyColor);
            fillRoundedRectOutline(graphics, phoneX, phoneY, phoneWidth, phoneHeight,
                    McnTheme.PHONE_RADIUS, McnTheme.DIVIDER_THICKNESS, 0xFF2A2A2A);
            fillRoundedRectOutline(graphics,
                    phoneX + McnTheme.DIVIDER_THICKNESS, phoneY + McnTheme.DIVIDER_THICKNESS,
                    phoneWidth - McnTheme.DIVIDER_THICKNESS * McnTheme.SPACE_XXS,
                    phoneHeight - McnTheme.DIVIDER_THICKNESS * McnTheme.SPACE_XXS,
                    McnTheme.PHONE_RADIUS - McnTheme.DIVIDER_THICKNESS, McnTheme.DIVIDER_THICKNESS,
                    0xFF1A1A1A);
            graphics.fill(phoneX + McnTheme.PHONE_RADIUS, phoneY + McnTheme.DIVIDER_THICKNESS,
                    phoneX + phoneWidth - McnTheme.PHONE_RADIUS,
                    phoneY + McnTheme.DIVIDER_THICKNESS * McnTheme.SPACE_XXS, 0xFF1A1A1A);

            int screenX = getScreenX(screenWidth);
                    int screenY = getScreenY(screenHeight);
                    int screenAreaWidth = getScreenWidth();
                    int screenAreaHeight = getScreenHeight();
                    // Screen radius = phone radius - bezel for perfect corner alignment
                    int screenRadius = McnTheme.PHONE_RADIUS - McnTheme.PHONE_BEZEL;
                    fillRoundedRect(graphics, screenX, screenY, screenAreaWidth, screenAreaHeight,
                            screenRadius, PhoneColors.bgPrimary(style));

            drawPhoneCamera(graphics, style, screenX, screenY, screenAreaWidth);
        }

    public static void drawPhoneWallpaper(GuiGraphics graphics, PhoneStyle style,
                                          int screenX, int screenY, int width, int height) {
        if (style == PhoneStyle.IOS) {
            fillRoundedRect(graphics, screenX, screenY, width, height,
                    McnTheme.PHONE_RADIUS - McnTheme.PHONE_BEZEL, 0xFF08090D);
            graphics.blit(SNAPPLE_WALLPAPER, screenX, screenY, width, height,
                    0.0F, 0.0F, 512, 960, 512, 960);
            drawPhoneCamera(graphics, style, screenX, screenY, width);
            return;
        }
        graphics.blit(G24_WALLPAPER, screenX, screenY, width, height,
                0.0F, 0.0F, 512, 960, 512, 960);
        drawPhoneCamera(graphics, style, screenX, screenY, width);
    }

    public static void drawPhoneCamera(GuiGraphics graphics, PhoneStyle style,
                                        int screenX, int screenY, int width) {
        int cameraWidth = style == PhoneStyle.IOS ? McnTheme.PHONE_NOTCH_WIDTH : 10;
        int cameraHeight = style == PhoneStyle.IOS ? McnTheme.PHONE_NOTCH_HEIGHT : 10;
        int x = screenX + (width - cameraWidth) / 2;
        int y = screenY + McnTheme.SPACE_XS;
        fillRoundedRect(graphics, x, y, cameraWidth, cameraHeight, cameraHeight / 2, 0xFF080A0F);
        fillRoundedRect(graphics, x + cameraWidth - 8, y + 3, 4, 4, 2, 0xFF28354D);
    }

    public static int getScreenX(int screenWidth) {
        return (screenWidth - McnTheme.PHONE_WIDTH) / McnTheme.SPACE_XXS + McnTheme.PHONE_BEZEL;
    }

    public static int getScreenY(int screenHeight) {
        return (screenHeight - McnTheme.PHONE_HEIGHT) / McnTheme.SPACE_XXS + McnTheme.PHONE_BEZEL;
    }

    public static int getScreenWidth() {
        return McnTheme.PHONE_WIDTH - McnTheme.PHONE_BEZEL * McnTheme.SPACE_XXS;
    }

    public static int getScreenHeight() {
        return McnTheme.PHONE_HEIGHT - McnTheme.PHONE_BEZEL * McnTheme.SPACE_XXS;
    }

    public static int getContentTop(int screenY) {
        return screenY + McnTheme.STATUS_BAR_HEIGHT + McnTheme.PHONE_NOTCH_HEIGHT
                + McnTheme.SPACE_S + McnTheme.TEXT_TITLE + McnTheme.SPACE_S;
    }

    public static int getContentBottom(int screenY, int screenHeight) {
        return screenY + screenHeight - McnTheme.HOME_INDICATOR_HEIGHT - McnTheme.SPACE_L;
    }

    public static int getContentLeft(int screenX) {
        return screenX + McnTheme.SPACE_M;
    }

    public static int getContentRight(int screenX, int screenWidth) {
        return screenX + screenWidth - McnTheme.SPACE_M;
    }

    public static void drawHeader(GuiGraphics graphics, PhoneStyle style, Font font,
                                  int x, int y, int width, String title, boolean showBackChevron) {
        float titleScale = McnTheme.TEXT_TITLE / (float) font.lineHeight;
        int titleX = x + (showBackChevron ? McnTheme.SPACE_L + McnTheme.SPACE_XS
                : style == PhoneStyle.SAMSUNG_ONE_UI ? McnTheme.SPACE_M : McnTheme.RADIUS_NONE);
        if (style == PhoneStyle.SAMSUNG_ONE_UI) {
            int maxWidth = Math.max(0, (int) ((width - (titleX - x) - McnTheme.SPACE_M) / titleScale));
            title = font.plainSubstrByWidth(title, maxWidth);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(titleX, y + McnTheme.SPACE_XS, McnTheme.ELEV_0);
        graphics.pose().scale(titleScale, titleScale, McnTheme.ELEV_1);
        if (style == PhoneStyle.IOS) {
            SnappleText.draw(graphics, font, title, 0, 0, PhoneColors.textPrimary(style));
        } else {
            graphics.drawString(font, title, 0, 0, PhoneColors.textPrimary(style),
                    style != PhoneStyle.SAMSUNG_ONE_UI);
        }
        graphics.pose().popPose();

        if (showBackChevron) {
                // Draw proper back button with background
                int btnX = x + McnTheme.SPACE_S;
                int btnY = y + McnTheme.SPACE_XXS;
                int btnSize = McnTheme.BUTTON_HEIGHT_MD; // 28px
                int btnRadius = McnTheme.RADIUS_S; // 4px
                // Button background
                fillRoundedRect(graphics, btnX, btnY, btnSize, btnSize, btnRadius,
                        PhoneColors.bgTertiary(style));
                // Arrow glyph (using a proper left arrow)
                float glyphScale = (McnTheme.TEXT_BODY * 1.2f) / (float) font.lineHeight;
                graphics.pose().pushPose();
                graphics.pose().translate(btnX + btnSize / 2.0, btnY + btnSize / 2.0, McnTheme.ELEV_1);
                graphics.pose().scale(glyphScale, glyphScale, McnTheme.ELEV_1);
                graphics.drawString(font, "\u2190", -font.width("\u2190") / 2, -font.lineHeight / 2,
                        PhoneColors.accent(style), false);
                graphics.pose().popPose();
            }
        }

    public static void drawHomeIndicator(GuiGraphics graphics, PhoneStyle style,
                                         int screenX, int screenY,
                                         int screenWidth, int screenHeight) {
            // Draw a more distinct home button - rounded square with home glyph
            int indicatorSize = McnTheme.HOME_INDICATOR_WIDTH / 2; // ~40px
            int x = screenX + (screenWidth - indicatorSize) / 2;
            int y = screenY + screenHeight - indicatorSize - McnTheme.SPACE_XS;
        
            // Button background - rounded square
            int radius = McnTheme.RADIUS_M; // 8px
            fillRoundedRect(graphics, x, y, indicatorSize, indicatorSize, radius,
                    PhoneColors.bgTertiary(style));
        
            // Home glyph (house icon)
            float glyphScale = (McnTheme.TEXT_BODY * 1.5f) / (float) Minecraft.getInstance().font.lineHeight;
            Font font = Minecraft.getInstance().font;
            graphics.pose().pushPose();
            graphics.pose().translate(x + indicatorSize / 2.0, y + indicatorSize / 2.0, McnTheme.ELEV_1);
            graphics.pose().scale(glyphScale, glyphScale, McnTheme.ELEV_1);
            graphics.drawString(font, "\u2302", -font.width("\u2302") / 2, -font.lineHeight / 2,
                    PhoneColors.textSecondary(style), false);
            graphics.pose().popPose();
        }

    public static void drawStatusBar(GuiGraphics graphics, PhoneStyle style, Font font,
                                         int screenX, int screenY, int screenWidth,
                                         String carrier, String time, int signalBars,
                                         int batteryPercent, boolean isRoaming, boolean hasService) {
            if (style == PhoneStyle.IOS) {
                drawSnappleStatusBar(graphics, font, screenX, screenY, screenWidth, time,
                        signalBars, batteryPercent, isRoaming, hasService, PhoneColors.dark(style));
                return;
            }
            // Notch bottom is at screenY + SPACE_XS + PHONE_NOTCH_HEIGHT; start content below it
            int notchBottom = screenY + McnTheme.SPACE_XS + McnTheme.PHONE_NOTCH_HEIGHT;
            int contentY = notchBottom + McnTheme.SPACE_XXS;
            int leftMargin = McnTheme.SPACE_M;
            int rightMargin = McnTheme.SPACE_M;
            int minGap = McnTheme.SPACE_S;

                // Calculate right zone first (fixed width) - signal bars + battery + battery text
                int barCount = 4;
                int barWidth = 3;
                int barGap = 1;
                int barsWidth = barCount * barWidth + (barCount - 1) * barGap;
                int maxBarHeight = 10;
                int batteryWidth = McnTheme.SPACE_M + McnTheme.SPACE_S;
                int batteryHeight = McnTheme.SPACE_S + McnTheme.SPACE_XXS;
                int nubWidth = McnTheme.SPACE_XXS;
                int nubHeight = McnTheme.SPACE_XXS * McnTheme.SPACE_XXS;
                String batteryText = Math.clamp(batteryPercent, 0, 100) + "%";
                int batteryTextWidth = Math.round(font.width(batteryText)
                        * (McnTheme.TEXT_FOOTNOTE / (float) font.lineHeight));
                int rightZoneWidth = barsWidth + McnTheme.SPACE_XS + batteryWidth + nubWidth
                        + McnTheme.SPACE_XXS + batteryTextWidth;

                // Calculate left zone (carrier + optional roaming) with MAX WIDTH constraint
                                String carrierName = carrier == null ? "" : carrier;
                                float textScale = McnTheme.TEXT_CAPTION / (float) font.lineHeight;
            
                // Max left zone: screen width - margins - right zone - min gaps - min time width (40px)
                int minTimeWidth = 40;
                int maxLeftZoneWidth = screenWidth - leftMargin - rightMargin - rightZoneWidth - minGap * 2 - minTimeWidth;
                if (maxLeftZoneWidth < 40) maxLeftZoneWidth = 40; // Absolute minimum
            
                int carrierWidth = Math.round(font.width(carrierName) * textScale);
                int roamingWidth = 0;
                if (hasService && isRoaming) {
                    roamingWidth = Math.round(font.width("R") * (McnTheme.TEXT_FOOTNOTE / (float) font.lineHeight));
                }
            
                // Truncate carrier name if too wide
                int leftZoneWidth;
                String displayCarrier = carrierName;
                if (hasService) {
                    int neededWidth = carrierWidth + (isRoaming ? McnTheme.SPACE_XXS + roamingWidth : 0);
                    if (neededWidth > maxLeftZoneWidth) {
                        // Truncate carrier name with ellipsis
                        String ellipsis = "...";
                        int ellipsisWidth = Math.round(font.width(ellipsis) * textScale);
                        int maxCarrierWidth = maxLeftZoneWidth - (isRoaming ? McnTheme.SPACE_XXS + roamingWidth : 0) - ellipsisWidth;
                        if (maxCarrierWidth > 20) {
                            displayCarrier = font.plainSubstrByWidth(carrierName, (int)(maxCarrierWidth / textScale)) + ellipsis;
                            carrierWidth = Math.round(font.width(displayCarrier) * textScale);
                        } else {
                            displayCarrier = ellipsis;
                            carrierWidth = ellipsisWidth;
                        }

                    }
                    leftZoneWidth = carrierWidth + (isRoaming ? McnTheme.SPACE_XXS + roamingWidth : 0);
                } else {
                    int noServiceWidth = Math.round(font.width("No Service") * textScale);
                    leftZoneWidth = Math.min(noServiceWidth, maxLeftZoneWidth);
                }

                // Calculate available space for time (centered between left and right zones)
                int availableTimeWidth = Math.max(minTimeWidth, screenWidth - leftMargin - rightMargin - leftZoneWidth - rightZoneWidth - minGap * 2);

                // Draw left zone (carrier + roaming)
                if (hasService) {
                    int carrierLeftX = screenX + leftMargin;
                    drawCenteredText(graphics, font, displayCarrier, carrierLeftX, contentY, carrierWidth, McnTheme.STATUS_BAR_HEIGHT,
                            McnTheme.TEXT_CAPTION, PhoneColors.textPrimary(style), true);
                    if (isRoaming) {
                        int roamingLeftX = screenX + leftMargin + carrierWidth + McnTheme.SPACE_XXS;
                        drawCenteredText(graphics, font, "R", roamingLeftX, contentY, roamingWidth, McnTheme.STATUS_BAR_HEIGHT,
                                McnTheme.TEXT_FOOTNOTE, PhoneColors.accent(style), false);
                    }
                } else {
                    int noServiceWidth = Math.round(font.width("No Service") * textScale);
                    int noServiceLeftX = screenX + leftMargin;
                    drawCenteredText(graphics, font, "No Service", noServiceLeftX, contentY + McnTheme.SPACE_XXS,
                            noServiceWidth, McnTheme.STATUS_BAR_HEIGHT,
                            McnTheme.TEXT_CAPTION, PhoneColors.error(style), false);
                }

                // Draw time centered in the available space
                String displayedTime = time == null ? "" : time;
                                int timeLeftX = screenX + leftMargin + leftZoneWidth + minGap;
                                drawCenteredText(graphics, font, displayedTime, timeLeftX, contentY, availableTimeWidth, McnTheme.STATUS_BAR_HEIGHT,
                        McnTheme.TEXT_CAPTION, PhoneColors.textPrimary(style), true);

                // Draw right zone (signal bars + battery)
                int barsX = screenX + screenWidth - rightMargin - rightZoneWidth;
                int barBottom = contentY + (McnTheme.STATUS_BAR_HEIGHT + maxBarHeight) / 2;
                int filledBars = Math.clamp(signalBars, 0, barCount);
                int signalColor = isRoaming ? PhoneColors.success(style) : PhoneColors.accent(style);

                // Draw signal bars
                for (int bar = 0; bar < barCount; bar++) {
                    int barHeight = 4 + bar * 2;
                    int barX = barsX + bar * (barWidth + barGap);
                    int barY = barBottom - barHeight;
                    graphics.fill(barX, barY, barX + barWidth, barBottom,
                            bar < filledBars ? signalColor : PhoneColors.bgTertiary(style));
                }

                // Draw battery
                int batteryX = barsX + barsWidth + McnTheme.SPACE_XS;
                int batteryY = contentY + (McnTheme.STATUS_BAR_HEIGHT - batteryHeight) / 2;
                int batteryColor = batteryPercent > 50 ? PhoneColors.success(style)
                        : batteryPercent >= 20 ? PhoneColors.warn(style) : PhoneColors.error(style);
                int batteryFillWidth = Math.round(Math.clamp(batteryPercent, 0, 100)
                        / 100.0F * (batteryWidth - McnTheme.SPACE_XXS * 2));
                fillRoundedRectOutline(graphics, batteryX, batteryY, batteryWidth, batteryHeight,
                        McnTheme.SPACE_XXS, McnTheme.DIVIDER_THICKNESS, PhoneColors.textPrimary(style));
                if (batteryFillWidth > 0) {
                    fillRoundedRect(graphics,
                            batteryX + McnTheme.DIVIDER_THICKNESS,
                            batteryY + McnTheme.DIVIDER_THICKNESS,
                            batteryFillWidth,
                            batteryHeight - McnTheme.DIVIDER_THICKNESS * 2,
                            McnTheme.DIVIDER_THICKNESS, batteryColor);
                }
                int nubX = batteryX + batteryWidth;
                int nubY = batteryY + (batteryHeight - nubHeight) / 2;
            graphics.fill(nubX, nubY, nubX + nubWidth, nubY + nubHeight, PhoneColors.textPrimary(style));
            int batteryTextX = nubX + nubWidth + batteryTextWidth / 2;
            drawCenteredText(graphics, font, batteryText, batteryTextX, contentY,
                    batteryTextWidth, McnTheme.STATUS_BAR_HEIGHT, McnTheme.TEXT_FOOTNOTE,
                    PhoneColors.textPrimary(style), true);
        }

    public static void drawSnappleStatusBar(GuiGraphics graphics, Font font,
                                             int screenX, int screenY, int screenWidth, String time,
                                             int signalBars, int batteryPercent, boolean roaming,
                                             boolean hasService, boolean onWallpaper) {
        int color = onWallpaper ? 0xFFFFFFFF : 0xFF1C1C1E;
        int muted = onWallpaper ? 0x88FFFFFF : 0x66808080;
        SnappleText.draw(graphics, font, time == null ? "" : time, screenX + 13, screenY + 8, color);
        int barsX = screenX + screenWidth - 56;
        int bottom = screenY + 16;
        if (!hasService) {
            SnappleText.draw(graphics, font, "SOS", barsX - 5, screenY + 8, color);
        } else {
            if (roaming) {
                SnappleText.draw(graphics, font, "R", barsX - 8, screenY + 8, color);
            }

            for (int i = 0; i < 4; i++) {
                int barHeight = 3 + i * 2;
                graphics.fill(barsX + i * 4, bottom - barHeight, barsX + i * 4 + 3, bottom,
                        i < Math.clamp(signalBars, 0, 4) ? color : muted);
            }
        }
        int batteryX = screenX + screenWidth - 32;
        int batteryY = screenY + 7;
        fillRoundedRectOutline(graphics, batteryX, batteryY, 20, 10, 3, 1, color);
        graphics.fill(batteryX + 20, batteryY + 3, batteryX + 22, batteryY + 7, color);
        int fill = Math.round(Math.clamp(batteryPercent, 0, 100) * 16 / 100.0F);
        if (fill > 0) {
            fillRoundedRect(graphics, batteryX + 2, batteryY + 2, fill, 6, 2,
                    batteryPercent <= 20 ? 0xFFFF453A : color);
        }
    }

    public static void drawAndroidStatusBar(GuiGraphics graphics, Font font,
                                             int screenX, int screenY, int screenWidth, String time,
                                             int signalBars, int batteryPercent, boolean roaming,
                                             boolean hasService, boolean onWallpaper) {
        int color = onWallpaper ? 0xFFFFFFFF : PhoneColors.textPrimary(PhoneStyle.SAMSUNG_ONE_UI);
        int muted = onWallpaper ? 0x88FFFFFF : PhoneColors.textSecondary(PhoneStyle.SAMSUNG_ONE_UI);
        graphics.drawString(font, time == null ? "" : time, screenX + 12, screenY + 8, color, false);
        int barsX = screenX + screenWidth - 57;
        int bottom = screenY + 16;
        if (!hasService) {
            graphics.drawString(font, "!", barsX + 5, screenY + 7, color, false);
        } else {
            if (roaming) {
                graphics.drawString(font, "R", barsX - 9, screenY + 8, color, false);
            }
            for (int i = 0; i < 4; i++) {
                int barHeight = 3 + i * 2;
                graphics.fill(barsX + i * 4, bottom - barHeight, barsX + i * 4 + 3, bottom,
                        i < Math.clamp(signalBars, 0, 4) ? color : muted);
            }
        }
        int batteryX = screenX + screenWidth - 32;
        int batteryY = screenY + 7;
        fillRoundedRectOutline(graphics, batteryX, batteryY, 20, 10, 3, 1, color);
        graphics.fill(batteryX + 20, batteryY + 3, batteryX + 22, batteryY + 7, color);
        int fill = Math.round(Math.clamp(batteryPercent, 0, 100) * 16 / 100.0F);
        if (fill > 0) {
            fillRoundedRect(graphics, batteryX + 2, batteryY + 2, fill, 6, 2,
                    batteryPercent <= 20 ? 0xFFFF453A : color);
        }
    }

    public static boolean isInStatusBar(int screenY, int mouseY) {
        return mouseY >= screenY && mouseY < screenY + McnTheme.STATUS_BAR_HEIGHT;
    }
}
