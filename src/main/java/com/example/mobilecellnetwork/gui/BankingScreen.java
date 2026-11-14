package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.BankTransferPacket;
import com.example.mobilecellnetwork.network.BankingOpenPacket;
import com.example.mobilecellnetwork.network.ClientBankAccountCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.util.CurrencyUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class BankingScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private EditBox recipient;
    private EditBox amount;
    private int scroll;
    private PhoneActionButton transferButton;
    private int selectedTransaction = -1;

    public BankingScreen(PhoneMenu menu) {
        super(Component.literal("Bank"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
        PacketDistributor.sendToServer(new BankingOpenPacket());
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        recipient = PhoneTextField.create(font, left + 14, top + 272, 176, 18,
                "Phone number", 9, style);
        addRenderableWidget(recipient);
        amount = PhoneTextField.create(font, left + 14, top + 295, 94, 18,
                "Amount", 12, style);
        addRenderableWidget(amount);
        transferButton = new PhoneActionButton(left + 115, top + 295, 75, 18,
                "Send", button -> transfer());
        addRenderableWidget(transferButton);
    }

    private void transfer() {
        if (PhoneDeviceClientCache.lockedBank() || PhoneDeviceClientCache.bankReadOnly()) {
            notifyPlayer(PhoneDeviceClientCache.lockedBank()
                    ? "This app has been remotely locked." : "This phone's bank account is read-only.");
            return;
        }
        if (recipient.getValue().matches("[0-9]{1,9}")) {
            try {
                long units = Long.parseLong(amount.getValue());
                if (units > 0) {
                    PacketDistributor.sendToServer(new BankTransferPacket(recipient.getValue(), units));
                    return;
                }
            } catch (NumberFormatException ignored) {
                notifyPlayer("Enter a valid recipient number and amount.");
                return;
            }
        }
        notifyPlayer("Enter a valid recipient number and amount.");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Banking", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, left + 8, top + 62, 188, 42);
        PhoneRenderer.drawCard(graphics, style, left + 8, top + 110, 188, 132);
        PhoneRenderer.drawCard(graphics, style, left + 8, top + 250, 188, 76);
        if (PhoneDeviceClientCache.lockedBank()) {
            graphics.fill(left + 8, top + 62, left + 196, top + 326, 0xE5101620);
            graphics.renderOutline(left + 8, top + 62, 188, 264, PhoneColors.ERROR_TEXT);
            graphics.drawCenteredString(font, "This app has been remotely locked",
                    left + 102, top + 177, PhoneColors.ERROR_TEXT);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
        graphics.drawString(font, "Available balance", left + 14, top + 68,
                PhoneColors.textSecondary(style), true);
        graphics.drawString(font, CurrencyUtil.format(ClientBankAccountCache.snapshot().balance()),
                left + 14, top + 81, PhoneColors.success(style), true);
        graphics.drawString(font, "Recent activity", left + 14, top + 116,
                PhoneColors.textPrimary(style), true);
        var transactions = ClientBankAccountCache.snapshot().transactions();
        int visibleRows = 4;
        int first = Math.min(scroll, Math.max(0, transactions.size() - visibleRows));
        if (transactions.isEmpty()) {
            graphics.drawString(font, "No transactions yet", left + 14, top + 137,
                    PhoneColors.textSecondary(style), true);
        }
        for (int i = first; i < Math.min(transactions.size(), first + visibleRows); i++) {
            var transaction = transactions.get(i);
            int rowY = top + 135 + (i - first) * 24;
            PhoneRenderer.drawCard(graphics, style, left + 10, rowY, 184, 21);
            graphics.drawString(font, font.plainSubstrByWidth(transaction.reason(), 112),
                    left + 14, rowY + 6, PhoneColors.textSecondary(style), true);
            String value = transaction.amount() > 0
                    ? "+" + CurrencyUtil.format(transaction.amount())
                    : CurrencyUtil.format(transaction.amount());
            int valueColor = transaction.amount() < 0 ? PhoneColors.error(style) : PhoneColors.success(style);
            graphics.drawString(font, value, left + 190 - font.width(value), rowY + 6,
                    valueColor, true);
        }
        graphics.drawString(font, "Transfer money", left + 14, top + 256,
                PhoneDeviceClientCache.bankReadOnly()
                        ? PhoneColors.textSecondary(style) : PhoneColors.textPrimary(style), true);
        if (PhoneDeviceClientCache.bankReadOnly()) {
            graphics.drawCenteredString(font, "Read-only access to previous owner's account",
                    left + 102, top + 281, PhoneColors.textSecondary(style));
        }
        PhoneTextField.renderBackground(graphics, style, left + 14, top + 272, 176, 18);
        PhoneTextField.renderBackground(graphics, style, left + 14, top + 295, 94, 18);
        PhoneTextField.renderBackground(graphics, style, left + 115, top + 295, 75, 18);
        transferButton.active = !PhoneDeviceClientCache.bankReadOnly();
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneRenderer.drawPrimaryButton(graphics, style,
                left + 115, top + 295, 75, 18,
                PhoneDeviceClientCache.bankReadOnly() ? "Read only" : "Send", font);
        if (selectedTransaction >= 0 && selectedTransaction < transactions.size()) {
            renderTransactionDetails(graphics, left, top, style, transactions.get(selectedTransaction));
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderTransactionDetails(GuiGraphics graphics, int left, int top, PhoneStyle style,
                                          com.example.mobilecellnetwork.network.BankAccountSyncPacket.Transaction transaction) {
        graphics.fill(left + 4, top + 40, left + PhoneFrame.getScreenW() - 4,
                top + PhoneFrame.getScreenH() - 24, 0xB8000000);
        int x = left + 8;
        int y = top + 100;
        PhoneRenderer.fillRoundedRect(graphics, style, x - 2, y - 2, 192, 160, 16,
                PhoneColors.divider(style));
        PhoneRenderer.fillRoundedRect(graphics, style, x, y, 188, 156, 14,
                PhoneColors.bgPrimary(style));
        PhoneRenderer.drawDivider(graphics, style, x + 12, y + 28, 164);
        graphics.drawString(font, "Transaction details", x + 12, y + 12,
                PhoneColors.textPrimary(style), true);
        String amountText = CurrencyUtil.format(transaction.amount());
        graphics.drawString(font, amountText, x + 12, y + 39,
                transaction.amount() < 0 ? PhoneColors.error(style) : PhoneColors.success(style), true);
        drawDetailLine(graphics, "Type: " + transaction.reason(), x + 12, y + 61,
                164, PhoneColors.textSecondary(style));
        String details = transaction.details().isBlank() ? "No additional details"
                : transaction.details();
        drawDetailLine(graphics, details, x + 12, y + 80, 164,
                PhoneColors.textSecondary(style));
        drawDetailLine(graphics, formatTimestamp(transaction.timestamp()), x + 12, y + 105, 164,
                PhoneColors.textSecondary(style));
        PhoneRenderer.drawPrimaryButton(graphics, style, x + 60, y + 126, 68, 20, "Close", font);
    }

    private void drawDetailLine(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text, maxWidth), x, y, color, true);
    }

    private static String formatTimestamp(long timestamp) {
        if (timestamp >= 100_000_000_000L) {
            return "Recorded: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp));
        }
        return "Server tick: " + timestamp;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutX = PhoneFrame.layoutMouseX(this, mouseX);
        double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        var transactions = ClientBankAccountCache.snapshot().transactions();
        if (selectedTransaction >= 0) {
            boolean insideDialog = layoutX >= left + 6 && layoutX < left + 198
                    && layoutY >= top + 98 && layoutY < top + 258;
            boolean closeClicked = layoutX >= left + 68 && layoutX < left + 136
                    && layoutY >= top + 226 && layoutY < top + 246;
            if (!insideDialog || closeClicked) {
                selectedTransaction = -1;
            }
            return true;
        }
        int first = Math.min(scroll, Math.max(0, transactions.size() - 4));
        for (int index = first; index < Math.min(transactions.size(), first + 4); index++) {
            int rowY = top + 135 + (index - first) * 24;
            if (layoutX >= left + 10 && layoutX < left + 194
                    && layoutY >= rowY && layoutY < rowY + 21) {
                selectedTransaction = index;
                return true;
            }
        }
        return super.mouseClicked(layoutX, layoutY, button);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selectedTransaction >= 0) {
            return true;
        }
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, ClientBankAccountCache.snapshot().transactions().size() - 4));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (selectedTransaction >= 0 && keyCode == 256) {
            selectedTransaction = -1;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void notifyPlayer(String message) {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(message), true);
        }
    }

    private void back() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.setScreen(new PhoneScreen(menu, minecraft.player.getInventory(), Component.literal("Phone")));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
