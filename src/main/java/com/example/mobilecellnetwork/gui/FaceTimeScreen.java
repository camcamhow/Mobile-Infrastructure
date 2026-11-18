package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SignalTier;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FaceTimeScreen extends Screen implements PhoneNavigableScreen {
    private static final int ROWS = 3;
    private final PhoneMenu menu;
    private EditBox number;
    private int scroll;
    private String message = "";

    public FaceTimeScreen(PhoneMenu menu) {
        super(Component.literal("FaceTime"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        PhoneNavigation.openHome(this);
    }

    @Override
    protected void init() {
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        number = PhoneTextField.create(font, x + 12, y + 77, 120, 22,
                "Phone number", 9, PhoneStyle.IOS);
        number.setFilter(value -> value.matches("[0-9]*"));
        addRenderableWidget(number);
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
    }

    @Override
    protected void setInitialFocus() {
        if (!HeldPhoneView.active()) {
            super.setInitialFocus();
        }
    }

    private List<Map.Entry<String, String>> contacts() {
        return ClientPhoneCache.contacts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void call(String target) {
        if (target.isBlank()) {
            message = "Enter a number or choose a contact.";
        } else if (ClientPhoneCache.planId().isBlank()) {
            message = "Choose a plan to call.";
        } else if (ClientSignalCache.tier() == SignalTier.NO_SERVICE) {
            message = "No service.";
        } else {
            PacketDistributor.sendToServer(new CallRequestPacket(target, true));
            minecraft.setScreen(new CallScreen(menu, target));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        PhoneFrame.renderHeader(graphics, this, "FaceTime", true);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        SnappleText.draw(graphics, font, "New video call", x + 12, y + 58, PhoneColors.textPrimary(PhoneStyle.IOS));
        PhoneTextField.renderBackground(graphics, PhoneStyle.IOS, x + 12, y + 77, 120, 22);
        PhoneRenderer.drawPrimaryButton(graphics, PhoneStyle.IOS, x + 138, y + 77, 54, 22, "Call", font);
        SnappleText.draw(graphics, font, "Contacts", x + 12, y + 111, PhoneColors.textSecondary(PhoneStyle.IOS));
        List<Map.Entry<String, String>> contacts = contacts();
        int first = Math.clamp(scroll, 0, Math.max(0, contacts.size() - ROWS));
        if (contacts.isEmpty()) {
            SnappleText.draw(graphics, font, "No contacts yet", x + 14, y + 146, PhoneColors.textSecondary(PhoneStyle.IOS));
        }
        for (int i = first; i < Math.min(first + ROWS, contacts.size()); i++) {
            var contact = contacts.get(i);
            int rowY = y + 130 + (i - first) * 46;
            SnappleAppLayout.group(graphics, x + 10, rowY, 184, 40);
            SnappleText.draw(graphics, font, SnappleAppLayout.fit(font, contact.getKey(), 125),
                    x + 18, rowY + 6, PhoneColors.textPrimary(PhoneStyle.IOS));
            SnappleText.draw(graphics, font, contact.getValue(), x + 18, rowY + 22,
                    PhoneColors.textSecondary(PhoneStyle.IOS));
            SnappleAppLayout.pill(graphics, font, x + 152, rowY + 10, 34, 20, "Call",
                    PhoneColors.bgTertiary(PhoneStyle.IOS), PhoneColors.accent(PhoneStyle.IOS));
        }
        SnappleText.draw(graphics, font, "During a call, tap Camera on.", x + 12, y + 276,
                PhoneColors.textSecondary(PhoneStyle.IOS));
        SnappleText.draw(graphics, font, "Each person controls their camera.", x + 12, y + 291,
                PhoneColors.textSecondary(PhoneStyle.IOS));
        if (!message.isEmpty()) {
            SnappleText.draw(graphics, font, SnappleAppLayout.fit(font, message, 180), x + 12, y + 312,
                    PhoneColors.error(PhoneStyle.IOS));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this), top = PhoneFrame.getScreenY(this);
        if (button == 0 && x >= left + 138 && x < left + 192 && y >= top + 77 && y < top + 99) {
            call(number.getValue());
            return true;
        }
        if (button == 0 && x >= left + 10 && x < left + 194 && y >= top + 130 && y < top + 130 + ROWS * 46) {
            List<Map.Entry<String, String>> contacts = contacts();
            int first = Math.clamp(scroll, 0, Math.max(0, contacts.size() - ROWS));
            int index = first + (int) ((y - top - 130) / 46);
            if (index < contacts.size() && (y - top - 130) % 46 < 40) {
                call(contacts.get(index).getValue());
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, contacts().size() - ROWS));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY),
                button, PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
