package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.KnownPhoneDevicesPacket;
import com.example.mobilecellnetwork.network.LockPhoneFeaturePacket;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
import com.example.mobilecellnetwork.network.RemoveKnownPhonePacket;
import com.example.mobilecellnetwork.network.RequestKnownPhoneDevicesPacket;
import com.example.mobilecellnetwork.network.WipePhonePacket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ManageDevicesScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private int scroll;

    public ManageDevicesScreen(PhoneMenu menu) {
        super(Component.literal("Manage Devices"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() { return menu; }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestKnownPhoneDevicesPacket());
        rebuildActions();
    }

    private void rebuildActions() {
        clearWidgets();
        int x = PhoneFrame.getScreenX(this) + 11;
        int top = PhoneFrame.getScreenY(this) + 82;
        int visible = 3;
        var devices = PhoneDeviceClientCache.devices();
        int first = Math.min(scroll, Math.max(0, devices.size() - visible));
        for (int index = first; index < Math.min(devices.size(), first + visible); index++) {
            KnownPhoneDevicesPacket.Device device = devices.get(index);
            int rowY = top + (index - first) * 75;
            addRenderableWidget(button(x, rowY + 43, 88,
                    device.bankLocked() ? "Unlock bank" : "Lock bank",
                    () -> sendLock(device, "bank", !device.bankLocked())));
            addRenderableWidget(button(x + 92, rowY + 43, 88, "Lock all apps",
                    () -> sendLock(device, "all", true)));
            addRenderableWidget(button(x, rowY + 60, 88, "Unlock",
                    () -> sendLock(device, "unlock", false)));
            addRenderableWidget(button(x + 92, rowY + 60, 88, "Wipe", () ->
                    Minecraft.getInstance().setScreen(new ConfirmActionModal(this, "Wipe Phone",
                            "Wipe all phone data, keep the number, and lock it.",
                            ignored -> PacketDistributor.sendToServer(new WipePhonePacket(device.id()))))));
            addRenderableWidget(button(x + 160, rowY, 20, "x",
                    () -> PacketDistributor.sendToServer(new RemoveKnownPhonePacket(device.id()))));
        }
    }

    private Button button(int x, int y, int width, String label, Runnable action) {
        return Button.builder(Component.literal(label), ignored -> action.run())
                .bounds(x, y, width, 18).build();
    }

    private void sendLock(KnownPhoneDevicesPacket.Device device, String feature, boolean lock) {
        PacketDistributor.sendToServer(new LockPhoneFeaturePacket(device.id(), feature, lock));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneFrame.renderHeader(graphics, this, "Manage Devices", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, x + 5, y + 62, PhoneFrame.getScreenW() - 10, 244);
        int visible = 3;
        var devices = PhoneDeviceClientCache.devices();
        int first = Math.min(scroll, Math.max(0, devices.size() - visible));
        for (int index = first; index < Math.min(devices.size(), first + visible); index++) {
            KnownPhoneDevicesPacket.Device device = devices.get(index);
            int rowY = y + 82 + (index - first) * 75;
            PhoneRenderer.drawCard(graphics, style, x + 8, rowY, PhoneFrame.getScreenW() - 16, 72);
            String id = device.id().toString();
            graphics.drawString(font, id.substring(0, 18), x + 13, rowY + 3,
                    PhoneColors.textPrimary(style), true);
            graphics.drawString(font, id.substring(18), x + 13, rowY + 13,
                    PhoneColors.textPrimary(style), true);
            String seen = "Last used " + DateTimeFormatter.ofPattern("MMM d HH:mm")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(device.lastSeen()));
            graphics.drawString(font, font.plainSubstrByWidth(seen, 155),
                    x + 13, rowY + 23, PhoneColors.textSecondary(style), true);
            String locks = device.allLocked() ? "All locked"
                    : device.bankLocked() ? "Bank locked" : "Unlocked";
            graphics.drawString(font, locks + (device.current() ? " - this phone" : ""),
                    x + 13, rowY + 33,
                    device.allLocked() || device.bankLocked() ? PhoneColors.ERROR_TEXT : PhoneColors.SUCCESS_TEXT, true);
        }
        if (devices.isEmpty()) {
            graphics.drawCenteredString(font, "No known devices", width / 2, y + 135,
                    PhoneColors.textSecondary(style));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, PhoneDeviceClientCache.devices().size() - 3));
            rebuildActions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(new DeviceSettingsScreen(menu));
    }

    @Override
    public void navigateHome() {
        minecraft.setScreen(new PhoneScreen(menu, minecraft.player.getInventory(),
                Component.translatable("gui.mobilecellnetwork.phone")));
    }

    @Override
    public void onClose() { navigateBack(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
