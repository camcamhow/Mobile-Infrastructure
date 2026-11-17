package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.network.packet.CreateNetworkPacket;
import com.example.mobilecellnetwork.network.packet.UpdateNetworkPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NetworkEditorScreen extends Screen {
    private final Screen parent;
    private final String networkId;
    private final String initialName;
    private boolean roaming;
    private EditBox name;
    private EditBox password;
    private String error = "";
    private boolean awaitingCreation;
    private int x, y, w, h;

    public NetworkEditorScreen(Screen parent, String networkId, String name, boolean roaming) {
        super(Component.literal(networkId.isEmpty() ? "New network" : "Edit network"));
        this.parent = parent;
        this.networkId = networkId;
        this.initialName = name;
        this.roaming = roaming;
    }

    @Override
    protected void init() {
        w = Math.min(300, width - 20);
        h = Math.min(190, height - 20);
        x = (width - w) / 2;
        y = (height - h) / 2;
        name = new EditBox(font, x + 12, y + 49, w - 24, 20, Component.literal("Network name"));
        name.setMaxLength(64);
        name.setValue(initialName);
        addRenderableWidget(name);
        if (networkId.isEmpty()) {
            password = new EditBox(font, x + 12, y + 90, w - 24, 20, Component.literal("Password"));
            password.setMaxLength(128);
            password.setFormatter((value, cursor) -> FormattedCharSequence.forward("*".repeat(value.length()), Style.EMPTY));
            addRenderableWidget(password);
        } else {
            addRenderableWidget(Button.builder(roamingLabel(), button -> {
                roaming = !roaming;
                button.setMessage(roamingLabel());
            }).bounds(x + 12, y + 90, w - 24, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save())
                .bounds(x + w - 116, y + h - 26, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(x + w - 62, y + h - 26, 50, 18).build());
    }

    private Component roamingLabel() {
        return Component.literal("Roaming: " + (roaming ? "On" : "Off"));
    }

    private void save() {
        if (awaitingCreation) return;
        String value = name.getValue().trim();
        if (value.isEmpty() || value.contains("|")) {
            error = "Enter a name without |.";
            return;
        }
        if (networkId.isEmpty()) {
            int minimum = MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
            if (password.getValue().length() < minimum) {
                error = "Password must be at least " + minimum + " characters.";
                return;
            }
            awaitingCreation = true;
            error = "Creating network...";
            PacketDistributor.sendToServer(new CreateNetworkPacket(value, password.getValue()));
        } else {
            PacketDistributor.sendToServer(new UpdateNetworkPacket(networkId, value, roaming));
            onClose();
        }
    }

    public void creationResult(boolean success, String message) {
        if (!awaitingCreation) return;
        awaitingCreation = false;
        if (success) onClose();
        else error = message;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(x, y, x + w, y + h, 0xFF0F1620);
        graphics.renderOutline(x, y, w, h, 0xFF3A4A5A);
        graphics.fill(x, y, x + w, y + 2, PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), w - 24),
                x + 12, y + 12, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "Network name", x + 12, y + 36, PhoneColors.SECONDARY_TEXT, true);
        graphics.drawString(font, networkId.isEmpty() ? "Password" : "Roaming availability",
                x + 12, y + 77, PhoneColors.SECONDARY_TEXT, true);
        if (!error.isEmpty()) {
            graphics.drawString(font, font.plainSubstrByWidth(error, w - 24),
                    x + 12, y + h - 40, PhoneColors.ERROR_TEXT, true);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
