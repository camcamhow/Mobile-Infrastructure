package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ContactsActionPacket;
import com.example.mobilecellnetwork.network.StartChatPacket;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class ContactsScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE_ROWS = 5;
    private static final int IOS_ROWS = 4;
    private static final int IOS_LIST_Y = 119;
    private static final int IOS_ROW_HEIGHT = 46;
    private static final String INDEX = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
    private final PhoneMenu menu;
    private int scroll;
    private String selectedName;
    private EditBox search;
    private boolean searching;
    private String searchText = "";
    private String profileName = "";
    private PhonePhotoLibrary.Photo profilePhoto;
    private long nextProfileLookup;

    public ContactsScreen(PhoneMenu menu) {
        super(Component.literal("Contacts"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            int x = PhoneFrame.getScreenX(this);
            int y = PhoneFrame.getScreenY(this);
            search = PhoneTextField.create(font, x + 12, y + 93, 176, 21,
                    "Search contacts", 64, PhoneStyle.IOS);
            search.setValue(searchText);
            search.setResponder(value -> {
                searchText = value;
                scroll = 0;
            });
            search.visible = searching;
            addRenderableWidget(search);
        }
    }

    @Override
    public void navigateBack() {
        if (selectedName != null) {
            selectedName = null;
        } else if (searching) {
            setSearching(false);
        } else {
            PhoneNavigation.openHome(this);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            PhoneFrame.render(graphics, this);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        if (style == PhoneStyle.IOS) {
            renderSnapple(graphics, x, y, mouseX, mouseY, partialTick);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        PhoneFrame.renderHeader(graphics, this, "Contacts", true);
        List<Map.Entry<String, String>> contacts = contacts();
        Set<String> online = ClientPhoneCache.onlinePlayers().stream()
                .map(ClientPhoneCache.OnlinePlayer::number).collect(Collectors.toSet());
        int first = Math.clamp(scroll, 0, Math.max(0, contacts.size() - VISIBLE_ROWS));
        for (int index = first; index < Math.min(first + VISIBLE_ROWS, contacts.size()); index++) {
            Map.Entry<String, String> contact = contacts.get(index);
            int rowY = y + 68 + (index - first) * 46;
            int statusColor = online.contains(contact.getValue())
                    ? PhoneColors.success(style) : PhoneColors.textSecondary(style);
            if (style == PhoneStyle.SAMSUNG_ONE_UI) {
                int avatarWidth = PhoneFrame.circularElementWidth(30);
                McnPrimitives.fillRoundedRect(graphics, x + 29 - avatarWidth / 2, rowY + 5,
                        avatarWidth, 30, 15, PhoneColors.bgTertiary(style));
                String initial = contact.getKey().isBlank() ? "?"
                        : contact.getKey().substring(0, 1).toUpperCase(Locale.ROOT);
                graphics.drawString(font, initial, x + 29 - font.width(initial) / 2,
                        rowY + 15, PhoneColors.accent(style), false);
                graphics.drawString(font, fit(contact.getKey(), 120), x + 51, rowY + 7,
                        PhoneColors.textPrimary(style), false);
                graphics.drawString(font, fit(contact.getValue(), 120), x + 51, rowY + 23,
                        PhoneColors.textSecondary(style), false);
                graphics.fill(x + 51, rowY + 43, x + 190, rowY + 44, PhoneColors.divider(style));
            } else {
                PhoneRenderer.drawCard(graphics, style, x + 10, rowY, 184, 40);
                graphics.drawString(font, fit(contact.getKey(), 145), x + 17, rowY + 7,
                        PhoneColors.textPrimary(style), true);
                graphics.drawString(font, contact.getValue(), x + 17, rowY + 23,
                        PhoneColors.textSecondary(style), true);
            }
            drawCircle(graphics, x + 183, rowY + 20, 4, statusColor);
        }
        if (contacts.isEmpty()) {
            graphics.drawCenteredString(font, "No contacts yet",
                    x + PhoneFrame.getScreenW() / 2, y + 132, PhoneColors.textSecondary(style));
        }
        PhoneRenderer.drawPrimaryButton(graphics, style, x + 48, y + 322, 108, 24,
                "New Contact", font);
        if (selectedName != null) {
            renderActions(graphics, x, y, style);
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderSnapple(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        PhoneStyle style = PhoneStyle.IOS;
        McnPrimitives.fillRoundedRect(graphics, x, y, PhoneFrame.getScreenW(), PhoneFrame.getScreenH(),
                14, PhoneColors.bgPrimary(style));
        McnPrimitives.drawPhoneCamera(graphics, style, x, y, PhoneFrame.getScreenW());
        SnappleText.centered(graphics, font, "Contacts", x + 102, y + 38, PhoneColors.textPrimary(style));
        SnappleAppLayout.pill(graphics, font, x + 169, y + 31, 26, 22, "+",
                PhoneColors.bgSecondary(style), PhoneColors.textPrimary(style));

        int avatarWidth = PhoneFrame.circularElementWidth(27);
        int avatarX = x + 29 - avatarWidth / 2;
        McnPrimitives.fillRoundedRect(graphics, avatarX, y + 62, avatarWidth, 27, 14, 0xFF554B73);
        PhonePhotoLibrary.Photo photo = profilePhoto();
        if (photo == null || !PhonePhotoLibrary.drawProfilePhoto(graphics, photo.name(),
                photo.bytesView(), avatarX, y + 62, avatarWidth, 27)) {
            SnappleText.centered(graphics, font, SnappleAppLayout.initials(profileLabel()), x + 29, y + 71, 0xFFFFFFFF);
        }
        SnappleText.draw(graphics, font, "My Card", x + 53, y + 64, PhoneColors.textPrimary(style));
        SnappleText.draw(graphics, font, fit(profileLabel(), 110), x + 53, y + 78,
                PhoneColors.textSecondary(style));
        if (searching) {
            PhoneTextField.renderBackground(graphics, style, x + 12, y + 93, 176, 21);
        }

        List<Map.Entry<String, String>> entries = filteredContacts();
        int first = Math.clamp(scroll, 0, Math.max(0, entries.size() - IOS_ROWS));
        for (int i = first; i < Math.min(entries.size(), first + IOS_ROWS); i++) {
            Map.Entry<String, String> contact = entries.get(i);
            int rowY = y + IOS_LIST_Y + (i - first) * IOS_ROW_HEIGHT;
            char group = group(contact.getKey());
            if (i == first || group(entries.get(i - 1).getKey()) != group) {
                SnappleText.draw(graphics, font, Character.toString(group), x + 12, rowY,
                        PhoneColors.textSecondary(style));
            }
            SnappleAppLayout.avatar(graphics, font, x + 29, rowY + 13, 25, contact.getKey());
            SnappleText.draw(graphics, font, fit(contact.getKey(), 120), x + 53, rowY + 21,
                    PhoneColors.textPrimary(style));
            SnappleAppLayout.separator(graphics, x + 53, rowY + 43, 125);
        }
        if (entries.isEmpty()) {
            SnappleText.centered(graphics, font, searchText.isBlank() ? "No contacts yet" : "No matches",
                    x + 96, y + 180, PhoneColors.textSecondary(style));
        }
        for (int i = 0; i < INDEX.length(); i++) {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 190, y + 120 + i * 6, 0);
            graphics.pose().scale(0.65F, 0.65F, 1);
            SnappleText.centered(graphics, font, INDEX.substring(i, i + 1), 0, 0, PhoneColors.accent(style));
            graphics.pose().popPose();
        }
        McnPrimitives.fillRoundedRect(graphics, x + 10, y + 315, 154, 30, 15, PhoneColors.bgSecondary(style));
        McnPrimitives.fillRoundedRect(graphics, x + 59, y + 317, 55, 26, 13, PhoneColors.bgTertiary(style));
        SnappleText.centered(graphics, font, "Calls", x + 34, y + 326, PhoneColors.textSecondary(style));
        SnappleText.centered(graphics, font, "Contacts", x + 86, y + 326, PhoneColors.accent(style));
        SnappleText.centered(graphics, font, "Keypad", x + 139, y + 326, PhoneColors.textSecondary(style));
        McnPrimitives.fillRoundedRect(graphics, x + 169, y + 315, 27, 30, 14, PhoneColors.bgSecondary(style));
        int searchColor = searching ? PhoneColors.accent(style) : PhoneColors.textPrimary(style);
        SnappleAppLayout.searchIcon(graphics, x + 177, y + 321, searchColor);
        if (selectedName == null) {
            super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                    PhoneFrame.displayMouseY(this, mouseY), partialTick);
        } else {
            renderActions(graphics, x, y, style);
        }
    }

    private String profileLabel() {
        return ClientPhoneCache.deviceName().isBlank()
                ? (minecraft.player == null ? "Player" : minecraft.player.getName().getString())
                : ClientPhoneCache.deviceName();
    }

    private PhonePhotoLibrary.Photo profilePhoto() {
        String name = ClientPhoneCache.profilePhoto();
        if (!name.equals(profileName)) {
            profileName = name;
            profilePhoto = null;
            nextProfileLookup = 0;
        }
        if (name.isBlank() || profilePhoto != null || System.currentTimeMillis() < nextProfileLookup) {
            return profilePhoto;
        }
        nextProfileLookup = System.currentTimeMillis() + 2000;
        try {
            profilePhoto = PhonePhotoLibrary.photos().stream().filter(photo -> photo.name().equals(name))
                    .findFirst().orElse(null);
        } catch (IOException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Unable to load phone profile photo", exception);
        }
        return profilePhoto;
    }

    private static char group(String name) {
        String upper = name.strip().toUpperCase(Locale.ROOT);
        char first = upper.isEmpty() ? '#' : upper.charAt(0);
        return first >= 'A' && first <= 'Z' ? first : '#';
    }

    private List<Map.Entry<String, String>> filteredContacts() {
        String query = searchText.strip().toLowerCase(Locale.ROOT);
        return contacts().stream()
                .filter(contact -> contact.getKey().toLowerCase(Locale.ROOT).contains(query)
                        || contact.getValue().contains(query))
                .sorted(Comparator.comparingInt((Map.Entry<String, String> entry) ->
                                group(entry.getKey()) == '#' ? 1 : 0)
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void setSearching(boolean enabled) {
        searching = enabled;
        if (search != null) {
            search.visible = enabled;
            search.setFocused(enabled);
            setFocused(enabled ? search : null);
            if (!enabled) {
                search.setValue("");
            }
        }
        scroll = 0;
    }

    private void renderActions(GuiGraphics graphics, int x, int y, PhoneStyle style) {
        if (style == PhoneStyle.IOS) {
            McnPrimitives.fillRoundedRect(graphics, x, y, PhoneFrame.getScreenW(),
                    PhoneFrame.getScreenH(), 14, 0x99000000);
            SnappleText.centered(graphics, font, SnappleAppLayout.fit(font, selectedName, 160),
                    x + 102, y + 126, 0xFFFFFFFF);
            SnappleAppLayout.group(graphics, x + 30, y + 146, 144, 146);
            String[] actions = {"Call", "Message", "Edit", "Delete"};
            for (int index = 0; index < actions.length; index++) {
                int rowY = y + 154 + index * 32;
                SnappleText.centered(graphics, font, actions[index], x + 102, rowY + 9,
                        index == 3 ? PhoneColors.error(style) : PhoneColors.accent(style));
                if (index < actions.length - 1) {
                    SnappleAppLayout.separator(graphics, x + 42, rowY + 29, 120);
                }
            }
            return;
        }
        PhoneRenderer.drawCard(graphics, style, x + 30, y + 146, 144, 146);
        String[] actions = {"Call", "Message", "Edit", "Delete"};
        for (int index = 0; index < actions.length; index++) {
            int rowY = y + 154 + index * 32;
            if (index == 0) {
                PhoneRenderer.drawPrimaryButton(graphics, style, x + 38, rowY, 128, 26, actions[index], font);
            } else {
                PhoneRenderer.drawSecondaryButton(graphics, style, x + 38, rowY, 128, 26, actions[index], font);
            }
        }
    }

    private List<Map.Entry<String, String>> contacts() {
        return ClientPhoneCache.contacts().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String fit(String text, int maxWidth) {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) return SnappleAppLayout.fit(font, text, maxWidth);
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private static void drawCircle(GuiGraphics graphics, int x, int y, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy <= radiusSquared) {
                    graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = PhoneFrame.layoutMouseX(this, mouseX);
        double ly = PhoneFrame.layoutMouseY(this, mouseY);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        if (selectedName != null) {
            if (button == 0 && lx >= x + 38 && lx < x + 166 && ly >= y + 154 && ly < y + 282
                    && (int) (ly - y - 154) % 32 < 26) {
                int action = (int) ((ly - (y + 154)) / 32);
                String number = ClientPhoneCache.contacts().get(selectedName);
                if (number == null) {
                    selectedName = null;
                    return true;
                }
                if (action < 2 && ClientPhoneCache.planId().isBlank()) {
                    if (minecraft.player != null) {
                        minecraft.player.displayClientMessage(Component.literal("Choose a plan to call or message."), true);
                    }
                    return true;
                }
                switch (action) {
                    case 0 -> {
                        PacketDistributor.sendToServer(new CallRequestPacket(number));
                        selectedName = null;
                    }
                    case 1 -> {
                        PacketDistributor.sendToServer(new StartChatPacket(number));
                        minecraft.setScreen(new SmsScreen(menu, number));
                    }
                    case 2 -> minecraft.setScreen(new ContactEditModalScreen(menu, selectedName, number));
                    case 3 -> {
                        PacketDistributor.sendToServer(new ContactsActionPacket(
                                "delete", selectedName, selectedName, ""));
                        selectedName = null;
                    }
                    default -> selectedName = null;
                }
                return true;
            }
            selectedName = null;
            return true;
        }
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            if (button != 0) {
                return super.mouseClicked(lx, ly, button);
            }
            double rx = lx - x;
            double ry = ly - y;
            if (inside(rx, ry, 169, 31, 26, 22)) {
                minecraft.setScreen(new ContactEditModalScreen(menu));
                return true;
            }
            if (inside(rx, ry, 10, 60, 180, 31)) {
                minecraft.setScreen(new SettingsScreen(menu));
                return true;
            }
            if (inside(rx, ry, 10, 315, 49, 30)) {
                minecraft.setScreen(new DialerScreen(menu, PhoneTab.RECENTS));
                return true;
            }
            if (inside(rx, ry, 59, 315, 55, 30)) {
                return true;
            }
            if (inside(rx, ry, 114, 315, 50, 30)) {
                minecraft.setScreen(new DialerScreen(menu, PhoneTab.KEYPAD));
                return true;
            }
            if (inside(rx, ry, 169, 315, 27, 30)) {
                setSearching(!searching);
                return true;
            }
            List<Map.Entry<String, String>> entries = filteredContacts();
            if (inside(rx, ry, 182, 120, 17, INDEX.length() * 6)) {
                char selected = INDEX.charAt(Math.clamp((int) ((ry - 120) / 6), 0, INDEX.length() - 1));
                for (int i = 0; i < entries.size(); i++) {
                    if (group(entries.get(i).getKey()) == selected) {
                        scroll = Math.min(i, Math.max(0, entries.size() - IOS_ROWS));
                        break;
                    }
                }
                return true;
            }
            if (inside(rx, ry, 9, IOS_LIST_Y, 171, IOS_ROWS * IOS_ROW_HEIGHT)) {
                int first = Math.clamp(scroll, 0, Math.max(0, entries.size() - IOS_ROWS));
                int index = first + (int) ((ry - IOS_LIST_Y) / IOS_ROW_HEIGHT);
                if (index < entries.size()) {
                    selectedName = entries.get(index).getKey();
                    if (search != null) {
                        search.setFocused(false);
                        setFocused(null);
                    }
                }
                return true;
            }
            return super.mouseClicked(lx, ly, button);
        }
        if (button == 0 && ly >= y + 322 && ly < y + 346 && lx >= x + 48 && lx < x + 156) {
            minecraft.setScreen(new ContactEditModalScreen(menu));
            return true;
        }
        if (button == 0 && lx >= x + 10 && lx < x + 194 && ly >= y + 68 && ly < y + 298
                && (int) (ly - y - 68) % 46 < 40) {
            List<Map.Entry<String, String>> contacts = contacts();
            int row = (int) ((ly - (y + 68)) / 46)
                    + Math.clamp(scroll, 0, Math.max(0, contacts.size() - VISIBLE_ROWS));
            if (row >= 0 && row < contacts.size()) {
                selectedName = contacts.get(row).getKey();
                return true;
            }
        }
        return super.mouseClicked(lx, ly, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            double x = PhoneFrame.layoutMouseX(this, mouseX) - PhoneFrame.getScreenX(this);
            double y = PhoneFrame.layoutMouseY(this, mouseY) - PhoneFrame.getScreenY(this);
            if (selectedName == null && scrollY != 0
                    && inside(x, y, 9, IOS_LIST_Y, 190, IOS_ROWS * IOS_ROW_HEIGHT)) {
                scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                        0, Math.max(0, filteredContacts().size() - IOS_ROWS));
                return true;
            }
            return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                    PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
        }
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, contacts().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
