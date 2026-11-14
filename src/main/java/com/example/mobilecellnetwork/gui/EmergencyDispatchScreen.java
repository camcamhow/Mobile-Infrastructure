package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientEmergencyDispatchCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.EmergencyDispatchDataPacket;
import com.example.mobilecellnetwork.network.EmergencyDispatchRequestPacket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class EmergencyDispatchScreen extends AbstractContainerScreen<EmergencyDispatchMenu> {
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listTop;
    private int listHeight;

    public EmergencyDispatchScreen(EmergencyDispatchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelX = 0;
        titleLabelY = -1000;
        inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        panelW = Math.min(420, width - 24);
        panelH = Math.min(320, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        imageWidth = panelW;
        imageHeight = panelH;
        leftPos = panelX;
        topPos = panelY;
        listTop = panelY + 40;
        listHeight = panelH - 82;
        addRenderableWidget(Button.builder(Component.literal("Refresh"),
                        button -> requestMessages())
                .bounds(panelX + panelW - 166, panelY + panelH - 28, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(panelX + panelW - 84, panelY + panelH - 28, 72, 20).build());
        requestMessages();
    }

    private void requestMessages() {
        PacketDistributor.sendToServer(new EmergencyDispatchRequestPacket());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF1E2A38);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + panelH - 2, 0xFF0F1620);
        graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF3A4A5A);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, PhoneColors.ERROR_TEXT);
        graphics.drawString(font, "Emergency Dispatch", panelX + 12, panelY + 14,
                PhoneColors.PRIMARY_TEXT, true);
        List<EmergencyDispatchDataPacket.Entry> messages = ClientEmergencyDispatchCache.messages();
        if (messages.isEmpty()) {
            graphics.drawCenteredString(font, "No emergency messages",
                    panelX + panelW / 2, listTop + 12, PhoneColors.SECONDARY_TEXT);
        } else {
            int rowHeight = 68;
            int visible = Math.max(1, listHeight / rowHeight);
            int first = Math.clamp(scroll, 0, Math.max(0, messages.size() - visible));
            graphics.enableScissor(panelX + 8, listTop, panelX + panelW - 8, listTop + listHeight);
            for (int index = first; index < Math.min(messages.size(), first + visible); index++) {
                EmergencyDispatchDataPacket.Entry message = messages.get(index);
                int y = listTop + (index - first) * rowHeight;
                PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(),
                        panelX + 8, y, panelW - 16, rowHeight - 4);
                String sender = message.senderName().isBlank() ? message.senderNumber() : message.senderName();
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(message.timestamp()));
                graphics.drawString(font, font.plainSubstrByWidth(sender + " (" + message.senderNumber() + ")",
                                panelW - 36),
                        panelX + 16, y + 5, PhoneColors.PRIMARY_TEXT, true);
                graphics.drawString(font, timestamp, panelX + panelW - 16 - font.width(timestamp),
                        y + 5, PhoneColors.SECONDARY_TEXT, true);
                graphics.drawString(font, fit(message.body(), panelW - 36), panelX + 16, y + 19,
                        PhoneColors.ERROR_TEXT, true);
                String location = message.dimension() + " " + message.x() + ", " + message.y() + ", " + message.z();
                graphics.drawString(font, font.plainSubstrByWidth(location, panelW - 36),
                        panelX + 16, y + 34, PhoneColors.SECONDARY_TEXT, true);
            }
            graphics.disableScissor();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String fit(String text, int widthLimit) {
        return font.plainSubstrByWidth(text, Math.max(0, widthLimit));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<EmergencyDispatchDataPacket.Entry> messages = ClientEmergencyDispatchCache.messages();
        int visible = Math.max(1, listHeight / 68);
        int next = Math.clamp(scroll - (int) Math.signum(scrollY), 0,
                Math.max(0, messages.size() - visible));
        if (next != scroll) {
            scroll = next;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
