package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
import com.example.mobilecellnetwork.network.PhoneSetupPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhoneSetupScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private int step;
    private String name = "";
    private String pin = "";
    private String appearance = "light";
    private String photo = "";
    private String error = "";
    private EditBox field;

    public PhoneSetupScreen(PhoneMenu menu) {
        super(Component.literal("Set up your phone"));
        this.menu = menu;
        ClientPhoneCache.setAppearance(appearance);
    }

    @Override
    public PhoneMenu phoneMenu() { return menu; }

    @Override
    public void navigateBack() {
        if (step > 0) {
            step--;
            rebuild();
        }
    }

    @Override
    public void navigateHome() { }

    @Override
    protected void init() {
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        field = null;
        if (step == 1 || step == 3) {
            field = PhoneTextField.create(font, x + 17, y + 145, 170, 28,
                    step == 1 ? "Phone name" : "Optional 4-12 digit PIN",
                    step == 1 ? 32 : 12, ClientPhoneCache.phoneStyle());
            if (step == 3) {
                field.setFilter(value -> value.matches("[0-9]*"));
            }
            field.setValue(step == 1 ? name : pin);
            addRenderableWidget(field);
        }
        addRenderableWidget(new PhoneActionButton(x + 24, y + 295, 156, 24, "Continue",
                button -> next()));
        if (step > 0) {
            addRenderableWidget(new PhoneActionButton(x + 24, y + 326, 156, 22, "Back",
                    button -> navigateBack()));
        }
        if (step == 2 || step == 4) {
            addRenderableWidget(new PhoneActionButton(x + 24, y + 185, 156, 24,
                    step == 2 ? "Change appearance" : "Choose photo", button -> {
                        if (step == 2) {
                            appearance = appearance.equals("light") ? "dark" : "light";
                            ClientPhoneCache.setAppearance(appearance);
                        } else {
                            minecraft.setScreen(new PhotoImportScreen(this, menu, selected -> photo = selected.name(), true));
                        }
                    }));
        }
        if (step == 4 && !photo.isEmpty()) {
            addRenderableWidget(new PhoneActionButton(x + 24, y + 216, 156, 22, "Remove photo",
                    button -> {
                        photo = "";
                        rebuild();
                    }));
        }
    }

    private void next() {
        error = "";
        if (step == 1) {
            name = field.getValue().strip();
            if (name.isEmpty() || name.chars().anyMatch(Character::isISOControl)) {
                error = "Enter a phone name.";
                return;
            }
        } else if (step == 3) {
            pin = field.getValue();
            if (!pin.isEmpty() && !pin.matches("[0-9]{4,12}")) {
                error = "PIN must have 4-12 digits.";
                return;
            }
        }
        if (step < 4) {
            step++;
            rebuild();
            return;
        }
        java.util.UUID id = PhoneDeviceClientCache.phoneId();
        if (id == null) {
            error = "Phone unavailable. Reopen it.";
            return;
        }
        PacketDistributor.sendToServer(new PhoneSetupPacket(id, name, appearance, pin, photo));
        error = "Saving setup...";
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    public void completed() {
        if (minecraft.player != null) {
            PhoneDeviceClientCache.setUnlocked(true);
            PhoneScreen.openPhone(menu, minecraft.player.getInventory());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        String heading = switch (step) {
            case 0 -> "Hello";
            case 1 -> "Name your phone";
            case 2 -> "Choose appearance";
            case 3 -> "Secure your phone";
            default -> "Profile photo";
        };
        PhoneFrame.renderHeader(graphics, this, heading, false);
        String description = switch (step) {
            case 0 -> "Welcome to your new phone";
            case 1 -> "Give this phone a name";
            case 2 -> "Current: " + (appearance.equals("light") ? "Light" : "Dark");
            case 3 -> "Leave blank to skip the PIN";
            default -> photo.isEmpty() ? "Optional: select a photo" : "Photo selected";
        };
        graphics.drawCenteredString(font, description, x + PhoneFrame.getScreenW() / 2, y + 112,
                PhoneColors.textPrimary(style));
        if (field != null) {
            PhoneTextField.renderBackground(graphics, style, x + 17, y + 145, 170, 28);
        }
        if (step == 2 || step == 4) {
            PhoneRenderer.drawSecondaryButton(graphics, style, x + 24, y + 185, 156, 24,
                    step == 2 ? "Change appearance" : "Choose photo", font);
        }
        if (step == 4 && !photo.isEmpty()) {
            PhoneRenderer.drawSecondaryButton(graphics, style, x + 24, y + 216, 156, 22, "Remove photo", font);
        }
        PhoneRenderer.drawPrimaryButton(graphics, style, x + 24, y + 295, 156, 24,
                step == 4 ? "Finish setup" : "Continue", font);
        if (step > 0) {
            PhoneRenderer.drawSecondaryButton(graphics, style, x + 24, y + 326, 156, 22, "Back", font);
        }
        if (!error.isBlank()) {
            graphics.drawCenteredString(font, error, x + PhoneFrame.getScreenW() / 2, y + 267,
                    PhoneColors.textSecondary(style));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, x), PhoneFrame.layoutMouseY(this, y), button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, x), PhoneFrame.layoutMouseY(this, y), button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public boolean isPauseScreen() { return false; }
}
