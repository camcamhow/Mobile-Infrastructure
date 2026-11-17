package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSmsCache;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SmsDataSyncPacket;
import com.example.mobilecellnetwork.network.VoicemailNotePacket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class VoicemailScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE_ENTRIES = 5;
    private static final int ROW_HEIGHT = 30;
    private static final int ROW_GAP = 4;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());

    private final PhoneMenu menu;
    private int selected = -1;
    private EditBox note;
    private int scroll;
    private boolean requestedData;
    private List<SmsDataSyncPacket.VoicemailEntry> displayedEntries = List.of();

    public VoicemailScreen(PhoneMenu menu) {
        super(Component.literal("Voicemail"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        if (!requestedData) {
            PacketDistributor.sendToServer(new RequestPhoneDataPacket());
            requestedData = true;
        }
        rebuildWidgets(null);
    }

    private void rebuildWidgets(String draft) {
        clearWidgets();
        note = null;
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        List<SmsDataSyncPacket.VoicemailEntry> entries = ClientSmsCache.voicemail();
        displayedEntries = List.copyOf(entries);
        scroll = Math.clamp(scroll, 0, Math.max(0, entries.size() - VISIBLE_ENTRIES));
        int first = Math.min(scroll, Math.max(0, entries.size() - VISIBLE_ENTRIES));
        for (int index = first; index < Math.min(first + VISIBLE_ENTRIES, entries.size()); index++) {
            int entryIndex = index;
            int rowY = top + 64 + (index - first) * (ROW_HEIGHT + ROW_GAP);
            addRenderableWidget(new PhoneActionButton(left + 8, rowY, 188, ROW_HEIGHT,
                    "Select voicemail from " + entries.get(index).caller(),
                    button -> {
                        selected = entryIndex;
                        rebuildWidgets(null);
                    }));
        }

        if (selected >= 0 && selected < entries.size()) {
            var entry = entries.get(selected);
            note = PhoneTextField.create(font, left + 10, top + 269, 184, 24, "Add a note", 160,
                    ClientPhoneCache.phoneStyle());
            note.setValue(draft == null ? entry.note() : draft);
            addRenderableWidget(note);
            addRenderableWidget(new PhoneActionButton(left + 10, top + 300, 88, 20, "Save note",
                    button -> PacketDistributor.sendToServer(
                            new VoicemailNotePacket(selected, note.getValue()))));
            PhoneActionButton callback = new PhoneActionButton(left + 104, top + 300, 90, 20, "Call back",
                    button -> PacketDistributor.sendToServer(new CallRequestPacket(entry.caller())));
            callback.active = !entry.caller().isBlank();
            addRenderableWidget(callback);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No service gating: show overlay if no active plan and screen is not exempt
            if (ClientPhoneCache.planId().isBlank()) {
                PhoneFrame.render(graphics, this);
                NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                        PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(),
                        PhoneFrame.getScreenH() - 24, "Voicemail", font);
                PhoneFrame.renderStatusBar(graphics, this);
                return;
            }
        List<SmsDataSyncPacket.VoicemailEntry> updated = ClientSmsCache.voicemail();
        if (!displayedEntries.equals(updated)) {
            SmsDataSyncPacket.VoicemailEntry previous = selected >= 0 && selected < displayedEntries.size()
                    ? displayedEntries.get(selected) : null;
            String draft = note == null ? null : note.getValue();
            selected = -1;
            if (previous != null) {
                for (int i = 0; i < updated.size(); i++) {
                    var entry = updated.get(i);
                    if (entry.timestamp() == previous.timestamp() && entry.caller().equals(previous.caller())) {
                        selected = i;
                        break;
                    }
                }
            }
            rebuildWidgets(selected < 0 ? null : draft);
        }
            PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        boolean snapple = ClientPhoneCache.phoneStyle() == PhoneStyle.IOS;
        if (snapple) {
            SnappleAppLayout.screen(graphics, font, left, top, "Voicemail");
        } else {
            PhoneFrame.renderPanel(graphics, left, PhoneFrame.getContentTop(this),
                    PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
            PhoneFrame.renderHeader(graphics, this, "Voicemail", true);
        }

        if (snapple) {
            SnappleAppLayout.group(graphics, left + 8, top + 64, 188, 168);
        } else {
            PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(), left + 4, top + 58,
                    PhoneFrame.getScreenW() - 8, 178);
            if (ClientPhoneCache.phoneStyle() != PhoneStyle.SAMSUNG_ONE_UI) {
                graphics.drawString(font, "RECENT VOICEMAIL", left + 12, top + 48,
                        PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()), true);
            }
        }

        List<SmsDataSyncPacket.VoicemailEntry> entries = ClientSmsCache.voicemail();
        int first = Math.min(scroll, Math.max(0, entries.size() - VISIBLE_ENTRIES));
        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, "No voicemails yet", left + PhoneFrame.getScreenW() / 2,
                    top + 130, PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
            graphics.drawCenteredString(font, "Missed messages will appear here",
                    left + PhoneFrame.getScreenW() / 2, top + 145,
                    PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
        } else {
            for (int index = first; index < Math.min(first + VISIBLE_ENTRIES, entries.size()); index++) {
                int rowY = top + 64 + (index - first) * (ROW_HEIGHT + ROW_GAP);
                boolean isSelected = index == selected;
                if (!snapple) {
                    PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(), left + 8, rowY, 188, ROW_HEIGHT);
                } else if (index > first) {
                    SnappleAppLayout.separator(graphics, left + 22, rowY - 2, 166);
                }
                if (isSelected) {
                    PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(),
                            left + 10, rowY + 2, 3, ROW_HEIGHT - 4, 2,
                            PhoneColors.accent(ClientPhoneCache.phoneStyle()));
                }
                String caller = entries.get(index).caller().isBlank() ? "Unknown caller" : entries.get(index).caller();
                graphics.drawString(font, fit(caller, 118), left + 17, rowY + 5,
                        PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()), true);
                graphics.drawString(font, timestamp(entries.get(index).timestamp()), left + 17, rowY + 17,
                        PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()), true);
                graphics.drawString(font, ">", left + 184, rowY + 9,
                        PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()), true);
            }
        }

        boolean androidEmpty = ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI && entries.isEmpty();
        if (snapple) {
            SnappleAppLayout.group(graphics, left + 8, top + 239, 188, 88);
        } else if (!androidEmpty) {
            PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(), left + 4, top + 239,
                    PhoneFrame.getScreenW() - 8, 88);
        }
        if (selected >= 0 && selected < entries.size()) {
            var entry = entries.get(selected);
            graphics.drawString(font, "From " + fit(entry.caller().isBlank() ? "Unknown caller" : entry.caller(),
                    148), left + 12, top + 247, PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()), true);
            PhoneTextField.renderBackground(graphics, ClientPhoneCache.phoneStyle(),
                    left + 10, top + 269, 184, 24);
        } else if (!androidEmpty) {
            graphics.drawCenteredString(font, entries.isEmpty() ? "Your inbox is clear" : "Select a message",
                    left + PhoneFrame.getScreenW() / 2, top + 268,
                    PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
            if (!entries.isEmpty()) {
                graphics.drawCenteredString(font, "Add a note or call back",
                        left + PhoneFrame.getScreenW() / 2, top + 283,
                        PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
            }
        }

        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        if (selected >= 0 && selected < entries.size()) {
            drawActionButton(graphics, left + 10, top + 300, 88, 20, "Save note", true,
                    layoutMouseX, layoutMouseY);
            drawActionButton(graphics, left + 104, top + 300, 90, 20, "Call back",
                    !entries.get(selected).caller().isBlank(), layoutMouseX, layoutMouseY);
        }
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void drawActionButton(GuiGraphics graphics, int x, int y, int width, int height,
                                  String label, boolean enabled, int mouseX, int mouseY) {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            SnappleAppLayout.pill(graphics, font, x, y, width, height, label,
                    PhoneColors.bgTertiary(PhoneStyle.IOS),
                    enabled ? PhoneColors.accent(PhoneStyle.IOS) : PhoneColors.textSecondary(PhoneStyle.IOS));
            return;
        }
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        int color = enabled && hovered ? PhoneColors.accent(ClientPhoneCache.phoneStyle())
                : PhoneColors.bgSecondary(ClientPhoneCache.phoneStyle());
        if (!enabled) {
            color = PhoneColors.divider(ClientPhoneCache.phoneStyle());
        }
        PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(), x, y, width, height, 7, color);
        graphics.drawCenteredString(font, label, x + width / 2, y + 6,
                PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
    }

    private String timestamp(long timestamp) {
        return TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    private String fit(String text, int maxWidth) {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) return SnappleAppLayout.fit(font, text, maxWidth);
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width(suffix))) + suffix;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ClientPhoneCache.planId().isBlank()) {
            return false;
        }
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double layoutX = PhoneFrame.layoutMouseX(this, mouseX);
        double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
        int listTop = PhoneFrame.getScreenY(this) + 64;
        if (scrollY != 0 && layoutX >= PhoneFrame.getScreenX(this)
                && layoutX < PhoneFrame.getScreenX(this) + PhoneFrame.getScreenW()
                && layoutY >= listTop && layoutY < listTop + 172) {
            int maximum = Math.max(0, ClientSmsCache.voicemail().size() - VISIBLE_ENTRIES);
            int next = Math.clamp(scroll + (scrollY < 0 ? 1 : -1), 0, maximum);
            if (next != scroll) {
                scroll = next;
                rebuildWidgets(note == null ? null : note.getValue());
            }
            return true;
        }
        return super.mouseScrolled(layoutX, layoutY, scrollX, scrollY);
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
    public boolean isPauseScreen() {
        return false;
    }
}
