package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.StartChatPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NewMessageModalScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private EditBox recipient;
    private EditBox message;
    private String error = "";

    public NewMessageModalScreen(PhoneMenu menu) {
        super(Component.literal("New Message"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() { return menu; }

    @Override
    protected void init() {
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 150;
        recipient = PhoneTextField.create(font, x + 8, y + 46, 168, 22,
                "Phone number or contact", 64, ClientPhoneCache.phoneStyle());
        message = PhoneTextField.create(font, x + 8, y + 80, 168, 22,
                "Message (optional)", 500, ClientPhoneCache.phoneStyle());
        addRenderableWidget(recipient);
        addRenderableWidget(message);
        addRenderableWidget(new PhoneActionButton(x + 8, y + 118, 78, 22, "Send",
                button -> send()));
        addRenderableWidget(new PhoneActionButton(x + 98, y + 118, 78, 22, "Cancel",
                button -> cancel()));
        recipient.setFocused(true);
    }

    private void send() {
        String target = recipient.getValue().strip();
        if (!target.matches("[0-9]{1,9}")) {
            String contactName = target;
            target = ClientPhoneCache.contacts().entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(contactName))
                    .map(entry -> entry.getValue()).findFirst().orElse("");
        }
        if (!target.matches("[0-9]{1,9}")) {
            error = "Enter a valid contact or phone number.";
            return;
        }
        PacketDistributor.sendToServer(new StartChatPacket(target));
        minecraft.setScreen(new SmsScreen(menu, target, "", message.getValue()));
    }

    private void cancel() {
        minecraft.setScreen(new SmsScreen(menu));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 150;
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, x, y, 192, 168);
        PhoneRenderer.drawHeader(graphics, style, x, y + 10,
                "New Message", font, false);
        PhoneTextField.renderBackground(graphics, style, x + 8, y + 46, 168, 22);
        PhoneTextField.renderBackground(graphics, style, x + 8, y + 80, 168, 22);
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        drawButton(graphics, style, x + 8, y + 118, 78, 22, "Send", true);
        drawButton(graphics, style, x + 98, y + 118, 78, 22, "Cancel", false);
        if (!error.isEmpty()) {
            graphics.drawString(font, error, x + 8, y + 145, PhoneColors.error(style), true);
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void drawButton(GuiGraphics graphics, PhoneStyle style, int x, int y, int w, int h,
                            String label, boolean primary) {
        if (primary) PhoneRenderer.drawPrimaryButton(graphics, style, x, y, w, h, label, font);
        else PhoneRenderer.drawSecondaryButton(graphics, style, x, y, w, h, label, font);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = PhoneFrame.layoutMouseX(this, mouseX);
        double ly = PhoneFrame.layoutMouseY(this, mouseY);
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 150;
        if (button == 0 && ly >= y + 118 && ly < y + 140) {
            if (lx >= x + 8 && lx < x + 86) send();
            else if (lx >= x + 98 && lx < x + 176) cancel();
            return true;
        }
        return super.mouseClicked(lx, ly, button);
    }

    @Override
    public void navigateBack() { cancel(); }

    @Override
    public void navigateHome() { PhoneNavigation.openHome(this); }

    private int modalX() {
        return PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - 192) / 2;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
}
