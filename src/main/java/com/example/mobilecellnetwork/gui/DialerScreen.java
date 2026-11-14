package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.data.CallDirection;
import com.example.mobilecellnetwork.data.CallLogEntry;
import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientCallLogCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.ClientSmsCache;
import com.example.mobilecellnetwork.network.DeleteCallLogEntryPacket;
import com.example.mobilecellnetwork.network.DeleteVoicemailPacket;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.network.SmsDataSyncPacket;
import com.example.mobilecellnetwork.network.StartChatPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class DialerScreen extends Screen implements PhoneNavigableScreen {
    private static final int W = 204;
    private static final int TAB_Y = 316;
    private static final int TAB_H = 44;
    private static final int LIST_TOP = 72;
    private static final int RECENT_TOP = 76;
    private static final int RECENT_PITCH = 42;
    private static final int RECENT_ROWS = 5;
    private static final int CONTACT_PITCH = 46;
    private static final int CONTACT_ROWS = 5;
    private static final int VOICEMAIL_PITCH = 56;
    private static final int VOICEMAIL_ROWS = 4;
    private static final int MAX_DIGITS = 9;
    private static final long LONG_PRESS_MS = 500L;
    private static final String KEYS = "123456789*0#";
    private static final String[] LETTERS = {"", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ",
            "", "+", ""};

    private enum RecentFilter {
        ALL("All"), MISSED("Missed"), INCOMING("Incoming"), OUTGOING("Outgoing");

        final String label;

        RecentFilter(String label) {
            this.label = label;
        }

        boolean matches(CallLogEntry entry) {
            return switch (this) {
                case ALL -> true;
                case MISSED -> entry.direction() == CallDirection.MISSED;
                case INCOMING -> entry.direction() == CallDirection.INCOMING;
                case OUTGOING -> entry.direction() == CallDirection.OUTGOING;
            };
        }
    }

    private record VoicemailRow(int index, SmsDataSyncPacket.VoicemailEntry entry) {
    }

    private final PhoneMenu menu;
    private PhoneTab activeTab = PhoneTab.KEYPAD;
    private final StringBuilder number = new StringBuilder();
    private RecentFilter filter = RecentFilter.ALL;
    private int recentsScroll;
    private int contactsScroll;
    private int voicemailScroll;
    private CallLogEntry sheetEntry;
    private String toast = "";
    private long toastUntil;
    private long backHeldSince;
    private boolean backLongPressed;

    public DialerScreen(PhoneMenu menu) {
        super(Component.literal("Phone"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        if (sheetEntry != null) {
            sheetEntry = null;
        } else {
            PhoneNavigation.openHome(this);
        }
    }

    @Override
    public void navigateHome() {
        PhoneNavigation.openHome(this);
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneFrame.renderHeader(graphics, this, "Phone", true);
        if (backHeldSince > 0L && !backLongPressed
                && System.currentTimeMillis() - backHeldSince >= LONG_PRESS_MS) {
            number.setLength(0);
            backLongPressed = true;
        }
        switch (activeTab) {
            case KEYPAD -> renderKeypad(graphics, style, x, y);
            case RECENTS -> renderRecents(graphics, style, x, y);
            case CONTACTS -> renderContacts(graphics, style, x, y);
            case VOICEMAIL -> renderVoicemail(graphics, style, x, y);
        }
        renderTabBar(graphics, style, x, y);
        if (sheetEntry != null) {
            renderSheet(graphics, style, x, y);
        }
        if (System.currentTimeMillis() < toastUntil) {
            int width = font.width(toast) + 16;
            PhoneRenderer.fillRoundedRect(graphics, style, x + (W - width) / 2, y + 52, width, 16, 8,
                    PhoneColors.bgTertiary(style));
            drawCentered(graphics, toast, x + W / 2, y + 56, 1.0F, PhoneColors.textPrimary(style));
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderTabBar(GuiGraphics graphics, PhoneStyle style, int x, int y) {
        graphics.fill(x, y + TAB_Y, x + W, y + TAB_Y + 1, PhoneColors.divider(style));
        int tabWidth = W / PhoneTab.values().length;
        for (PhoneTab tab : PhoneTab.values()) {
            boolean active = tab == activeTab;
            int color = active ? PhoneColors.textPrimary(style) : PhoneColors.textSecondary(style);
            int centerX = x + tab.ordinal() * tabWidth + tabWidth / 2;
            drawCentered(graphics, tab.glyph, centerX, y + TAB_Y + 7, 1.2F, color);
            drawCentered(graphics, tab.label, centerX, y + TAB_Y + 22, 0.85F, color);
            if (active) {
                int labelWidth = (int) (font.width(tab.label) * 0.85F);
                graphics.fill(centerX - labelWidth / 2, y + TAB_Y + 33,
                        centerX + (labelWidth + 1) / 2, y + TAB_Y + 35, PhoneColors.accent(style));
            }
        }
    }

    private void renderKeypad(GuiGraphics graphics, PhoneStyle style, int x, int y) {
        String contact = contactName(number.toString());
        if (number.isEmpty()) {
            drawCentered(graphics, "Enter a number", x + W / 2, y + 57, 1.0F, PhoneColors.textSecondary(style));
        } else {
            drawCentered(graphics, PhoneRenderer.formatPhoneNumber(number.toString()), x + W / 2, y + 52, 2.0F,
                    PhoneColors.textPrimary(style));
        }
        if (contact != null) {
            drawCentered(graphics, contact, x + W / 2, y + 72, 1.1F, PhoneColors.textSecondary(style));
            drawCentered(graphics, contact + " - tap to call", x + W / 2, y + 85, 0.95F,
                    PhoneColors.accent(style));
        }
        for (int index = 0; index < KEYS.length(); index++) {
            int keyX = x + 16 + (index % 3) * 60;
            int keyY = y + 96 + (index / 3) * 43;
            PhoneRenderer.fillRoundedRect(graphics, style, keyX, keyY, 52, 40, 12, PhoneColors.bgSecondary(style));
            drawCentered(graphics, String.valueOf(KEYS.charAt(index)), keyX + 26, keyY + 5, 1.6F,
                    PhoneColors.textPrimary(style));
            if (!LETTERS[index].isEmpty()) {
                drawCentered(graphics, LETTERS[index], keyX + 26, keyY + 27, 0.8F,
                        PhoneColors.textSecondary(style));
            }
        }
        PhoneRenderer.fillRoundedRect(graphics, style, x + 22, y + 268, 40, 40, 20, PhoneColors.bgTertiary(style));
        drawCentered(graphics, "\u2190", x + 42, y + 283, 1.2F, PhoneColors.textPrimary(style));
        boolean canCall = !number.isEmpty();
        PhoneRenderer.fillRoundedRect(graphics, style, x + 136, y + 262, 52, 52, 26,
                canCall ? PhoneColors.success(style) : PhoneColors.bgTertiary(style));
        drawCentered(graphics, "\u260E", x + 162, y + 282, 1.5F,
                canCall ? 0xFFFFFFFF : PhoneColors.textSecondary(style));
    }

    private void renderRecents(GuiGraphics graphics, PhoneStyle style, int x, int y) {
        int filterX = x + 8;
        for (RecentFilter option : RecentFilter.values()) {
            int width = font.width(option.label) + 12;
            boolean active = option == filter;
            int color = active ? PhoneColors.textPrimary(style) : PhoneColors.textSecondary(style);
            graphics.drawString(font, option.label, filterX + 6, y + 56, color, true);
            if (active) {
                graphics.fill(filterX + 6, y + 68, filterX + 6 + font.width(option.label), y + 70,
                        PhoneColors.accent(style));
            }
            filterX += width + 3;
        }
        List<CallLogEntry> entries = filteredRecents();
        if (entries.isEmpty()) {
            drawCentered(graphics, "No recent calls", x + W / 2, y + 160, 1.0F, PhoneColors.textSecondary(style));
            return;
        }
        int first = Math.clamp(recentsScroll, 0, Math.max(0, entries.size() - RECENT_ROWS));
        for (int index = first; index < Math.min(entries.size(), first + RECENT_ROWS); index++) {
            CallLogEntry entry = entries.get(index);
            int rowY = y + RECENT_TOP + (index - first) * RECENT_PITCH;
            PhoneRenderer.drawCard(graphics, style, x + 8, rowY, 188, 38);
            int iconColor = switch (entry.direction()) {
                case INCOMING -> PhoneColors.accent(style);
                case OUTGOING -> PhoneColors.success(style);
                case MISSED -> PhoneColors.error(style);
            };
            String icon = switch (entry.direction()) {
                case INCOMING -> "\u2199";
                case OUTGOING -> "\u2197";
                case MISSED -> "\u2298";
            };
            graphics.drawString(font, icon, x + 17, rowY + 15, iconColor, true);
            String duration = entry.direction() == CallDirection.MISSED ? "" : formatDuration(entry.durationSeconds());
            int durationWidth = font.width(duration);
            graphics.drawString(font, fit(displayName(entry), 100), x + 32, rowY + 7,
                    entry.direction() == CallDirection.MISSED
                            ? PhoneColors.error(style) : PhoneColors.textPrimary(style), true);
            graphics.drawString(font, fit(PhoneRenderer.formatPhoneNumber(entry.otherNumber()) + " - "
                            + PhoneRenderer.formatRelativeTime(entry.timestamp()), 104), x + 32, rowY + 21,
                    PhoneColors.textSecondary(style), true);
            if (!duration.isEmpty()) {
                graphics.drawString(font, duration, x + 172 - durationWidth, rowY + 15,
                        PhoneColors.textSecondary(style), true);
            }
            graphics.drawString(font, "...", x + 178, rowY + 14, PhoneColors.textSecondary(style), true);
        }
    }

    private void renderContacts(GuiGraphics graphics, PhoneStyle style, int x, int y) {
        graphics.drawString(font, "Contacts", x + 12, y + 56, PhoneColors.textSecondary(style), true);
        graphics.drawString(font, "Edit", x + W - 12 - font.width("Edit"), y + 56, PhoneColors.accent(style), true);
        List<Map.Entry<String, String>> contacts = contacts();
        if (contacts.isEmpty()) {
            drawCentered(graphics, "No contacts yet - tap Edit to add one", x + W / 2, y + 160, 0.9F,
                    PhoneColors.textSecondary(style));
            return;
        }
        Set<String> online = onlineNumbers();
        int first = Math.clamp(contactsScroll, 0, Math.max(0, contacts.size() - CONTACT_ROWS));
        for (int index = first; index < Math.min(contacts.size(), first + CONTACT_ROWS); index++) {
            Map.Entry<String, String> contact = contacts.get(index);
            int rowY = y + LIST_TOP + (index - first) * CONTACT_PITCH;
            PhoneRenderer.drawCard(graphics, style, x + 8, rowY, 188, 40);
            graphics.drawString(font, fit(contact.getKey(), 78), x + 16, rowY + 8,
                    PhoneColors.textPrimary(style), true);
            graphics.drawString(font, contact.getValue(), x + 16, rowY + 23,
                    PhoneColors.textSecondary(style), true);
            int dot = online.contains(contact.getValue())
                    ? PhoneColors.success(style) : PhoneColors.textSecondary(style);
            PhoneRenderer.fillRoundedRect(graphics, style, x + 97, rowY + 17, 6, 6, 3, dot);
            PhoneRenderer.drawPrimaryButton(graphics, style, x + 108, rowY + 11, 34, 18, "Call", font);
            PhoneRenderer.drawSecondaryButton(graphics, style, x + 146, rowY + 11, 46, 18, "Message", font);
        }
    }

    private void renderVoicemail(GuiGraphics graphics, PhoneStyle style, int x, int y) {
        graphics.drawString(font, "Voicemail", x + 12, y + 56, PhoneColors.textSecondary(style), true);
        List<VoicemailRow> rows = voicemailRows();
        if (rows.isEmpty()) {
            drawCentered(graphics, "No voicemails", x + W / 2, y + 160, 1.0F, PhoneColors.textSecondary(style));
            return;
        }
        int first = Math.clamp(voicemailScroll, 0, Math.max(0, rows.size() - VOICEMAIL_ROWS));
        for (int index = first; index < Math.min(rows.size(), first + VOICEMAIL_ROWS); index++) {
            SmsDataSyncPacket.VoicemailEntry entry = rows.get(index).entry();
            int rowY = y + LIST_TOP + (index - first) * VOICEMAIL_PITCH;
            PhoneRenderer.drawCard(graphics, style, x + 8, rowY, 188, 52);
            graphics.drawString(font, fit(voicemailTitle(entry), 100), x + 16, rowY + 14,
                    PhoneColors.textPrimary(style), true);
            graphics.drawString(font, PhoneRenderer.formatRelativeTime(entry.timestamp()), x + 16, rowY + 28,
                    PhoneColors.textSecondary(style), true);
            PhoneRenderer.drawPrimaryButton(graphics, style, x + 132, rowY + 4, 60, 20, "Call back", font);
            PhoneRenderer.drawSecondaryButton(graphics, style, x + 132, rowY + 28, 60, 20, "Delete", font);
        }
    }

    private void renderSheet(GuiGraphics graphics, PhoneStyle style, int x, int y) {
        PhoneRenderer.fillRoundedRect(graphics, style, x, y, W, PhoneFrame.getScreenH(), 17, 0xAA000000);
        PhoneRenderer.drawCard(graphics, style, x + 22, y + 110, 160, 160);
        drawCentered(graphics, fit(displayName(sheetEntry), 140), x + W / 2, y + 118, 1.0F,
                PhoneColors.textPrimary(style));
        PhoneRenderer.drawPrimaryButton(graphics, style, x + 34, y + 136, 136, 26, "Call back", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 34, y + 166, 136, 26, "Send message", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 34, y + 196, 136, 26, "Add to Contacts", font);
        PhoneRenderer.fillRoundedRect(graphics, style, x + 34, y + 226, 136, 26, 10, PhoneColors.bgTertiary(style));
        drawCentered(graphics, "Delete entry", x + W / 2, y + 235, 1.0F, PhoneColors.error(style));
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double rx = PhoneFrame.layoutMouseX(this, mouseX) - PhoneFrame.getScreenX(this);
        double ry = PhoneFrame.layoutMouseY(this, mouseY) - PhoneFrame.getScreenY(this);
        if (sheetEntry != null) {
            if (button == 0) {
                handleSheetClick(rx, ry);
            } else {
                sheetEntry = null;
            }
            return true;
        }
        if (button == 0 && ry >= TAB_Y && ry < TAB_Y + TAB_H && rx >= 0 && rx < W) {
            activeTab = PhoneTab.values()[(int) (rx / (W / PhoneTab.values().length))
                    % PhoneTab.values().length];
            return true;
        }
        return switch (activeTab) {
            case KEYPAD -> button == 0 && clickKeypad(rx, ry);
            case RECENTS -> clickRecents(rx, ry, button);
            case CONTACTS -> button == 0 && clickContacts(rx, ry);
            case VOICEMAIL -> button == 0 && clickVoicemail(rx, ry);
        };
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && backHeldSince > 0L) {
            if (!backLongPressed && !number.isEmpty()) {
                number.setLength(number.length() - 1);
            }
            backHeldSince = 0L;
            backLongPressed = false;
            return true;
        }
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    private boolean clickKeypad(double rx, double ry) {
        for (int index = 0; index < KEYS.length(); index++) {
            if (inside(rx, ry, 16 + (index % 3) * 60, 96 + (index / 3) * 43, 52, 40)) {
                if (number.length() < MAX_DIGITS && Character.isDigit(KEYS.charAt(index))) {
                    number.append(KEYS.charAt(index));
                }
                return true;
            }
        }
        if (inside(rx, ry, 22, 268, 40, 40)) {
            backHeldSince = System.currentTimeMillis();
            backLongPressed = false;
            return true;
        }
        if (inside(rx, ry, 136, 262, 52, 52) && !number.isEmpty()) {
            placeCall(number.toString(), true);
            return true;
        }
        if (contactName(number.toString()) != null && inside(rx, ry, 8, 82, 188, 13)) {
            placeCall(number.toString(), true);
            return true;
        }
        return false;
    }

    private boolean clickRecents(double rx, double ry, int button) {
        if (button == 0 && ry >= 52 && ry < 72) {
            int filterX = 8;
            for (RecentFilter option : RecentFilter.values()) {
                int width = font.width(option.label) + 12;
                if (rx >= filterX && rx < filterX + width) {
                    filter = option;
                    recentsScroll = 0;
                    return true;
                }
                filterX += width + 3;
            }
            return false;
        }
        List<CallLogEntry> entries = filteredRecents();
        int first = Math.clamp(recentsScroll, 0, Math.max(0, entries.size() - RECENT_ROWS));
        for (int index = first; index < Math.min(entries.size(), first + RECENT_ROWS); index++) {
            int rowY = RECENT_TOP + (index - first) * RECENT_PITCH;
            if (inside(rx, ry, 8, rowY, 188, 38)) {
                if (button == 1 || (button == 0 && rx >= 174)) {
                    sheetEntry = entries.get(index);
                    return true;
                }
                return button == 0;
            }
        }
        return false;
    }

    private boolean clickContacts(double rx, double ry) {
        if (inside(rx, ry, W - 12 - font.width("Edit") - 6, 50, font.width("Edit") + 12, 18)) {
            minecraft.setScreen(new ContactsScreen(menu));
            return true;
        }
        List<Map.Entry<String, String>> contacts = contacts();
        int first = Math.clamp(contactsScroll, 0, Math.max(0, contacts.size() - CONTACT_ROWS));
        for (int index = first; index < Math.min(contacts.size(), first + CONTACT_ROWS); index++) {
            int rowY = LIST_TOP + (index - first) * CONTACT_PITCH;
            String contactNumber = contacts.get(index).getValue();
            if (inside(rx, ry, 108, rowY + 11, 34, 18)) {
                placeCall(contactNumber, false);
                return true;
            }
            if (inside(rx, ry, 146, rowY + 11, 46, 18)) {
                openMessage(contactNumber);
                return true;
            }
        }
        return false;
    }

    private boolean clickVoicemail(double rx, double ry) {
        List<VoicemailRow> rows = voicemailRows();
        int first = Math.clamp(voicemailScroll, 0, Math.max(0, rows.size() - VOICEMAIL_ROWS));
        for (int index = first; index < Math.min(rows.size(), first + VOICEMAIL_ROWS); index++) {
            int rowY = LIST_TOP + (index - first) * VOICEMAIL_PITCH;
            VoicemailRow row = rows.get(index);
            if (inside(rx, ry, 132, rowY + 4, 60, 20)) {
                if (!row.entry().caller().isBlank()) {
                    placeCall(row.entry().caller(), false);
                }
                return true;
            }
            if (inside(rx, ry, 132, rowY + 28, 60, 20)) {
                PacketDistributor.sendToServer(new DeleteVoicemailPacket(row.index()));
                return true;
            }
        }
        return false;
    }

    private boolean handleSheetClick(double rx, double ry) {
        CallLogEntry entry = sheetEntry;
        if (inside(rx, ry, 34, 136, 136, 26)) {
            sheetEntry = null;
            placeCall(entry.otherNumber(), false);
        } else if (inside(rx, ry, 34, 166, 136, 26)) {
            sheetEntry = null;
            openMessage(entry.otherNumber());
        } else if (inside(rx, ry, 34, 196, 136, 26)) {
            sheetEntry = null;
            minecraft.setScreen(new ContactEditModalScreen(menu, "", entry.otherNumber()));
        } else if (inside(rx, ry, 34, 226, 136, 26)) {
            sheetEntry = null;
            ClientCallLogCache.remove(entry.id());
            PacketDistributor.sendToServer(new DeleteCallLogEntryPacket(entry.id()));
        } else if (!inside(rx, ry, 22, 110, 160, 160)) {
            sheetEntry = null;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0 || sheetEntry != null) {
            return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                    PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
        }
        int delta = scrollY < 0 ? 1 : -1;
        switch (activeTab) {
            case RECENTS -> recentsScroll = Math.clamp(recentsScroll + delta,
                    0, Math.max(0, filteredRecents().size() - RECENT_ROWS));
            case CONTACTS -> contactsScroll = Math.clamp(contactsScroll + delta,
                    0, Math.max(0, contacts().size() - CONTACT_ROWS));
            case VOICEMAIL -> voicemailScroll = Math.clamp(voicemailScroll + delta,
                    0, Math.max(0, voicemailRows().size() - VOICEMAIL_ROWS));
            default -> {
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeTab == PhoneTab.KEYPAD && sheetEntry == null && Character.isDigit(codePoint)
                && number.length() < MAX_DIGITS) {
            number.append(codePoint);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeTab == PhoneTab.KEYPAD && keyCode == GLFW.GLFW_KEY_BACKSPACE && !number.isEmpty()) {
            number.setLength(number.length() - 1);
            return true;
        }
        if (activeTab == PhoneTab.KEYPAD && keyCode == GLFW.GLFW_KEY_ENTER && !number.isEmpty()) {
            placeCall(number.toString(), true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------------------------------------------------------------- actions & data

    private void placeCall(String target, boolean fromKeypad) {
        if (ClientSignalCache.tier() == SignalTier.NO_SERVICE) {
            showToast("No Service");
            return;
        }
        PacketDistributor.sendToServer(new CallRequestPacket(target));
        if (fromKeypad) {
            number.setLength(0);
        }
        minecraft.setScreen(new CallScreen(menu, target));
    }

    private void openMessage(String target) {
        PacketDistributor.sendToServer(new StartChatPacket(target));
        minecraft.setScreen(new SmsScreen(menu, target));
    }

    private void showToast(String message) {
        toast = message;
        toastUntil = System.currentTimeMillis() + 2000L;
    }

    private List<CallLogEntry> filteredRecents() {
        return ClientCallLogCache.entriesNewestFirst().stream().filter(filter::matches).toList();
    }

    private List<Map.Entry<String, String>> contacts() {
        return ClientPhoneCache.contacts().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Set<String> onlineNumbers() {
        return ClientPhoneCache.onlinePlayers().stream()
                .map(ClientPhoneCache.OnlinePlayer::number).collect(Collectors.toSet());
    }

    private List<VoicemailRow> voicemailRows() {
        List<SmsDataSyncPacket.VoicemailEntry> entries = ClientSmsCache.voicemail();
        List<VoicemailRow> rows = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            rows.add(new VoicemailRow(index, entries.get(index)));
        }
        rows.sort(Comparator.comparingLong((VoicemailRow row) -> row.entry().timestamp()).reversed());
        return rows;
    }

    private String contactName(String digits) {
        if (digits.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> contact : ClientPhoneCache.contacts().entrySet()) {
            if (contact.getValue().equals(digits)) {
                return contact.getKey();
            }
        }
        return null;
    }

    private String displayName(CallLogEntry entry) {
        String name = contactName(entry.otherNumber());
        if (name == null) {
            name = entry.otherName();
        }
        return name == null || name.isBlank() ? PhoneRenderer.formatPhoneNumber(entry.otherNumber()) : name;
    }

    private String voicemailTitle(SmsDataSyncPacket.VoicemailEntry entry) {
        if (entry.caller().isBlank()) {
            return "Unknown caller";
        }
        String name = contactName(entry.caller());
        return name != null ? name : PhoneRenderer.formatPhoneNumber(entry.caller());
    }

    private static String formatDuration(int seconds) {
        return seconds >= 60 ? (seconds / 60) + "m " + (seconds % 60) + "s" : seconds + "s";
    }

    private static boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private void drawCentered(GuiGraphics graphics, String text, int centerX, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - font.width(text) * scale / 2.0F, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, true);
        graphics.pose().popPose();
    }
}
