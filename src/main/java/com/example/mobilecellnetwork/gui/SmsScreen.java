package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.data.Chat;
import com.example.mobilecellnetwork.data.ChatType;
import com.example.mobilecellnetwork.data.EmergencyMessages;
import com.example.mobilecellnetwork.data.Message;
import com.example.mobilecellnetwork.network.ChatActionPacket;
import com.example.mobilecellnetwork.network.ClientChatCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.MarkChatReadPacket;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SmsSendPacket;
import com.example.mobilecellnetwork.network.packet.EmergencyMessagePacket;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class SmsScreen extends Screen implements PhoneNavigableScreen {
    private static final UUID GLOBAL_ID = new UUID(0L, 0L);
    private static final UUID EMERGENCY_ID = EmergencyMessages.CHAT_ID;
    private static final int IOS_LIST_TOP = 92;
    private static final int IOS_ROW_PITCH = 51;
    private static final int IOS_ROWS = 4;
    private final PhoneMenu menu;
    private final String requestedRecipient;
    private final String requestedGroupName;
    private EditBox compose;
    private EditBox inboxSearch;
    private String inboxQuery = "";
    private boolean inboxSearchActive;
    private boolean editInbox;
    private boolean unreadOnly;
    private UUID selectedChatId;
    private int scrollOffset;
    private UUID contextChatId;
    private String pendingMessage = "";
    private boolean pendingSent;
    private boolean attemptedOpen;
    private boolean requestedData;

    public SmsScreen(PhoneMenu menu) {
        this(menu, "", "");
    }

    public static SmsScreen emergency(PhoneMenu menu) {
        SmsScreen screen = new SmsScreen(menu);
        screen.selectedChatId = EMERGENCY_ID;
        return screen;
    }

    public SmsScreen(PhoneMenu menu, String recipientNumber) {
        this(menu, recipientNumber, "");
    }

    public SmsScreen(PhoneMenu menu, String recipientNumber, String groupName) {
        this(menu, recipientNumber, groupName, "");
    }

    public SmsScreen(PhoneMenu menu, String recipientNumber, String groupName, String initialMessage) {
        super(Component.literal("Messages"));
        this.menu = menu;
        this.requestedRecipient = recipientNumber == null ? "" : recipientNumber;
        this.requestedGroupName = groupName == null ? "" : groupName;
        this.pendingMessage = initialMessage == null ? "" : initialMessage;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        attemptedOpen = false;
        if (!requestedData) {
            requestedData = true;
            PacketDistributor.sendToServer(new RequestPhoneDataPacket());
        }
        if (selectedChatId != null) {
            int screenX = PhoneFrame.getScreenX(this);
            int screenY = PhoneFrame.getScreenY(this);
            compose = PhoneTextField.create(font, screenX + 12, screenY + 326,
                    136, 20, "Message", 500, ClientPhoneCache.phoneStyle());
            addRenderableWidget(compose);
        } else if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            int screenX = PhoneFrame.getScreenX(this);
            int screenY = PhoneFrame.getScreenY(this);
            inboxSearch = PhoneTextField.create(font, screenX + 10, screenY + 315,
                    139, 28, "Search", 64, PhoneStyle.IOS);
            inboxSearch.setValue(inboxQuery);
            inboxSearch.setResponder(value -> {
                inboxQuery = value;
                scrollOffset = 0;
            });
            addRenderableWidget(inboxSearch);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hasPlan = !ClientPhoneCache.planId().isBlank();
        boolean emergencySelected = EMERGENCY_ID.equals(selectedChatId);
        if (!hasPlan && selectedChatId != null && !emergencySelected) {
            PhoneFrame.render(graphics, this);
            NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                    PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24, "SMS", font);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        PhoneFrame.render(graphics, this);
        if (hasPlan) {
            tryOpenRequestedChat();
        } else if (selectedChatId == null) {
            attemptedOpen = true;
        }
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        if (selectedChatId == null) {
            renderChatList(graphics, x, y, style, mouseX, mouseY);
        } else {
            renderConversation(graphics, x, y, style, mouseX, mouseY);
        }
        if (selectedChatId != null) {
            PhoneTextField.renderBackground(graphics, style, x + 12, y + 326, 136, 20);
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        if (selectedChatId != null) {
            PhoneRenderer.drawPrimaryButton(graphics, style, x + 152, y + 326, 52, 20, "Send", font);
        }
        if (contextChatId != null) {
            renderContextMenu(graphics, x, y, style);
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderChatList(GuiGraphics graphics, int x, int y, PhoneStyle style,
                                int mouseX, int mouseY) {
        if (style == PhoneStyle.IOS) {
            renderSnappleInbox(graphics, x, y);
            return;
        }
        PhoneFrame.renderHeader(graphics, this, "Messages", true);
        if (!ClientPhoneCache.planId().isBlank()) {
            boolean android = style == PhoneStyle.SAMSUNG_ONE_UI;
            drawTextButton(graphics, style, x + (android ? 126 : 130), y + 38, android ? 30 : 34, "New");
            drawTextButton(graphics, style, x + (android ? 159 : 166), y + 38, android ? 40 : 42, "Group");
        }
        List<Chat> chats = visibleChats();
        int first = Math.clamp(scrollOffset, 0, Math.max(0, chats.size() - 5));
        int rowY = y + 68;
        for (int i = first; i < Math.min(chats.size(), first + 5); i++) {
            Chat chat = chats.get(i);
            int currentY = rowY + (i - first) * 52;
            boolean android = style == PhoneStyle.SAMSUNG_ONE_UI;
            if (android) {
                int avatarWidth = PhoneFrame.circularElementWidth(32);
                McnPrimitives.fillRoundedRect(graphics, x + 27 - avatarWidth / 2, currentY + 6,
                        avatarWidth, 32, 16, PhoneColors.bgTertiary(style));
                String initial = chat.displayName().isBlank() ? "?"
                        : chat.displayName().substring(0, 1).toUpperCase(Locale.ROOT);
                graphics.drawString(font, initial, x + 27 - font.width(initial) / 2, currentY + 17,
                        PhoneColors.accent(style), false);
                graphics.fill(x + 50, currentY + 48, x + 194, currentY + 49, PhoneColors.divider(style));
            } else {
                PhoneRenderer.drawCard(graphics, style, x + 8, currentY, 188, 46);
            }
            String title = fit(chat.displayName(), android ? 92 : 94);
            String preview = latestPreview(chat);
            graphics.drawString(font, title, x + (android ? 50 : 14), currentY + 5,
                    PhoneColors.textPrimary(style), !android);
            graphics.drawString(font, fit(preview, android ? 120 : 110), x + (android ? 50 : 14), currentY + 23,
                    PhoneColors.textSecondary(style), !android);
            String time = relativeTime(chat.latestTimestamp());
            graphics.drawString(font, time, x + 190 - font.width(time),
                    currentY + 5, PhoneColors.textSecondary(style), true);
            int unread = chat.unreadCount();
            if (unread > 0) {
                PhoneRenderer.fillRoundedRect(graphics, style, x + 178, currentY + 29, 14, 14, 7,
                        PhoneColors.accent(style));
                graphics.drawCenteredString(font, unread > 9 ? "9+" : Integer.toString(unread),
                        x + 185, currentY + 32, 0xFFFFFFFF);
            }
        }
        if (chats.isEmpty()) {
            graphics.drawCenteredString(font, "No conversations available",
                    x + PhoneFrame.getScreenW() / 2, y + 100, PhoneColors.textSecondary(style));
        }
    }

    private void renderSnappleInbox(GuiGraphics graphics, int x, int y) {
            PhoneStyle style = PhoneStyle.IOS;
            McnPrimitives.fillRoundedRect(graphics, x, y, PhoneFrame.getScreenW(), PhoneFrame.getScreenH(),
                    14, PhoneColors.bgPrimary(style));
            McnPrimitives.drawPhoneCamera(graphics, style, x, y, PhoneFrame.getScreenW());
            SnappleAppLayout.pill(graphics, font, x + 10, y + 31, 40, 22,
                    editInbox ? "Done" : "Edit", PhoneColors.bgSecondary(style), PhoneColors.textPrimary(style));
            SnappleAppLayout.pill(graphics, font, x + 143, y + 31, 51, 22,
                    unreadOnly ? "Unread" : "All", PhoneColors.bgSecondary(style), PhoneColors.accent(style));
            graphics.pose().pushPose();
            graphics.pose().translate(x + 10, y + 62, 0);
            graphics.pose().scale(1.6F, 1.6F, 1);
            SnappleText.draw(graphics, font, "Messages", 0, 0, PhoneColors.textPrimary(style));
            graphics.pose().popPose();

            List<Chat> chats = inboxChats();
            int first = Math.clamp(scrollOffset, 0, Math.max(0, chats.size() - IOS_ROWS));
            for (int i = first; i < Math.min(chats.size(), first + IOS_ROWS); i++) {
                Chat chat = chats.get(i);
                int rowY = y + IOS_LIST_TOP + (i - first) * IOS_ROW_PITCH;
                if (chat.unreadCount() > 0) {
                    McnPrimitives.fillRoundedRect(graphics, x + 5, rowY + 18, 5, 5, 2, PhoneColors.accent(style));
                }
                SnappleAppLayout.avatar(graphics, font, x + 27, rowY + 7, 27, chat.displayName());
                String time = relativeTime(chat.latestTimestamp());
                int timeWidth = font.width(SnappleText.text(time));
                int titleWidth = Math.max(0, 136 - timeWidth - 5);
                SnappleText.draw(graphics, font, fit(chat.displayName(), titleWidth), x + 50, rowY + 4,
                        PhoneColors.textPrimary(style));
                SnappleText.draw(graphics, font, time, x + 190 - timeWidth, rowY + 4, PhoneColors.textSecondary(style));
                SnappleText.draw(graphics, font, fit(latestPreview(chat), 140), x + 50, rowY + 19,
                        PhoneColors.textSecondary(style));
                if (editInbox && chat.type() != ChatType.GLOBAL && chat.type() != ChatType.EMERGENCY) {
                    SnappleText.draw(graphics, font, "Options", x + 50, rowY + 32, PhoneColors.accent(style));
                }
                SnappleAppLayout.separator(graphics, x + 50, rowY + 48, 144);
            }
            if (chats.isEmpty()) {
                SnappleText.centered(graphics, font, inboxQuery.isBlank()
                                ? (unreadOnly ? "No unread messages" : "No conversations")
                                : "No matching conversations",
                        x + 102, y + 175, PhoneColors.textSecondary(style));
            }
            PhoneTextField.renderBackground(graphics, style, x + 10, y + 315, 139, 28);
            if (!ClientPhoneCache.planId().isBlank()) {
                SnappleAppLayout.pill(graphics, font, x + 153, y + 315, 41, 28, "New",
                        PhoneColors.bgSecondary(style), PhoneColors.accent(style));
                SnappleAppLayout.pill(graphics, font, x + 87, y + 31, 50, 22, "Group",
                        PhoneColors.bgSecondary(style), PhoneColors.accent(style));
            }
        }

        private List<Chat> inboxChats() {
            String query = inboxQuery.strip().toLowerCase(Locale.ROOT);
            return visibleChats().stream().filter(chat -> !unreadOnly || chat.unreadCount() > 0)
                    .filter(chat -> chat.displayName().toLowerCase(Locale.ROOT).contains(query)
                            || chat.memberNumbers().stream().anyMatch(number -> number.contains(query))
                            || chat.messages().stream().anyMatch(message ->
                                    message.body().toLowerCase(Locale.ROOT).contains(query)))
                    .toList();
        }
    private void renderConversation(GuiGraphics graphics, int x, int y, PhoneStyle style,
                                    int mouseX, int mouseY) {
        Chat chat = selectedChat();
        if (chat == null) {
            if (EmergencyMessages.CHAT_ID.equals(selectedChatId)) {
                graphics.drawCenteredString(font, "Loading emergency thread...",
                        x + PhoneFrame.getScreenW() / 2, y + 178, PhoneColors.textSecondary(style));
                return;
            }
            selectedChatId = null;
            clearWidgets();
            init();
            return;
        }
        String header = chat.type() == ChatType.EMERGENCY ? "Emergency"
                : chat.type() == ChatType.GLOBAL ? "Global" : chat.displayName();
        graphics.drawString(font, "\u2039", x + 4, y + 38,
                PhoneColors.accent(style), true);
        graphics.drawCenteredString(font, fit(header, 126),
                x + PhoneFrame.getScreenW() / 2, y + 39, PhoneColors.textPrimary(style));
        if (chat.type() == ChatType.GROUP) {
            drawTextButton(graphics, style, x + 185, y + 38, 20, "...");
        }
        PhoneRenderer.drawCard(graphics, style, x + 8, y + 64, 188, 250);
        List<Message> messages = chat.messages().stream()
                .sorted(Comparator.comparingLong(Message::timestamp)).toList();
        int first = Math.max(0, messages.size() - 6 - scrollOffset);
        int stop = Math.min(messages.size(), first + 6);
        int messageY = y + 72;
        for (int index = first; index < stop; index++) {
            Message message = messages.get(index);
            if ("system".equalsIgnoreCase(message.senderNumber())) {
                graphics.drawCenteredString(font, fit(message.body(), 186), x + 110,
                        messageY + 4, PhoneColors.textSecondary(style));
                messageY += 31;
                continue;
            }
            boolean sent = message.senderNumber().equals(ClientPhoneCache.phoneNumber());
            List<net.minecraft.util.FormattedCharSequence> lines =
                    font.split(Component.literal(message.body()), 116);
            if (lines.isEmpty()) {
                lines = List.of(font.split(Component.literal(" "), 126).getFirst());
            }
            int shownLines = Math.min(3, lines.size());
            int contentWidth = 0;
            for (int line = 0; line < shownLines; line++) {
                contentWidth = Math.max(contentWidth, font.width(lines.get(line)));
            }
            int bubbleWidth = Math.clamp(contentWidth + 14, 45, 132);
            int bubbleHeight = shownLines * font.lineHeight + 8;
            int bubbleX = sent ? x + 196 - bubbleWidth - 8 : x + 16;
            if (!sent && chat.type() == ChatType.GROUP) {
                graphics.drawString(font, message.senderNumber(), bubbleX + 2, messageY,
                        PhoneColors.textSecondary(style), true);
                messageY += 10;
            }
            PhoneRenderer.drawBubble(graphics, style, bubbleX, messageY, bubbleWidth, bubbleHeight,
                    sent, lines.subList(0, shownLines), font);
            String stamp = DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(message.timestamp()));
            graphics.drawString(font, stamp, sent ? bubbleX + bubbleWidth - font.width(stamp) : bubbleX,
                    messageY + bubbleHeight + 2, PhoneColors.textSecondary(style), true);
            messageY += bubbleHeight + 16;
        }
        if (messages.isEmpty()) {
            graphics.drawCenteredString(font, "No messages yet",
                    x + PhoneFrame.getScreenW() / 2, y + 174, PhoneColors.textSecondary(style));
        }
    }

    private void renderContextMenu(GuiGraphics graphics, int x, int y, PhoneStyle style) {
        Chat chat = ClientChatCache.get(contextChatId);
        if (chat == null || chat.type() == ChatType.EMERGENCY) {
            contextChatId = null;
            return;
        }
        int menuX = x + 46;
        int menuY = y + 162;
        int options = chat.type() == ChatType.GROUP ? 3 : 1;
        PhoneRenderer.drawCard(graphics, style, menuX, menuY, 124, options * 24 + 8);
        int row = 0;
        if (chat.type() == ChatType.GROUP) {
            graphics.drawString(font, "Rename", menuX + 8, menuY + 7,
                    PhoneColors.textPrimary(style), true);
            graphics.drawString(font, "Leave", menuX + 8, menuY + 31,
                    PhoneColors.textPrimary(style), true);
            row = 2;
        }
        graphics.drawString(font, "Delete chat", menuX + 8, menuY + 7 + row * 24,
                PhoneColors.error(style), true);
    }

    private void tryOpenRequestedChat() {
        if (selectedChatId != null || attemptedOpen
                || (requestedRecipient.isBlank() && requestedGroupName.isBlank())) {
            return;
        }
        Chat target = requestedRecipient.isBlank()
                ? ClientChatCache.chats().stream().filter(chat -> chat.type() == ChatType.GROUP
                && chat.displayName().equals(requestedGroupName)).findFirst().orElse(null)
                : ClientChatCache.chats().stream().filter(chat -> chat.type() == ChatType.DIRECT
                && chat.memberNumbers().contains(requestedRecipient)).findFirst().orElse(null);
        if (target != null) {
            openChat(target);
            if (!pendingSent && !pendingMessage.isBlank()) {
                pendingSent = true;
                if (compose != null) {
                    compose.setValue(pendingMessage);
                    sendMessage();
                }
            }
        }
        attemptedOpen = target != null;
    }

    private List<Chat> visibleChats() {
        List<Chat> result = new ArrayList<>(ClientChatCache.chats());
        if (ClientPhoneCache.planId().isBlank()) {
            return result.stream().filter(chat -> EMERGENCY_ID.equals(chat.id())
                    || chat.type() == ChatType.EMERGENCY).toList();
        }
        if (result.stream().noneMatch(chat -> chat.type() == ChatType.GLOBAL)) {
            result.add(ClientChatCache.globalChat());
        }
        result.sort((left, right) -> {
            int leftPriority = left.type() == ChatType.EMERGENCY ? 0 : left.type() == ChatType.GLOBAL ? 1 : 2;
            int rightPriority = right.type() == ChatType.EMERGENCY ? 0 : right.type() == ChatType.GLOBAL ? 1 : 2;
            int priority = Integer.compare(leftPriority, rightPriority);
            if (priority != 0) return priority;
            if (left.pinnedToTop() != right.pinnedToTop()) {
                return left.pinnedToTop() ? -1 : 1;
            }
            return Long.compare(right.latestTimestamp(), left.latestTimestamp());
        });
        return result;
    }

    private Chat selectedChat() {
        if (GLOBAL_ID.equals(selectedChatId)) {
            return ClientChatCache.chats().stream().filter(chat -> chat.id().equals(GLOBAL_ID))
                    .findFirst().orElseGet(ClientChatCache::globalChat);
        }
        return ClientChatCache.get(selectedChatId);
    }

    private void openChat(Chat chat) {
        selectedChatId = chat.id();
        scrollOffset = 0;
        contextChatId = null;
        compose = null;
        inboxSearch = null;
        inboxSearchActive = false;
        clearWidgets();
        init();
        if (!GLOBAL_ID.equals(chat.id())) {
            PacketDistributor.sendToServer(new MarkChatReadPacket(chat.id()));
        }
    }

    private void sendMessage() {
        if (compose == null || compose.getValue().isBlank() || selectedChatId == null) {
            return;
        }
        String body = compose.getValue().strip();
        if (body.isEmpty()) {
            return;
        }
        Message local = new Message(ClientPhoneCache.phoneNumber(), body, System.currentTimeMillis(), true);
        ClientChatCache.addLocalMessage(selectedChatId, local);
        if (EmergencyMessages.CHAT_ID.equals(selectedChatId)) {
            PacketDistributor.sendToServer(new EmergencyMessagePacket(body));
        } else {
            PacketDistributor.sendToServer(new SmsSendPacket(selectedChatId.toString(), body));
        }
        compose.setValue("");
        scrollOffset = 0;
    }

    private void drawTextButton(GuiGraphics graphics, PhoneStyle style, int x, int y, int width, String label) {
        graphics.drawCenteredString(font, label, x + width / 2, y + 3, PhoneColors.accent(style));
    }

    private String latestPreview(Chat chat) {
        String preview = chat.messages().stream().max(Comparator.comparingLong(Message::timestamp))
                .map(Message::body).orElse(chat.type() == ChatType.EMERGENCY
                        ? "Send a message to emergency dispatch" : "Start a conversation");
        return preview.length() > 40 ? preview.substring(0, 40) + "..." : preview;
    }

    private String relativeTime(long timestamp) {
        if (timestamp <= 0) return "";
        long minutes = Math.max(0L, (System.currentTimeMillis() - timestamp) / 60_000L);
        if (minutes < 1) return "now";
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        LocalDate then = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
        if (then.equals(LocalDate.now().minusDays(1))) return "Yesterday";
        return then.format(DateTimeFormatter.ofPattern("MM/dd"));
    }

    private String fit(String text, int width) {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) return SnappleAppLayout.fit(font, text, width);
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double xMouse = PhoneFrame.layoutMouseX(this, mouseX);
        double yMouse = PhoneFrame.layoutMouseY(this, mouseY);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        if (contextChatId != null) {
            Chat chat = ClientChatCache.get(contextChatId);
            int menuY = y + 162;
            int optionCount = chat != null && chat.type() == ChatType.GROUP ? 3 : 1;
            if (button == 0 && chat != null && xMouse >= x + 46 && xMouse < x + 170
                    && yMouse >= menuY + 4 && yMouse < menuY + 4 + optionCount * 24) {
                int item = (int) ((yMouse - menuY - 4) / 24);
                if (chat.type() == ChatType.GROUP && item == 0) {
                    minecraft.setScreen(new RenameChatModalScreen(menu, chat));
                } else if (chat.type() == ChatType.GROUP && item == 1) {
                    PacketDistributor.sendToServer(new ChatActionPacket("leave", chat.id(), ""));
                } else {
                    PacketDistributor.sendToServer(new ChatActionPacket("delete", chat.id(), ""));
                }
                contextChatId = null;
                return true;
            }
            contextChatId = null;
            return true;
        }
        if (selectedChatId == null) {
            if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
                return clickSnappleInbox(xMouse, yMouse, x, y, button);
            }
            boolean android = ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI;
            if (!ClientPhoneCache.planId().isBlank()
                    && button == 0 && inside(xMouse, yMouse, x + (android ? 126 : 128), y + 34,
                            android ? 30 : 40, 28)) {
                minecraft.setScreen(new NewMessageModalScreen(menu));
                return true;
            }
            if (!ClientPhoneCache.planId().isBlank()
                    && button == 0 && inside(xMouse, yMouse, x + (android ? 159 : 164), y + 34,
                            android ? 40 : 48, 28)) {
                minecraft.setScreen(new NewGroupChatModalScreen(menu));
                return true;
            }
            if (yMouse >= y + 68 && yMouse < y + 328) {
                int row = (int) ((yMouse - (y + 68)) / 52) + scrollOffset;
                List<Chat> chats = visibleChats();
                if (row >= 0 && row < chats.size()) {
                    if (button == 1 && chats.get(row).type() != ChatType.GLOBAL
                            && chats.get(row).type() != ChatType.EMERGENCY) {
                        contextChatId = chats.get(row).id();
                    } else if (button == 0) {
                        openChat(chats.get(row));
                    }
                    return true;
                }
            }
        } else if (button == 0 && inside(xMouse, yMouse, x + 152, y + 322, 56, 28)) {
            sendMessage();
            return true;
        } else if (selectedChatId != null && button == 0
                && inside(xMouse, yMouse, x + 2, y + 34, 36, 26)) {
            selectedChatId = null;
            contextChatId = null;
            clearWidgets();
            init();
            return true;
        } else if (button == 0 && inside(xMouse, yMouse, x + 185, y + 34, 24, 26)) {
            Chat chat = selectedChat();
            if (chat != null && chat.type() == ChatType.GROUP) {
                contextChatId = chat.id();
            }
            return true;
        }
        return super.mouseClicked(xMouse, yMouse, button);
    }

    private boolean clickSnappleInbox(double mouseX, double mouseY, int x, int y, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, x + 10, y + 31, 40, 22)) {
                editInbox = !editInbox;
                return true;
            }
            if (inside(mouseX, mouseY, x + 143, y + 31, 51, 22)) {
                unreadOnly = !unreadOnly;
                scrollOffset = 0;
                return true;
            }
            if (!ClientPhoneCache.planId().isBlank()) {
                if (inside(mouseX, mouseY, x + 153, y + 315, 41, 28)) {
                    minecraft.setScreen(new NewMessageModalScreen(menu));
                    return true;
                }
                if (inside(mouseX, mouseY, x + 87, y + 31, 50, 22)) {
                    minecraft.setScreen(new NewGroupChatModalScreen(menu));
                    return true;
                }
            }
            if (inside(mouseX, mouseY, x + 10, y + 315, 139, 28) && inboxSearch != null) {
                inboxSearchActive = true;
                inboxSearch.setFocused(true);
                setFocused(inboxSearch);
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        List<Chat> chats = inboxChats();
        int first = Math.clamp(scrollOffset, 0, Math.max(0, chats.size() - IOS_ROWS));
        if (inside(mouseX, mouseY, x + 4, y + IOS_LIST_TOP, 192, IOS_ROWS * IOS_ROW_PITCH)) {
            int row = (int) ((mouseY - y - IOS_LIST_TOP) / IOS_ROW_PITCH);
            int index = first + row;
            if (index < chats.size()) {
                Chat chat = chats.get(index);
                boolean canManage = chat.type() != ChatType.GLOBAL && chat.type() != ChatType.EMERGENCY;
                if (button == 1 || (button == 0 && editInbox)) {
                    if (canManage) {
                        contextChatId = chat.id();
                        if (inboxSearch != null) {
                            inboxSearch.setFocused(false);
                            setFocused(null);
                        }
                    }
                } else if (button == 0) {
                    openChat(chat);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            if (selectedChatId == null) {
                int size = ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                        ? inboxChats().size() : visibleChats().size();
                int visible = ClientPhoneCache.phoneStyle() == PhoneStyle.IOS ? IOS_ROWS : 5;
                scrollOffset = Math.clamp(scrollOffset + (scrollY < 0 ? 1 : -1),
                        0, Math.max(0, size - visible));
            } else {
                Chat chat = selectedChat();
                if (chat != null) {
                    scrollOffset = Math.clamp(scrollOffset + (scrollY < 0 ? 1 : -1),
                            0, Math.max(0, chat.messages().size() - 6));
                }
            }
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedChatId != null && keyCode == 257) {
            sendMessage();
            return true;
        }
        if (selectedChatId != null && keyCode == 256) {
            selectedChatId = null;
            clearWidgets();
            init();
            return true;
        }
        if (selectedChatId == null && inboxSearchActive && keyCode == 256) {
            inboxSearchActive = false;
            if (inboxSearch != null) {
                inboxSearch.setValue("");
                inboxSearch.setFocused(false);
                setFocused(null);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void navigateBack() {
        if (selectedChatId != null) {
            selectedChatId = null;
            contextChatId = null;
            clearWidgets();
            init();
        } else if (inboxSearchActive) {
            inboxSearchActive = false;
            if (inboxSearch != null) {
                inboxSearch.setValue("");
                inboxSearch.setFocused(false);
                setFocused(null);
            }
        } else {
            PhoneNavigation.openHome(this);
        }
    }

    @Override
    public void navigateHome() {
        PhoneNavigation.openHome(this);
    }

    public void setPendingMessage(String message) {
        pendingMessage = message == null ? "" : message;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
}
