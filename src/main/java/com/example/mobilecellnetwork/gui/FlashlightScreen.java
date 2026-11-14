package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.FlashlightPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FlashlightScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;

    public FlashlightScreen(PhoneMenu menu) {
        super(Component.literal("Flashlight"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Flashlight", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        boolean on = ClientPhoneCache.flashlightOn();
        int cx = left + 102;
        int cy = top + 150;
        int glow = on ? 0xFFFFD84A : PhoneColors.bgTertiary(style);
        if (on) {
            PhoneRenderer.fillRoundedRect(graphics, style, cx - 56, cy - 56, 112, 112, 56, 0x40FFD84A);
        }
        PhoneRenderer.fillRoundedRect(graphics, style, cx - 40, cy - 40, 80, 80, 40, glow);
        graphics.drawCenteredString(font, on ? "ON" : "OFF", cx, cy - 4,
                on ? 0xFF3A2A00 : PhoneColors.textSecondary(style));
        graphics.drawCenteredString(font, on ? "Tap to turn off" : "Tap to turn on", cx, cy + 70,
                PhoneColors.textSecondary(style));
        graphics.drawCenteredString(font, "Uses extra battery while on", cx, cy + 86,
                PhoneColors.textSecondary(style));
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int cx = PhoneFrame.getScreenX(this) + 102;
        int cy = PhoneFrame.getContentTop(this) + 150;
        if (button == 0 && Math.hypot(x - cx, y - cy) <= 44) {
            PacketDistributor.sendToServer(new FlashlightPacket(!ClientPhoneCache.flashlightOn()));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }
}
