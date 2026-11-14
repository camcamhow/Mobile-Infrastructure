package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.CallControlPacket;
import com.example.mobilecellnetwork.network.SetLowPowerPacket;
import com.example.mobilecellnetwork.network.SettingsUpdatePacket;
import com.example.mobilecellnetwork.network.SetPhoneStylePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SettingsScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private int observedRevision = -1;
    private int scrollOffset;
    private static final java.util.List<String> BASE_RINGTONES = java.util.List.of(
            "minecraft:block.note_block.bell", "minecraft:block.note_block.hat");
    private static final java.util.List<String> EXTRA_RINGTONES = java.util.List.of(
            "minecraft:block.note_block.bell", "minecraft:block.note_block.chime",
            "minecraft:block.note_block.hat", "minecraft:block.note_block.pling");

    public SettingsScreen(PhoneMenu menu) {
        super(Component.literal("Settings"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        observedRevision = ClientPhoneCache.lowPowerRevision();
        int x = PhoneFrame.getScreenX(this) + 8;
        int y = PhoneFrame.getScreenY(this) + 80;
        java.util.List<SettingRow> rows = rows();
        int visible = 8;
        int first = Math.min(scrollOffset, Math.max(0, rows.size() - visible));
        for (int i = first; i < Math.min(rows.size(), first + visible); i++) {
            SettingRow row = rows.get(i);
            int rowY = y + (i - first) * 27;
            addRenderableWidget(new SettingsHitbox(x, rowY, 188, 24, row.action()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (observedRevision != ClientPhoneCache.lowPowerRevision()) {
            clearWidgets();
            init();
        }
        PhoneFrame.render(graphics, this);
        int screenX = PhoneFrame.getScreenX(this);
        int screenY = PhoneFrame.getScreenY(this);
        PhoneFrame.renderHeader(graphics, this, "Settings", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        PhoneRenderer.drawCard(graphics, style, screenX + 5, screenY + 62,
                PhoneFrame.getScreenW() - 10, 245);
        graphics.drawString(font, "DEVICE & APP PREFERENCES", screenX + 12, screenY + 67,
                PhoneColors.textSecondary(style), false);
        java.util.List<SettingRow> rows = rows();
        int visible = 8;
        int first = Math.min(scrollOffset, Math.max(0, rows.size() - visible));
        for (int i = first; i < Math.min(rows.size(), first + visible); i++) {
            SettingRow row = rows.get(i);
            int rowY = screenY + 80 + (i - first) * 27;
            boolean hovered = layoutMouseX >= screenX + 8 && layoutMouseX < screenX + PhoneFrame.getScreenW() - 8
                    && layoutMouseY >= rowY && layoutMouseY < rowY + 24;
            PhoneRenderer.drawCard(graphics, style, screenX + 8, rowY, PhoneFrame.getScreenW() - 16, 24);
            if (hovered) {
                PhoneRenderer.fillRoundedRect(graphics, style, screenX + 9, rowY + 1,
                        PhoneFrame.getScreenW() - 18, 22, 9, PhoneColors.bgTertiary(style));
            }
            graphics.drawString(font, row.title(), screenX + 14, rowY + 8,
                    PhoneColors.textPrimary(style), false);
            if (row.toggle()) {
                PhoneRenderer.drawToggle(graphics, style, screenX + PhoneFrame.getScreenW() - 54,
                        rowY + 1, row.enabled());
            } else {
                String value = font.plainSubstrByWidth(row.value(), 74);
                graphics.drawString(font, value,
                        screenX + PhoneFrame.getScreenW() - 82, rowY + 8,
                        PhoneColors.accent(style), false);
            }
        }
        String scrollLabel = rows.size() > visible
                ? "Scroll for more  " + (first + 1) + "-" + Math.min(first + visible, rows.size())
                    + " of " + rows.size()
                : "All preferences";
        graphics.drawString(font, scrollLabel, screenX + 12, screenY + 297,
                PhoneColors.textSecondary(style), false);
        int infoY = screenY + 313;
        PhoneRenderer.drawCard(graphics, style, screenX + 8, infoY,
                PhoneFrame.getScreenW() - 16, 43);
        graphics.drawString(font, "Phone " + ClientPhoneCache.phoneNumber(),
                screenX + 13, infoY + 5, PhoneColors.textSecondary(style), false);
        graphics.drawString(font, "Plan " + ClientPhoneCache.planId()
                        + " | Roaming " + (ClientSignalCache.roaming() ? "On" : "Off"),
                screenX + 13, infoY + 20, PhoneColors.textSecondary(style), false);
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button,
                PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                    PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
        }
        int maximum = Math.max(0, rows().size() - 8);
        int next = Math.clamp(scrollOffset + (scrollY < 0 ? 1 : -1), 0, maximum);
        if (next != scrollOffset) {
            scrollOffset = next;
            clearWidgets();
            init();
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    private java.util.List<SettingRow> rows() {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        return java.util.List.of(
                new SettingRow("Low Power Mode", "", true, ClientPhoneCache.lowPowerMode(),
                        () -> PacketDistributor.sendToServer(new SetLowPowerPacket(!ClientPhoneCache.lowPowerMode()))),
                new SettingRow("Ringtone", "", true, ClientPhoneCache.ringtoneEnabled(),
                        () -> PacketDistributor.sendToServer(new CallControlPacket(
                                CallControlPacket.Action.TOGGLE_RINGTONE, !ClientPhoneCache.ringtoneEnabled()))),
                new SettingRow("Tone", toneName(ClientPhoneCache.ringtoneSound()), false, false,
                        this::cycleRingtone),
                new SettingRow("Notification Sounds", "", true, ClientPhoneCache.notificationSoundsEnabled(),
                        () -> update("sounds", Boolean.toString(!ClientPhoneCache.notificationSoundsEnabled()))),
                new SettingRow("Do Not Disturb", "", true, ClientPhoneCache.doNotDisturb(),
                        () -> update("dnd", Boolean.toString(!ClientPhoneCache.doNotDisturb()))),
                new SettingRow("Phone style", styleName(style), false, false, this::cycleStyle),
                new SettingRow("Cell Tower", ClientSignalCache.roaming() ? "Roaming" : "Auto", false, false,
                        () -> minecraft.setScreen(new TowerSelectScreen(menu))),
                new SettingRow("Device", "Lock & PIN", false, false,
                        () -> minecraft.setScreen(new DeviceSettingsScreen(menu))),
                notificationRow("SMS", "sms"),
                notificationRow("Calls", "call"),
                notificationRow("Socials", "socials"),
                notificationRow("Plan", "plan"),
                notificationRow("Bank", "bank"));
    }

    private SettingRow notificationRow(String label, String app) {
        boolean enabled = ClientPhoneCache.appNotifications().getOrDefault(app, true);
        return new SettingRow(label + " Notifications", "", true, enabled,
                () -> update("app:" + app, Boolean.toString(!enabled)));
    }

    private void cycleStyle() {
        PhoneStyle current = ClientPhoneCache.phoneStyle();
        PhoneStyle[] values = PhoneStyle.values();
        PhoneStyle next = values[(current.ordinal() + 1) % values.length];
        ClientPhoneCache.setPhoneStyle(next);
        PacketDistributor.sendToServer(new SetPhoneStylePacket(next));
    }

    private static String styleName(PhoneStyle style) {
        return switch (style) {
            case SAMSUNG_ONE_UI -> "Samsung One UI";
            case IOS -> "iOS";
            case DEFAULT -> "Default";
        };
    }

    private void cycleRingtone() {
        java.util.List<String> ringtones = ClientPhoneCache.installedApps().contains("ringtones")
                ? EXTRA_RINGTONES : BASE_RINGTONES;
        int current = ringtones.indexOf(ClientPhoneCache.ringtoneSound());
        update("ringtone", ringtones.get((current + 1) % ringtones.size()));
    }

    private void update(String key, String value) {
        PacketDistributor.sendToServer(new SettingsUpdatePacket(key, value));
    }

    private static String toneName(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1).replace("block.note_block.", "") : id;
    }

    private void returnToPhone() {
        minecraft.setScreen(new PhoneScreen(menu, minecraft.player.getInventory(),
                Component.translatable("gui.mobilecellnetwork.phone")));
    }

    @Override
    public void onClose() {
        returnToPhone();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SettingRow(String title, String value, boolean toggle, boolean enabled, Runnable action) {
    }

    private static final class SettingsHitbox extends Button {
        private SettingsHitbox(int x, int y, int width, int height, Runnable action) {
            super(x, y, width, height, Component.empty(), button -> action.run(), DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }
    }
}
