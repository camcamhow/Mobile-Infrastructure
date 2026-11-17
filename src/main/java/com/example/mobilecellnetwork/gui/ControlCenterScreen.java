package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.network.CallControlPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.FlashlightPacket;
import com.example.mobilecellnetwork.network.SetLowPowerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ControlCenterScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;

    public ControlCenterScreen(PhoneMenu menu) {
        super(Component.literal("Control Center"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            renderQuickPanel(graphics, x, y);
            PhoneFrame.renderStatusBar(graphics, this, true);
            return;
        }
        McnPrimitives.drawPhoneWallpaper(graphics, PhoneStyle.IOS, x, y,
                PhoneFrame.getScreenW(), PhoneFrame.getScreenH());
        McnPrimitives.fillRoundedRect(graphics, x, y, PhoneFrame.getScreenW(),
                PhoneFrame.getScreenH(), 14, 0xB2080B1C);
        McnPrimitives.drawPhoneCamera(graphics, PhoneStyle.IOS, x, y, PhoneFrame.getScreenW());
        SnappleText.draw(graphics, font, "Control Center", x + 13, y + 41, 0xFFFFFFFF);
        drawPanel(graphics, x + 10, y + 61, 88, 106);
        drawPanel(graphics, x + 106, y + 61, 88, 106);
        drawCircle(graphics, x + 54, y + 90, "battery", ClientPhoneCache.lowPowerMode());
        SnappleText.centered(graphics, font, "Low Power", x + 54, y + 117, 0xFFFFFFFF);
        SnappleText.centered(graphics, font, ClientPhoneCache.lowPowerMode() ? "On" : "Off",
                x + 54, y + 133, 0xFFD3D6E4);
        drawCircle(graphics, x + 150, y + 90, "ringtone", ClientPhoneCache.ringtoneEnabled());
        SnappleText.centered(graphics, font, "Ringtone", x + 150, y + 117, 0xFFFFFFFF);
        SnappleText.centered(graphics, font, ClientPhoneCache.ringtoneEnabled() ? "On" : "Off",
                x + 150, y + 133, 0xFFD3D6E4);

        drawPanel(graphics, x + 10, y + 177, 184, 55);
        SnappleText.draw(graphics, font, "Phone status", x + 20, y + 186, 0xFFFFFFFF);
        String network = ClientSignalCache.networkName();
        if (network.isBlank()) {
            network = "No Service";
        }
        SnappleText.draw(graphics, font, font.plainSubstrByWidth(network, 105),
                x + 20, y + 206, 0xFFD8D9E5);
        SnappleText.draw(graphics, font, PhoneFrame.phoneChargePercent() + "%",
                x + 156, y + 206, 0xFFFFFFFF);

        drawCircle(graphics, x + 54, y + 268, "flashlight", ClientPhoneCache.flashlightOn());
        drawCircle(graphics, x + 150, y + 268, "settings", false);
        SnappleText.centered(graphics, font, "Flashlight", x + 54, y + 296, 0xFFFFFFFF);
        SnappleText.centered(graphics, font, "Settings", x + 150, y + 296, 0xFFFFFFFF);
        PhoneFrame.renderStatusBar(graphics, this, true);
    }

    private void renderQuickPanel(GuiGraphics graphics, int x, int y) {
        PhoneStyle style = PhoneStyle.SAMSUNG_ONE_UI;
        McnPrimitives.drawPhoneWallpaper(graphics, style, x, y, PhoneFrame.getScreenW(), PhoneFrame.getScreenH());
        McnPrimitives.fillRoundedRect(graphics, x, y, PhoneFrame.getScreenW(), PhoneFrame.getScreenH(),
                14, 0xD5161C27);
        McnPrimitives.drawPhoneCamera(graphics, style, x, y, PhoneFrame.getScreenW());
        graphics.drawString(font, "Quick settings", x + 12, y + 40, 0xFFFFFFFF, false);
        drawAndroidPanel(graphics, x + 10, y + 61, 88, 106);
        drawAndroidPanel(graphics, x + 106, y + 61, 88, 106);
        drawCircle(graphics, x + 54, y + 90, "battery", ClientPhoneCache.lowPowerMode());
        drawCircle(graphics, x + 150, y + 90, "ringtone", ClientPhoneCache.ringtoneEnabled());
        graphics.drawCenteredString(font, "Power saving", x + 54, y + 118, 0xFFFFFFFF);
        graphics.drawCenteredString(font, ClientPhoneCache.lowPowerMode() ? "On" : "Off",
                x + 54, y + 134, 0xFFCCD5E3);
        graphics.drawCenteredString(font, "Sound", x + 150, y + 118, 0xFFFFFFFF);
        graphics.drawCenteredString(font, ClientPhoneCache.ringtoneEnabled() ? "On" : "Off",
                x + 150, y + 134, 0xFFCCD5E3);
        drawAndroidPanel(graphics, x + 10, y + 177, 184, 55);
        graphics.drawString(font, "Mobile network", x + 20, y + 186, 0xFFFFFFFF, false);
        String network = ClientSignalCache.networkName().isBlank() ? "No Service" : ClientSignalCache.networkName();
        graphics.drawString(font, font.plainSubstrByWidth(network, 105), x + 20, y + 206, 0xFFCCD5E3, false);
        graphics.drawString(font, PhoneFrame.phoneChargePercent() + "%", x + 156, y + 206, 0xFFFFFFFF, false);
        drawCircle(graphics, x + 54, y + 268, "flashlight", ClientPhoneCache.flashlightOn());
        drawCircle(graphics, x + 150, y + 268, "settings", false);
        graphics.drawCenteredString(font, "Flashlight", x + 54, y + 296, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Settings", x + 150, y + 296, 0xFFFFFFFF);
    }

    private void drawAndroidPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, 16, 0xE6333B49);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, 17, 0xAA4C546C);
        McnPrimitives.fillRoundedRectOutline(graphics, x, y, width, height, 17, 1, 0x887D91B6);
    }

    private void drawCircle(GuiGraphics graphics, int cx, int cy, String icon, boolean on) {
        McnPrimitives.fillRoundedRect(graphics, cx - 21, cy - 21, 42, 42, 21,
                on ? 0xFF418BE8 : 0xCC59637E);
        McnPrimitives.fillRoundedRectOutline(graphics, cx - 21, cy - 21, 42, 42, 21, 1, 0xAA91A3C3);
        int white = 0xFFFFFFFF;
        switch (icon) {
            case "battery" -> {
                McnPrimitives.fillRoundedRectOutline(graphics, cx - 11, cy - 7, 20, 14, 3, 2, white);
                graphics.fill(cx + 9, cy - 3, cx + 12, cy + 3, white);
                McnPrimitives.fillRoundedRect(graphics, cx - 8, cy - 4, 12, 8, 2, white);
            }
            case "ringtone" -> {
                McnPrimitives.fillRoundedRect(graphics, cx - 8, cy - 9, 16, 16, 8, white);
                McnPrimitives.fillRoundedRect(graphics, cx - 10, cy + 3, 20, 4, 2, white);
                McnPrimitives.fillRoundedRect(graphics, cx - 3, cy + 8, 6, 3, 1, white);
            }
            case "flashlight" -> {
                McnPrimitives.fillRoundedRect(graphics, cx - 7, cy - 10, 14, 5, 2, white);
                graphics.fill(cx - 5, cy - 5, cx + 5, cy - 1, white);
                McnPrimitives.fillRoundedRect(graphics, cx - 4, cy - 1, 8, 13, 2, white);
            }
            case "settings" -> {
                McnPrimitives.fillRoundedRectOutline(graphics, cx - 8, cy - 8, 16, 16, 8, 3, white);
                graphics.fill(cx - 2, cy - 12, cx + 2, cy - 7, white);
                graphics.fill(cx - 2, cy + 7, cx + 2, cy + 12, white);
                graphics.fill(cx - 12, cy - 2, cx - 7, cy + 2, white);
                graphics.fill(cx + 7, cy - 2, cx + 12, cy + 2, white);
            }
            default -> throw new IllegalArgumentException("Unknown control center icon: " + icon);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX) - PhoneFrame.getScreenX(this);
        double y = PhoneFrame.layoutMouseY(this, mouseY) - PhoneFrame.getScreenY(this);
        if (button == 0 && y >= 61 && y < 167) {
            if (x >= 10 && x < 98) {
                PacketDistributor.sendToServer(new SetLowPowerPacket(!ClientPhoneCache.lowPowerMode()));
                return true;
            }
            if (x >= 106 && x < 194) {
                PacketDistributor.sendToServer(new CallControlPacket(
                        CallControlPacket.Action.TOGGLE_RINGTONE, !ClientPhoneCache.ringtoneEnabled()));
                return true;
            }
        }
        if (button == 0 && y >= 247 && y < 305) {
            if (x >= 33 && x < 75) {
                PacketDistributor.sendToServer(new FlashlightPacket(!ClientPhoneCache.flashlightOn()));
                return true;
            }
            if (x >= 129 && x < 171) {
                Minecraft.getInstance().setScreen(new SettingsScreen(menu));
                return true;
            }
        }
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
