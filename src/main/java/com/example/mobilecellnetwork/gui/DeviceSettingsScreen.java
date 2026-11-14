package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
import com.example.mobilecellnetwork.network.SetPhoneLockScreenPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DeviceSettingsScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;

    public DeviceSettingsScreen(PhoneMenu menu) {
        super(Component.literal("Device"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() { return menu; }

    @Override
    protected void init() {
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        int buttonX = left + 20;
        int buttonWidth = PhoneFrame.getScreenW() - 40;
        addRenderableWidget(Button.builder(Component.literal("Lock screen: "
                        + (PhoneDeviceClientCache.lockScreenEnabled() ? "On" : "Off")),
                button -> PacketDistributor.sendToServer(new SetPhoneLockScreenPacket(
                        !PhoneDeviceClientCache.lockScreenEnabled())))
                .bounds(buttonX, top + 100, buttonWidth, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Change PIN"),
                button -> minecraft.setScreen(new PhonePinEntryScreen(this, false)))
                .bounds(buttonX, top + 130, buttonWidth, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Remove PIN"),
                button -> minecraft.setScreen(new PhonePinEntryScreen(this, true)))
                .bounds(buttonX, top + 160, buttonWidth, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Manage devices"),
                button -> minecraft.setScreen(new ManageDevicesScreen(menu)))
                .bounds(buttonX, top + 190, buttonWidth, 22).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        PhoneFrame.renderHeader(graphics, this, "Device", true);
        PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(), left + 8, top + 62,
                PhoneFrame.getScreenW() - 16, 202);
        graphics.drawCenteredString(font, "Device security", width / 2, top + 73,
                PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
        String phoneId = abbreviate(PhoneDeviceClientCache.phoneId());
        if (phoneId.length() > 18) {
            graphics.drawCenteredString(font, "Phone ID: " + phoneId.substring(0, 18),
                    width / 2, top + 275, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
            graphics.drawCenteredString(font, phoneId.substring(18),
                    width / 2, top + 287, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
        } else {
            graphics.drawCenteredString(font, "Phone ID: " + phoneId,
                    width / 2, top + 275, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private static String abbreviate(java.util.UUID id) {
        return id == null ? "Unavailable" : id.toString();
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(new SettingsScreen(menu));
    }

    @Override
    public void navigateHome() {
        minecraft.setScreen(new PhoneScreen(menu, minecraft.player.getInventory(),
                Component.translatable("gui.mobilecellnetwork.phone")));
    }

    @Override
    public void onClose() {
        navigateBack();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
