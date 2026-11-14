package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ContactsActionPacket;
import com.example.mobilecellnetwork.network.StartChatPacket;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class ContactsScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE_ROWS = 5;
    private final PhoneMenu menu;
    private int scroll;
    private String selectedName;

    public ContactsScreen(PhoneMenu menu) {
        super(Component.literal("Contacts"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        if (selectedName != null) {
            selectedName = null;
        } else {
            PhoneNavigation.openHome(this);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No service gating: show overlay if no active plan and screen is not exempt
            if (ClientPhoneCache.planId().isBlank()) {
                NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                        PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(), PhoneFrame.getScreenH(), "Contacts", font);
                return;
            }
            PhoneFrame.render(graphics, this);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneFrame.renderHeader(graphics, this, "Contacts", true);
        List<Map.Entry<String, String>> contacts = contacts();
        Set<String> online = ClientPhoneCache.onlinePlayers().stream()
                .map(ClientPhoneCache.OnlinePlayer::number).collect(Collectors.toSet());
        int first = Math.clamp(scroll, 0, Math.max(0, contacts.size() - VISIBLE_ROWS));
        for (int index = first; index < Math.min(first + VISIBLE_ROWS, contacts.size()); index++) {
            Map.Entry<String, String> contact = contacts.get(index);
            int rowY = y + 68 + (index - first) * 46;
            PhoneRenderer.drawCard(graphics, style, x + 10, rowY, 184, 40);
            graphics.drawString(font, fit(contact.getKey(), 145), x + 17, rowY + 7,
                    PhoneColors.textPrimary(style), true);
            graphics.drawString(font, contact.getValue(), x + 17, rowY + 23,
                    PhoneColors.textSecondary(style), true);
            int statusColor = online.contains(contact.getValue())
                    ? PhoneColors.success(style) : PhoneColors.textSecondary(style);
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

    private void renderActions(GuiGraphics graphics, int x, int y, PhoneStyle style) {
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
            if (button == 0 && lx >= x + 38 && lx < x + 166 && ly >= y + 154 && ly < y + 282) {
                int action = (int) ((ly - (y + 154)) / 32);
                String number = ClientPhoneCache.contacts().get(selectedName);
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
        if (button == 0 && ly >= y + 322 && ly < y + 346 && lx >= x + 48 && lx < x + 156) {
            minecraft.setScreen(new ContactEditModalScreen(menu));
            return true;
        }
        if (button == 0 && ly >= y + 68 && ly < y + 298) {
            int row = (int) ((ly - (y + 68)) / 46) + scroll;
            List<Map.Entry<String, String>> contacts = contacts();
            if (row >= 0 && row < contacts.size()) {
                selectedName = contacts.get(row).getKey();
                return true;
            }
        }
        return super.mouseClicked(lx, ly, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, contacts().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
