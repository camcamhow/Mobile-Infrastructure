package com.example.mobilecellnetwork.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;

/**
 * Reusable modal that prompts the user for a password.
 * The password field is masked with the standard bullet character (●).
 * An {@link PasswordCallback} receives the entered password when the user
 * presses the OK button.
 */
public class PasswordModal extends Screen {
    /** Callback invoked with the entered password. */
    public interface PasswordCallback {
        /** Called when the user confirms the password.
         * @param password the entered password (may be empty) */
        void onPassword(String password);
    }

    private final Screen parent;
    private final String title;
    private final String subtitle;
    private final PasswordCallback callback;
    private EditBox passwordField;
    private Button okButton;
    private String validationMessage = "";

    private static final int WIDTH = 200;
    private static final int HEIGHT = 120;

    public PasswordModal(Screen parent, String title, String subtitle, PasswordCallback callback) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.subtitle = subtitle;
        this.callback = callback;
    }

    @Override
    protected void init() {
        int cx = (this.width - WIDTH) / 2;
        int cy = (this.height - HEIGHT) / 2;

        // Password input (masked)
        this.passwordField = new EditBox(this.font,
                cx + 20, cy + 35, WIDTH - 40, 12,
                Component.literal("Password"));
        this.passwordField.setCanLoseFocus(true);
        this.passwordField.setMaxLength(128);
        this.passwordField.setFormatter((value, cursorPosition) ->
                FormattedCharSequence.forward("●".repeat(value.length()), Style.EMPTY));
        this.addWidget(this.passwordField);

        // OK button
        this.okButton = Button.builder(Component.literal("OK"), btn -> submit())
                .bounds(cx + 20, cy + 60, 80, 20).build();
        this.addRenderableWidget(this.okButton);
        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> returnToParent())
                .bounds(cx + WIDTH - 100, cy + 60, 80, 20).build());
    }

    private void submit() {
        String password = passwordField.getValue();
        int minimumLength = MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        if (password.length() < minimumLength) {
            validationMessage = "Password must be at least " + minimumLength + " characters.";
            return;
        }
        callback.onPassword(password);
        returnToParent();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Phone‑frame inner panel style
        graphics.fill(this.width / 2 - WIDTH / 2 - 2, this.height / 2 - HEIGHT / 2 - 2,
                this.width / 2 + WIDTH / 2 + 2, this.height / 2 + HEIGHT / 2 + 2, 0xFF0F1620);
        graphics.fill(this.width / 2 - WIDTH / 2, this.height / 2 - HEIGHT / 2,
                this.width / 2 + WIDTH / 2, this.height / 2 + HEIGHT / 2, 0xFF17212B);

        Font font = this.minecraft.font;
        int titleX = this.width / 2 - font.width(this.title) / 2;
        graphics.drawString(font, this.title, titleX, this.height / 2 - HEIGHT / 2 + 8, 0xFFFFFFFF, true);
        if (this.subtitle != null && !this.subtitle.isEmpty()) {
            int subX = this.width / 2 - font.width(this.subtitle) / 2;
            graphics.drawString(font, this.subtitle, subX, this.height / 2 - HEIGHT / 2 + 22, 0xFFCCCCCC, true);
        }
        String validationText = passwordField != null
                && !passwordField.getValue().isEmpty()
                && passwordField.getValue().length() < MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get()
                ? "Password must be at least "
                + MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get() + " characters."
                : validationMessage;
        if (!validationText.isEmpty()) {
            int errorX = this.width / 2 - font.width(validationText) / 2;
            graphics.drawString(font, validationText, errorX,
                    this.height / 2 - HEIGHT / 2 + 92, 0xFFFF5555, true);
        }
        if (okButton != null && passwordField != null) {
            okButton.active = passwordField.getValue().length()
                    >= MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void returnToParent() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        returnToParent();
    }
}
