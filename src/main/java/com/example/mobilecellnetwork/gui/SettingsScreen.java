package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.CallControlPacket;
import com.example.mobilecellnetwork.network.SetLowPowerPacket;
import com.example.mobilecellnetwork.network.SettingsUpdatePacket;
import com.example.mobilecellnetwork.network.SetPhoneStylePacket;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SettingsScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private int observedRevision = -1;
    private int scrollOffset;
    private PhoneStyle observedStyle;
    private String loadedProfileName = "";
    private PhonePhotoLibrary.Photo loadedProfile;
    private long nextProfileLookup;
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
        observedStyle = ClientPhoneCache.phoneStyle();
        int x = PhoneFrame.getScreenX(this) + 8;
        int y = PhoneFrame.getScreenY(this) + (observedStyle == PhoneStyle.IOS ? 129 : 80);
        java.util.List<SettingRow> rows = rows();
        int visible = 8;
        int first = Math.min(scrollOffset, Math.max(0, rows.size() - visible));
        if (observedStyle == PhoneStyle.IOS) {
            addRenderableWidget(new SettingsHitbox(x, PhoneFrame.getScreenY(this) + 61, 188, 48,
                    this::importProfilePhoto));
        }
        for (int i = first; i < Math.min(rows.size(), first + visible); i++) {
            SettingRow row = rows.get(i);
            int rowY = y + (i - first) * (observedStyle == PhoneStyle.IOS ? 26 : 27);
            addRenderableWidget(new SettingsHitbox(x, rowY, 188,
                    observedStyle == PhoneStyle.IOS ? 26 : 24, row.action()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (observedRevision != ClientPhoneCache.lowPowerRevision()
                || observedStyle != ClientPhoneCache.phoneStyle()) {
            clearWidgets();
            init();
        }
        PhoneFrame.render(graphics, this);
        int screenX = PhoneFrame.getScreenX(this);
        int screenY = PhoneFrame.getScreenY(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        if (style == PhoneStyle.IOS) {
            renderSnapple(graphics, screenX, screenY, layoutMouseX, layoutMouseY);
            super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        PhoneFrame.renderHeader(graphics, this, "Settings", true);
        PhoneRenderer.drawCard(graphics, style, screenX + 5, screenY + 62,
                PhoneFrame.getScreenW() - 10, 245);
        drawSettingsText(graphics, style, "DEVICE & APP PREFERENCES", screenX + 12, screenY + 67,
                PhoneColors.textSecondary(style));
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
            int valueX = screenX + PhoneFrame.getScreenW() - (row.toggle() ? 54 : 82);
            String title = style == PhoneStyle.SAMSUNG_ONE_UI
                    ? font.plainSubstrByWidth(row.title(), Math.max(0, valueX - (screenX + 14) - 5))
                    : row.title();
            drawSettingsText(graphics, style, title, screenX + 14, rowY + 8,
                    PhoneColors.textPrimary(style));
            if (row.toggle()) {
                PhoneRenderer.drawToggle(graphics, style, screenX + PhoneFrame.getScreenW() - 54,
                        rowY + 1, row.enabled());
            } else {
                String value = font.plainSubstrByWidth(row.value(), style == PhoneStyle.SAMSUNG_ONE_UI ? 70 : 74);
                drawSettingsText(graphics, style, value,
                        style == PhoneStyle.SAMSUNG_ONE_UI
                                ? screenX + PhoneFrame.getScreenW() - 15 - font.width(value)
                                : screenX + PhoneFrame.getScreenW() - 82,
                        rowY + 8, PhoneColors.accent(style));
            }
        }
        String scrollLabel = rows.size() > visible
                ? "Scroll for more  " + (first + 1) + "-" + Math.min(first + visible, rows.size())
                    + " of " + rows.size()
                : "All preferences";
        drawSettingsText(graphics, style, scrollLabel, screenX + 12, screenY + 297,
                PhoneColors.textSecondary(style));
        int infoY = screenY + 313;
        PhoneRenderer.drawCard(graphics, style, screenX + 8, infoY,
                PhoneFrame.getScreenW() - 16, 43);
        drawSettingsText(graphics, style, "Phone " + ClientPhoneCache.phoneNumber(),
                screenX + 13, infoY + 5, PhoneColors.textSecondary(style));
        drawSettingsText(graphics, style, "Plan " + ClientPhoneCache.planId()
                        + " | Roaming " + (ClientSignalCache.roaming() ? "On" : "Off"),
                screenX + 13, infoY + 20, PhoneColors.textSecondary(style));
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderSnapple(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int width = PhoneFrame.getScreenW();
        int height = PhoneFrame.getScreenH();
        McnPrimitives.fillRoundedRect(graphics, x, y, width, height, 14, PhoneColors.bgPrimary(PhoneStyle.IOS));
        McnPrimitives.drawPhoneCamera(graphics, PhoneStyle.IOS, x, y, width);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 13, y + 34, 0);
        graphics.pose().scale(1.6F, 1.6F, 1);
        SnappleText.draw(graphics, font, "Settings", 0, 0, PhoneColors.textPrimary(PhoneStyle.IOS));
        graphics.pose().popPose();

        McnPrimitives.fillRoundedRect(graphics, x + 8, y + 61, width - 16, 48, 14,
                PhoneColors.bgSecondary(PhoneStyle.IOS));
        int avatarWidth = PhoneFrame.circularElementWidth(31);
        int avatarX = x + 32 - avatarWidth / 2;
        McnPrimitives.fillRoundedRect(graphics, avatarX, y + 69, avatarWidth, 31, 16, 0xFFB5B5C4);
        String name = ClientPhoneCache.deviceName().isBlank()
                ? (minecraft.player == null ? "Player" : minecraft.player.getName().getString())
                : ClientPhoneCache.deviceName();
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        PhonePhotoLibrary.Photo profile = findProfilePhoto();
        if (profile == null || !PhonePhotoLibrary.drawProfilePhoto(graphics, profile.name(),
                profile.bytesView(), avatarX, y + 69, avatarWidth, 31)) {
            SnappleText.centered(graphics, font, initial, x + 32, y + 80, 0xFFFFFFFF);
        }
        SnappleText.draw(graphics, font, font.plainSubstrByWidth(name, 118), x + 56, y + 73,
                PhoneColors.textPrimary(PhoneStyle.IOS));
        SnappleText.draw(graphics, font, "Phone " + ClientPhoneCache.phoneNumber(), x + 56, y + 88,
                PhoneColors.textSecondary(PhoneStyle.IOS));

        java.util.List<SettingRow> rows = rows();
        int visible = 8;
        int first = Math.min(scrollOffset, Math.max(0, rows.size() - visible));
        McnPrimitives.fillRoundedRect(graphics, x + 8, y + 129, width - 16, 208, 13,
                PhoneColors.bgSecondary(PhoneStyle.IOS));
        for (int i = first; i < Math.min(rows.size(), first + visible); i++) {
            SettingRow row = rows.get(i);
            int rowY = y + 129 + (i - first) * 26;
            boolean hovered = mouseX >= x + 8 && mouseX < x + width - 8
                    && mouseY >= rowY && mouseY < rowY + 26;
            if (hovered) {
                McnPrimitives.fillRoundedRect(graphics, x + 10, rowY + 1, width - 20, 24, 6,
                        PhoneColors.bgTertiary(PhoneStyle.IOS));
            }
            int iconColor = switch (row.title()) {
                case "Appearance" -> 0xFF5368BF;
                case "Low Power Mode" -> 0xFF36B95B;
                case "Ringtone" -> 0xFFFF7656;
                case "Tone" -> 0xFFEA6681;
                case "Notification Sounds" -> 0xFFFF6B5E;
                case "Do Not Disturb" -> 0xFF7864CC;
                case "Profile photo", "Profile gallery" -> 0xFFB5B5C4;
                case "Cell Tower" -> 0xFF42BA68;
                default -> 0xFF898990;
            };
            String glyph = row.title().endsWith("Notifications") ? "N" : row.title().substring(0, 1);
            McnPrimitives.fillRoundedRect(graphics, x + 14, rowY + 5, 16, 16, 4, iconColor);
            SnappleText.centered(graphics, font, glyph, x + 22, rowY + 9, 0xFFFFFFFF);
            String value = row.toggle() ? "" : font.plainSubstrByWidth(row.value(), 54);
            int valueWidth = font.width(SnappleText.text(value));
            int titleWidth = row.toggle() ? 111 : Math.max(50, width - 58 - valueWidth - 35);
            String title = font.plainSubstrByWidth(row.title(), titleWidth);
            SnappleText.draw(graphics, font, title, x + 35, rowY + 9, PhoneColors.textPrimary(PhoneStyle.IOS));
            if (row.toggle()) {
                int toggleX = x + width - 42;
                McnPrimitives.fillRoundedRect(graphics, toggleX, rowY + 5, 29, 16, 8,
                        row.enabled() ? 0xFF34C759 : 0xFFC5C5C9);
                McnPrimitives.fillRoundedRect(graphics, toggleX + (row.enabled() ? 15 : 2),
                        rowY + 7, 12, 12, 6, 0xFFFFFFFF);
            } else {
                SnappleText.draw(graphics, font, value,
                        x + width - 24 - valueWidth, rowY + 9, PhoneColors.textSecondary(PhoneStyle.IOS));
                SnappleText.draw(graphics, font, ">", x + width - 17, rowY + 9,
                        PhoneColors.textSecondary(PhoneStyle.IOS));
            }
            if (i < Math.min(rows.size(), first + visible) - 1) {
                graphics.fill(x + 35, rowY + 25, x + width - 13, rowY + 26,
                        PhoneColors.divider(PhoneStyle.IOS));
            }
        }
        SnappleText.centered(graphics, font, "Scroll for more  " + (first + 1) + "-"
                + Math.min(first + visible, rows.size()) + " / " + rows.size(),
                x + width / 2, y + 346, PhoneColors.textSecondary(PhoneStyle.IOS));
    }

    private void drawSettingsText(GuiGraphics graphics, PhoneStyle style, String text, int x, int y, int color) {
        if (style == PhoneStyle.IOS) {
            SnappleText.draw(graphics, font, text, x, y, color);
        } else {
            graphics.drawString(font, text, x, y, color, false);
        }
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
                new SettingRow("Appearance", PhoneColors.dark(style) ? "Dark" : "Light", false, false,
                        () -> update("appearance", PhoneColors.dark(ClientPhoneCache.phoneStyle()) ? "light" : "dark")),
                new SettingRow("Profile photo", "Browse", false, false, this::importProfilePhoto),
                new SettingRow("Profile gallery", "Choose", false, false, this::chooseProfilePhoto),
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
                new SettingRow("Phone style", fixedPhoneStyle() ? "Fixed" : styleName(style),
                        false, false, this::cycleStyle),
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

    private void chooseProfilePhoto() {
        minecraft.setScreen(new GalleryScreen(menu, this, photo -> update("profilePhoto", photo.name())));
    }

    private void importProfilePhoto() {
        minecraft.setScreen(new PhotoImportScreen(this, menu,
                photo -> update("profilePhoto", photo.name()), true));
    }

    private PhonePhotoLibrary.Photo findProfilePhoto() {
        String name = ClientPhoneCache.profilePhoto();
        if (!name.equals(loadedProfileName)) {
            loadedProfileName = name;
            loadedProfile = null;
            nextProfileLookup = 0;
        }
        if (name.isBlank()) {
            return null;
        }
        if (loadedProfile != null || System.currentTimeMillis() < nextProfileLookup) {
            return loadedProfile;
        }
        nextProfileLookup = System.currentTimeMillis() + 2000;
        try {
            loadedProfile = PhonePhotoLibrary.photos().stream().filter(photo -> photo.name().equals(name))
                    .findFirst().orElse(null);
        } catch (java.io.IOException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Unable to load phone profile photo", exception);
        }
        return loadedProfile;
    }

    private SettingRow notificationRow(String label, String app) {
        boolean enabled = ClientPhoneCache.appNotifications().getOrDefault(app, true);
        return new SettingRow(label + " Notifications", "", true, enabled,
                () -> update("app:" + app,
                        Boolean.toString(!ClientPhoneCache.appNotifications().getOrDefault(app, true))));
    }

    private void cycleStyle() {
        if (fixedPhoneStyle()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("This phone's interface style is fixed."), true);
            }
            return;
        }
        PhoneStyle current = ClientPhoneCache.phoneStyle();
        PhoneStyle[] values = PhoneStyle.values();
        PhoneStyle next = values[(current.ordinal() + 1) % values.length];
        ClientPhoneCache.setPhoneStyle(next);
        PacketDistributor.sendToServer(new SetPhoneStylePacket(next));
    }

    private boolean fixedPhoneStyle() {
        if (minecraft.player == null) return false;
        java.util.UUID phoneId = com.example.mobilecellnetwork.network.PhoneDeviceClientCache.phoneId();
        if (phoneId == null) return false;
        return java.util.stream.Stream.concat(minecraft.player.getInventory().items.stream(),
                        minecraft.player.getInventory().offhand.stream())
                .filter(stack -> phoneId.equals(stack.get(
                        com.example.mobilecellnetwork.registry.ModDataComponents.PHONE_ID.get())))
                .anyMatch(stack -> stack.getItem() instanceof com.example.mobilecellnetwork.item.CellphoneItem phone
                        && phone.fixedStyle() != null);
    }

    private static String styleName(PhoneStyle style) {
        return switch (style) {
            case SAMSUNG_ONE_UI -> "Samsung One UI";
            case IOS -> "Snapple";
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
