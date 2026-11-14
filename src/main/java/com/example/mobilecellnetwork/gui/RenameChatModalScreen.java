package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.data.Chat;
import com.example.mobilecellnetwork.network.ChatActionPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RenameChatModalScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private final Chat chat;
    private EditBox name;

    public RenameChatModalScreen(PhoneMenu menu, Chat chat) {
        super(Component.literal("Rename group"));
        this.menu = menu;
        this.chat = chat;
    }

    @Override
    public PhoneMenu phoneMenu() { return menu; }

    @Override
    protected void init() {
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 165;
        name = PhoneTextField.create(font, x + 8, y + 42, 152, 22,
                "Group name", 32, ClientPhoneCache.phoneStyle());
        name.setValue(chat.displayName());
        addRenderableWidget(name);
    }

    private void save() {
        if (!name.getValue().isBlank()) {
            PacketDistributor.sendToServer(new ChatActionPacket("rename", chat.id(), name.getValue().strip()));
        }
        cancel();
    }

    private void cancel() { minecraft.setScreen(new SmsScreen(menu)); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 165;
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, x, y, 176, 110);
        PhoneRenderer.drawHeader(graphics, style, x, y + 8, "Rename group", font, false);
        PhoneTextField.renderBackground(graphics, style, x + 8, y + 42, 152, 22);
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneRenderer.drawPrimaryButton(graphics, style, x + 8, y + 74, 70, 20, "Save", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 90, y + 74, 70, 20, "Cancel", font);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = PhoneFrame.layoutMouseX(this, mouseX);
        double ly = PhoneFrame.layoutMouseY(this, mouseY);
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 165;
        if (button == 0 && ly >= y + 74 && ly < y + 94) {
            if (lx >= x + 8 && lx < x + 78) save();
            else if (lx >= x + 90 && lx < x + 160) cancel();
            return true;
        }
        return super.mouseClicked(lx, ly, button);
    }

    @Override
    public void navigateBack() { cancel(); }

    private int modalX() {
        return PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - 176) / 2;
    }

    @Override
    public void navigateHome() { PhoneNavigation.openHome(this); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
}
