package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.PhoneColors;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

public final class PhoneNavigation {
    private static final int BUTTON_WIDTH = 26;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 10;

    private PhoneNavigation() {
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        int screenX = PhoneFrame.getScreenX(screen);
        int screenY = PhoneFrame.getScreenY(screen);
        int centerX = screenX + PhoneFrame.getScreenW() / 2;
        int y = screenY + PhoneFrame.getScreenH() - BUTTON_HEIGHT - 4;
        int backX = centerX - BUTTON_GAP / 2 - BUTTON_WIDTH;
        int homeX = centerX + BUTTON_GAP / 2;
        PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
        drawButton(graphics, style, backX, y, "‹", false);
        drawButton(graphics, style, homeX, y, "", true);
    }

    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof PhoneNavigableScreen phoneScreen)) {
            return;
        }
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getHeight();
        Screen screen = minecraft.screen;
        double layoutX = PhoneFrame.layoutMouseX(screen, mouseX);
        double layoutY = PhoneFrame.layoutMouseY(screen, mouseY);
        int centerX = PhoneFrame.getScreenX(screen) + PhoneFrame.getScreenW() / 2;
        int y = PhoneFrame.getScreenY(screen) + PhoneFrame.getScreenH() - BUTTON_HEIGHT - 4;
        int backX = centerX - BUTTON_GAP / 2 - BUTTON_WIDTH;
        int homeX = centerX + BUTTON_GAP / 2;
        if (contains(layoutX, layoutY, backX, y)) {
            phoneScreen.navigateBack();
            event.setCanceled(true);
        } else if (contains(layoutX, layoutY, homeX, y)) {
            phoneScreen.navigateHome();
            event.setCanceled(true);
        }
    }

    private static void drawButton(GuiGraphics graphics, PhoneStyle style, int x, int y,
                                   String glyph, boolean home) {
            // Use McnPrimitives for consistent styling
            int btnWidth = BUTTON_WIDTH;
            int btnHeight = BUTTON_HEIGHT;
            int radius = 10;
        
            // Button background
            McnPrimitives.fillRoundedRect(graphics, x, y, btnWidth, btnHeight, radius,
                    PhoneColors.bgSecondary(style));
            // Button border
            McnPrimitives.fillRoundedRectOutline(graphics, x, y, btnWidth, btnHeight, radius,
                    McnTheme.DIVIDER_THICKNESS, PhoneColors.divider(style));
        
            if (home) {
                // Home button - house icon
                int centerX = x + btnWidth / 2;
                int centerY = y + btnHeight / 2;
                float glyphScale = (McnTheme.TEXT_BODY * 1.2f) / (float) Minecraft.getInstance().font.lineHeight;
                Font font = Minecraft.getInstance().font;
                graphics.pose().pushPose();
                graphics.pose().translate(centerX, centerY, McnTheme.ELEV_1);
                graphics.pose().scale(glyphScale, glyphScale, McnTheme.ELEV_1);
                graphics.drawString(font, "\u2302", -font.width("\u2302") / 2, -font.lineHeight / 2,
                        PhoneColors.textPrimary(style), false);
                graphics.pose().popPose();
            } else {
                // Back button - left arrow
                int centerX = x + btnWidth / 2;
                int centerY = y + btnHeight / 2;
                float glyphScale = (McnTheme.TEXT_BODY * 1.2f) / (float) Minecraft.getInstance().font.lineHeight;
                Font font = Minecraft.getInstance().font;
                graphics.pose().pushPose();
                graphics.pose().translate(centerX, centerY, McnTheme.ELEV_1);
                graphics.pose().scale(glyphScale, glyphScale, McnTheme.ELEV_1);
                graphics.drawString(font, "\u2190", -font.width("\u2190") / 2, -font.lineHeight / 2,
                        PhoneColors.textPrimary(style), false);
                graphics.pose().popPose();
            }
        }

    private static boolean contains(double x, double y, int buttonX, int buttonY) {
        return x >= buttonX && x < buttonX + BUTTON_WIDTH
                && y >= buttonY && y < buttonY + BUTTON_HEIGHT;
    }

    static void openHome(PhoneNavigableScreen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && screen.phoneMenu() != null) {
            minecraft.setScreen(new PhoneScreen(screen.phoneMenu(), minecraft.player.getInventory(),
                    Component.translatable("gui.mobilecellnetwork.phone")));
        }
    }
}
