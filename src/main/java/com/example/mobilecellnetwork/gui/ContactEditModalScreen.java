package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ContactsActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ContactEditModalScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private final String previousName;
    private EditBox name;
    private EditBox number;
    private String error = "";

    public ContactEditModalScreen(PhoneMenu menu) {
        this(menu, "", "");
    }

    public ContactEditModalScreen(PhoneMenu menu, String previousName, String number) {
        super(Component.literal(previousName == null || previousName.isBlank()
                ? "New Contact" : "Edit Contact"));
        this.menu = menu;
        this.previousName = previousName == null ? "" : previousName;
        this.initialNumber = number == null ? "" : number;
    }

    private final String initialNumber;

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        int x = modalX();
        int y = modalY();
        name = PhoneTextField.create(font, x + 10, y + 54, 164, 22,
                "Name", 64, ClientPhoneCache.phoneStyle());
        number = PhoneTextField.create(font, x + 10, y + 91, 164, 22,
                "Phone Number", 9, ClientPhoneCache.phoneStyle());
        if (!previousName.isBlank()) {
            name.setValue(previousName);
        }
        number.setValue(initialNumber);
        addRenderableWidget(name);
        addRenderableWidget(number);
        name.setFocused(previousName.isBlank());
        if (!previousName.isBlank()) {
            number.setFocused(true);
        }
    }

    private int modalX() {
        return PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - 192) / 2;
    }

    private int modalY() {
        return PhoneFrame.getScreenY(this) + (PhoneFrame.getScreenH() - 160) / 2;
    }

    private void save() {
        String contactName = name.getValue().strip();
        String phoneNumber = number.getValue().strip();
        if (contactName.isBlank() || !phoneNumber.matches("[0-9]{1,9}")) {
            error = "Enter a name and 1-9 digits.";
            return;
        }
        PacketDistributor.sendToServer(new ContactsActionPacket(
                previousName.isBlank() ? "add" : "edit", previousName, contactName, phoneNumber));
        minecraft.setScreen(new ContactsScreen(menu));
    }

    private void cancel() {
        minecraft.setScreen(new ContactsScreen(menu));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = modalX();
        int y = modalY();
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        if (style == PhoneStyle.IOS) {
            SnappleAppLayout.screen(graphics, font, PhoneFrame.getScreenX(this),
                    PhoneFrame.getScreenY(this), "Contacts");
            SnappleAppLayout.group(graphics, x, y, 192, 160);
            SnappleText.draw(graphics, font, previousName.isBlank() ? "New Contact" : "Edit Contact",
                    x + 10, y + 17, PhoneColors.textPrimary(style));
            SnappleText.draw(graphics, font, "Name", x + 10, y + 42, PhoneColors.textSecondary(style));
            SnappleText.draw(graphics, font, "Phone number", x + 10, y + 79, PhoneColors.textSecondary(style));
        } else {
            PhoneRenderer.drawCard(graphics, style, x, y, 192, 160);
            PhoneRenderer.drawHeader(graphics, style, x + 4, y + 10,
                    previousName.isBlank() ? "New Contact" : "Edit Contact", font, false);
        }
        PhoneTextField.renderBackground(graphics, style, x + 10, y + 54, 164, 22);
        PhoneTextField.renderBackground(graphics, style, x + 10, y + 91, 164, 22);
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        if (style == PhoneStyle.IOS) {
            SnappleAppLayout.pill(graphics, font, x + 10, y + 119, 76, 22, "Save",
                    PhoneColors.accent(style), 0xFFFFFFFF);
            SnappleAppLayout.pill(graphics, font, x + 98, y + 119, 76, 22, "Cancel",
                    PhoneColors.bgTertiary(style), PhoneColors.accent(style));
        } else {
            PhoneRenderer.drawPrimaryButton(graphics, style, x + 10, y + 119, 76, 22, "Save", font);
            PhoneRenderer.drawSecondaryButton(graphics, style, x + 98, y + 119, 76, 22, "Cancel", font);
        }
        if (!error.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(error, 172),
                    x + 10, y + 144, PhoneColors.error(style), true);
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double lx = PhoneFrame.layoutMouseX(this, mouseX);
        double ly = PhoneFrame.layoutMouseY(this, mouseY);
        int x = modalX();
        int y = modalY();
        if (button == 0 && ly >= y + 119 && ly < y + 141) {
            if (lx >= x + 10 && lx < x + 86) save();
            else if (lx >= x + 98 && lx < x + 174) cancel();
            return true;
        }
        return super.mouseClicked(lx, ly, button);
    }

    @Override
    public void navigateBack() {
        cancel();
    }

    @Override
    public void navigateHome() {
        PhoneNavigation.openHome(this);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }
}
